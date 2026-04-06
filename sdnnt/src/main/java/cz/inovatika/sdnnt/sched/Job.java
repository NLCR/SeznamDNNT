/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.sched;

import cz.inovatika.sdnnt.IndexerServlet;
import cz.inovatika.sdnnt.InitServlet;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.AbstractXMLImport;
import cz.inovatika.sdnnt.index.OAIHarvester;
import cz.inovatika.sdnnt.index.XMLImporterDistri;
import cz.inovatika.sdnnt.index.XMLImporterHeureka;
import cz.inovatika.sdnnt.index.XMLImporterKosmas;

import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.services.*;
import cz.inovatika.sdnnt.services.PXKrameriusService.CheckResults;
import cz.inovatika.sdnnt.services.compare.DifferencesResult;
import cz.inovatika.sdnnt.services.compare.RecordFingerprint;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.*;
import cz.inovatika.sdnnt.services.impl.hackcerts.HttpsTrustManager;
import cz.inovatika.sdnnt.services.impl.shib.ShibUsersControllerImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.services.locks.LocksSupport;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.StringUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.UnableToInterruptJobException;

/**
 * @author alberto
 */
public class Job implements InterruptableJob {

    private static final Logger LOGGER = Logger.getLogger(Job.class.getName());


    public Job() {

    }

    public void fire(String jobName) {
        LOGGER.log(Level.INFO, "Job {0} fired", jobName);
        fire(Options.getInstance().getJSONObject("jobs").getJSONObject(jobName));
    }

    public void fire(JSONObject jobData) {
        jobData.put("interrupted", false);
        String action = jobData.getString("type");
        Actions actionToDo = Actions.valueOf(action.toUpperCase());
        actionToDo.doPerform(jobData);
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        //
    }

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        String jobName = "unnamed";
        try {
            JobDataMap data = jec.getMergedJobDataMap();
            jobName = (String) data.get("jobname");
            String jobKey = jec.getJobDetail().getKey().toString();
            String path = InitServlet.CONFIG_DIR + File.separator + "sched.state";
            String state = "";
            File f = new File(path);
            if (f.exists() && f.canRead()) {
                state = FileUtils.readFileToString(f, "UTF-8");
            }
            if ("paused".equals(state) || SchedulerMgr.getInstance().isPaused()) {
                LOGGER.log(Level.INFO, "Scheduler is paused. Nothing to do.", jobKey);
                return;
            }
            int i = 0;
            for (JobExecutionContext j : jec.getScheduler().getCurrentlyExecutingJobs()) {
                if (jobKey.equals(j.getJobDetail().getKey().toString())) {
                    i++;
                }
            }
            if (i > 1) {
                LOGGER.log(Level.INFO, "jobKey {0} is still running. Nothing to do.", jobKey);
                return;
            }

            fire(jobName);
            LOGGER.log(Level.FINE, "job {0} finished", jobKey);
        } catch (SchedulerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            LOGGER.log(Level.SEVERE, "Can't execute job " + jobName, ex);
        }
    }

    public enum Actions {

        
        /** Depreceted - alternativni linky do alephu - jiz se nepouziva */
        ALTERNATIVE_LINKS_UPDATE {
            @Override
            void doPerform(JSONObject jobData) {
                long start = System.currentTimeMillis();
                LOGGER.fine(name()+":configuration is "+jobData);
                UpdateAlternativeAlephLinksImpl impl = new UpdateAlternativeAlephLinksImpl();
                impl.updateLinks();
                QuartzUtils.printDuration(UpdateAlternativeAlephLinksImpl.LOGGER, start);
            }
        },

        /** SKC4  update followers */
        SKC_4 {
            
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    LOGGER.fine(name()+":configuration is "+jobData);
                    long start = System.currentTimeMillis();
                    JSONObject results = jobData.optJSONObject("results");
                    String loggerPostfix = jobData.optString("logger");
                    SKCJoinServiceImpl service = new SKCJoinServiceImpl(loggerPostfix, results);
                    try {
                        service.updateFollowers();
                    } catch (IOException  e) {
                        service.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    }finally {
                        QuartzUtils.printDuration(service.getLogger(), start);
                    }

                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },
        

        /** Update digital libraries */
        DL_UPDATE {
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    long start = System.currentTimeMillis();
                    LOGGER.fine(name()+":configuration is "+jobData);
                    String loggerName = jobData.optString("logger");
                    UpdateDigitalLibrariesImpl digitalLibraries = new UpdateDigitalLibrariesImpl(loggerName);
                    digitalLibraries.updateDL();
                    QuartzUtils.printDuration(UpdateAlternativeAlephLinksImpl.LOGGER, start);
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        /** Update dates libraries */
        DATE_UPDATE {
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    long start = System.currentTimeMillis();
                    LOGGER.fine(name()+":configuration is "+jobData);
                    String loggerName = jobData.optString("logger");
                    UpdateDatesImpl digitalLibraries = new UpdateDatesImpl(loggerName);
                    digitalLibraries.updateDates();
                    QuartzUtils.printDuration(UpdateAlternativeAlephLinksImpl.LOGGER, start);
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        /** SKC Update */
        SKC_UPDATE {
            @Override
            void doPerform(JSONObject jobData) {
                try {

                    LocksSupport.SERVICES_LOCK.lock();
                    JSONObject json = new JSONObject();
                    JSONObject results = jobData.optJSONObject("results");

                    boolean debug = jobData.optBoolean("debug", false);
                    OAIHarvester oai = new OAIHarvester(jobData.optString("from"), results);
                    oai.setDebug(debug);

                    String set = "SKC";
                    String core = "catalog";
                    boolean merge = true;
                    json.put("indexed", oai.update(set, core,
                            merge,
                            true,
                            false));
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }

            }
        },
        
        
        /** Type check */
        SKC_TYPE_CHECK {
            
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    LOGGER.fine(name()+":configuration is "+jobData);
                    long start = System.currentTimeMillis();
                    JSONObject results = jobData.optJSONObject("results");
                    String loggerPostfix = jobData.optString("logger");
                    AbstractCheckDeleteService service = new SKCTypeServiceImpl(loggerPostfix, results);
                    try {
                        service.update();
                    } catch (IOException | SolrServerException e) {
                        service.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    }finally {
                        QuartzUtils.printDuration(service.getLogger(), start);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }

            }
        },
        
        /** Parovani SCK */
        DNT_SKC_PAIR {

            @Override
            void doPerform(JSONObject jobData) {
                LOGGER.fine(name()+":configuration is "+jobData);
                long start = System.currentTimeMillis();
                JSONObject results = jobData.optJSONObject("results");
                String loggerPostfix = jobData.optString("logger");
                AbstractCheckDeleteService service = new DNTSKCPairServiceImpl(loggerPostfix, results);
                try {
                    service.update();
                } catch (IOException | SolrServerException e) {
                    service.getLogger().log(Level.SEVERE, e.getMessage(), e);
                }finally {
                    QuartzUtils.printDuration(service.getLogger(), start);
                }
            }
        },
        
        /**
         * Kontrola oproti datumu vydani
         */
        DATE_PX_CHECK {
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    LOGGER.fine(name()+":configuration is "+jobData);
                    long start = System.currentTimeMillis();
                    JSONObject iteration = jobData.optJSONObject("iteration");
                    JSONObject results = jobData.optJSONObject("results");
    
                    JSONArray jsonArrayOfStates = jobData.optJSONArray("states");
                    List<String> states = new ArrayList<>();
                    if (jsonArrayOfStates != null) {
                        jsonArrayOfStates.forEach(it -> {
                            states.add(it.toString());
                        });
                    }
    
                    String loggerPostfix = jobData.optString("logger");
    
                    PXYearService service = new PXYearServiceImpl(loggerPostfix, iteration, results);
                    try {
                        List<String> check = service.check();
                        
                        service.getLogger().info("Number of found candidates "+check.size());
                        if (!check.isEmpty()) {
                            int maximum = 100;
                            if (results != null && results.has("request") && results.getJSONObject("request").has("items")) {
                                maximum = results.getJSONObject("request").getInt("items");
                            }
    
                            int numberOfBatch = check.size() / maximum;
                            if (check.size() % maximum > 0) {
                                numberOfBatch = numberOfBatch + 1;
                            }
                            for (int i = 0; i < numberOfBatch; i++) {
    
                                int startIndex = i * maximum;
                                int endIndex = Math.min((i + 1) * maximum, check.size());
                                List<String> subList = check.subList(startIndex, endIndex);
                                // posle zadost
                                if (results.has("request")) {
                                    service.getLogger().info("Creating request for sublist "+subList);
                                    service.request(subList);
                                }
                                // provede pouze update
                                if (results.has("state")) {
                                    service.update(subList);
                                }
                            }
                        }
                    } catch (ConflictException | AccountException | IOException | SolrServerException e) {
                        service.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    }finally {
                        QuartzUtils.printDuration(service.getLogger(), start);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        /**
         * Kontrola oproti kramerium
         */
        KRAMERIUS_PX_CHECK {
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    LOGGER.fine(name()+":configuration is "+jobData);
                    long start = System.currentTimeMillis();
                    String loggerPostfix = jobData.optString("logger");
                    JSONObject iteration = jobData.optJSONObject("iteration");
                    JSONObject results = jobData.optJSONObject("results");
    
                    JSONArray jsonArrayOfStates = jobData.optJSONArray("states");
                    List<String> states = new ArrayList<>();
                    if (jsonArrayOfStates != null) {
                        jsonArrayOfStates.forEach(it -> {
                            states.add(it.toString());
                        });
                    }
    
                    HttpsTrustManager.allowAllSSL();
                    
                    PXKrameriusService service = new PXKrameriusServiceImpl(loggerPostfix,iteration, results);
                    try {
    
                        // check true, check private 
                        Map<CheckResults,Set<String>> map = service.check();
                        Set<String> check = map.get(CheckResults.public_dl_results);
                        service.getLogger().info("Number of found candidates "+check.size());
                        if (!check.isEmpty()) {
    
                            int maximum = 100;
                            if (results != null && results.has("request") && results.getJSONObject("request").has("items")) {
                                maximum = results.getJSONObject("request").getInt("items");
                            }
    
                            int numberOfBatch = check.size() / maximum;
                            if (check.size() % maximum > 0) {
                                numberOfBatch = numberOfBatch + 1;
                            }
                            for (int i = 0; i < numberOfBatch; i++) {
    
                                int startIndex = i * maximum;
                                int endIndex = Math.min((i + 1) * maximum, check.size());
                                //List<String> subList = check.subList(startIndex, endIndex);
                                List<String> subList = check.stream()
                                        .skip(startIndex)
                                        .limit(endIndex - startIndex)
                                        .collect(Collectors.toList());
                                
                                // posle zadost
                                if (results.has("request")) {
                                    service.getLogger().info("Creating request for sublist "+subList);
                                    service.request(subList);
                                }

                                // provede pouze update
                                if (results.has("state") || results.has("ctx")) {
                                    service.getLogger().info("Updating sublist "+subList);
                                    service.update(subList);
                                }
                                
                                
                            }
                        }
                        
                        Set<String> disableCtx = map.get(CheckResults.disable_ctx_results);
                        if (!disableCtx.isEmpty()) {
                            
                            int maximum = 100;
                            if (results != null && results.has("request") && results.getJSONObject("request").has("items")) {
                                maximum = results.getJSONObject("request").getInt("items");
                            }
    
                            int numberOfBatch = disableCtx.size() / maximum;
                            if (disableCtx.size() % maximum > 0) {
                                numberOfBatch = numberOfBatch + 1;
                            }
                            for (int i = 0; i < numberOfBatch; i++) {
                                int startIndex = i * maximum;
                                int endIndex = Math.min((i + 1) * maximum, disableCtx.size());
                                //List<String> subList = check.subList(startIndex, endIndex);
                                List<String> subList = disableCtx.stream()
                                        .skip(startIndex)
                                        .limit(endIndex - startIndex)
                                        .collect(Collectors.toList());
                                // provede pouze update
                                if (results.has("ctx")) {
                                    service.getLogger().info("Updating sublist "+subList);
                                    service.disableContext(subList);
                                }
                            }
                        }
                        
                    } catch (ConflictException | AccountException | IOException | SolrServerException e) {
                        service.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(service.getLogger(), start);
    
                    }
                } finally {
                     LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        /** Currator action */
        CURATOR_ACTION {

            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();
                    long start = System.currentTimeMillis();
                    
                    JSONObject iteration = jobData.optJSONObject("iteration");
                    JSONObject results = jobData.optJSONObject("results");
                    String logger = jobData.optString("logger");

                    CuratorActionsSetImpl currActionsSet = new CuratorActionsSetImpl(logger, iteration, results);
                    try {
                        List<String> checkList = currActionsSet.check();
                        currActionsSet.getLogger().info("Found candidates "+checkList.size());
                        currActionsSet.update(checkList);

                    } catch (IOException e) {
                        currActionsSet.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } catch (ConflictException e) {
                        currActionsSet.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } catch (SolrServerException e) {
                        currActionsSet.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(currActionsSet.getLogger(), start);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE,ex.getMessage(), ex);
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
            
        },
        
        
        SETSTATE_CURATOR_ACTION {

            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();
                    long start = System.currentTimeMillis();
                    
                    JSONObject iteration = jobData.optJSONObject("iteration");
                    JSONObject results = jobData.optJSONObject("results");
                    String logger = jobData.optString("logger");

                    ChangeStateFromCurratorActionImpl currActionsSet = new ChangeStateFromCurratorActionImpl(logger, iteration, results);
                    try {
                        List<String> checkList = currActionsSet.check();
                        currActionsSet.update(checkList);
                    } catch (IOException e) {
                        currActionsSet.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } catch (SolrServerException e) {
                        currActionsSet.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(currActionsSet.getLogger(), start);
                    }
                    
                    
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE,ex.getMessage(), ex);
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
                
            }
        },
        
        
        /** Refresh and set granularity */
        GRANULARITY {
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    long start = System.currentTimeMillis();
                    String logger = jobData.optString("logger");

                    GranularityServiceImpl gservice = new GranularityServiceImpl(logger);
                    GranularitySetStateServiceImpl sservice = new GranularitySetStateServiceImpl(logger);

                    try {
                        gservice.initialize();
                        gservice.refershGranularity();
                        sservice.setStates(new ArrayList<>());
                    } catch (IOException e) {
                        gservice.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(gservice.getLogger(), start);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },
        
        /** Refresh granularit */
        REFRESH_GRANULARITIES {
            
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    long start = System.currentTimeMillis();
                    String logger = jobData.optString("logger");
                    GranularityServiceImpl service = new GranularityServiceImpl(logger);
                    try {
                        service.initialize();
                        service.refershGranularity();
                    } catch (IOException e) {
                        service.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(service.getLogger(), start);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        /** Nastaveni stavu u granularit */
        SETSTATES_GRANULARITIES {
            
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    long start = System.currentTimeMillis();
                    String logger = jobData.optString("logger");
                    GranularitySetStateServiceImpl service = new GranularitySetStateServiceImpl(logger);
                    try {
                        service.setStates(new ArrayList<>());
                    } catch (IOException e) {
                        service.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(service.getLogger(), start);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },
        

        
        


        EUIPO_IOCP {

            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    LOGGER.fine(name()+":configuration is "+jobData);
                    long start = System.currentTimeMillis();
                    String logger = jobData.optString("logger");
                    JSONObject iteration = jobData.optJSONObject("iteration");
                    JSONObject results = jobData.optJSONObject("results");
    
                    JSONArray jsonArrayOfStates = jobData.optJSONArray("states");
                    List<String> states = new ArrayList<>();
                    if (jsonArrayOfStates != null) {
                        jsonArrayOfStates.forEach(it -> {
                            states.add(it.toString());
                        });
                    }
                    EUIPOImportService impl = new EUIPOImportServiceImpl(logger, iteration, results);
                    try {
                        
                        
                        List<String> checkBK = impl.check("BK");
                        List<Integer> updated = new ArrayList<>();
                        int updatedBKCount = impl.update("BK", checkBK);
    
                        List<String> checkSE = impl.check("SE");
                        int updatedSECount = impl.update("SE", checkSE);

                    } catch (Exception e) {
                        impl.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(impl.getLogger(), start);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        EUIPO_UOCP {

            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    
                    LOGGER.fine(name()+":configuration is "+jobData);
                    long start = System.currentTimeMillis();
                    String logger = jobData.optString("logger");
                    JSONObject iteration = jobData.optJSONObject("iteration");
                    JSONObject results = jobData.optJSONObject("results");
    
                    JSONArray jsonArrayOfStates = jobData.optJSONArray("states");
                    List<String> states = new ArrayList<>();
                    if (jsonArrayOfStates != null) {
                        jsonArrayOfStates.forEach(it -> {
                            states.add(it.toString());
                        });
                    }
                    EUIPOCancelService impl = new EUIPOCancelServiceImpl(logger, iteration, results);
                    try {
                        List<String> checkBK = impl.check();
                        int updatedBKCount = impl.update(checkBK);

                    } catch (Exception e) {
                        impl.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        QuartzUtils.printDuration(impl.getLogger(), start);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
            
        },

        
        
        /** import from distri */
        IMPORT {

            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();
                    long start = System.currentTimeMillis();

                    long groupId = System.currentTimeMillis();
                    String hexGroupId = Long.toHexString(groupId);
                    
                    LinkedHashSet<String> processingSet = new LinkedHashSet<>();
                    
                    JSONObject checkPn = jobData.optJSONObject("checkPN");
                    int value = checkPn != null ? checkPn.optInt("value",6) : 6;
                    String unit = checkPn != null ? checkPn.optString("unit","month") : "month";
                    
                    JSONObject distriConf = jobData.optJSONObject("districz");
                    String distriUrl = distriConf != null ? distriConf.optString("url") : null;
                    JSONObject kosmasConf = jobData.optJSONObject("kosmas");
                    String kosmasUrl = kosmasConf != null ? kosmasConf.optString("url") : null;
                    JSONObject heurekaConf = jobData.optJSONObject("heureka");
                    String heurekaUrl = heurekaConf != null ? heurekaConf.optString("url"):null;

                    float match1 = jobData != null ? jobData.optFloat("match_1",1.0f) : 1.0f;
                    float match21 = jobData != null ? jobData.optFloat("match_21",1.0f) : 1.0f;
                    float match22 = jobData != null ? jobData.optFloat("match_22",0.5f) : 0.5f;

                    String logger = jobData.optString("logger");
                    AbstractXMLImport lastImport = null;
                    try {
                        if (StringUtils.isAnyString(kosmasUrl)) {
                            AbstractXMLImport kosmas = new XMLImporterKosmas(logger, hexGroupId, kosmasUrl, value, unit, match1, match21, match22);
                            processingSet =  kosmas.doImport(null, false, processingSet);
                            lastImport = kosmas;
                        }
                        
                        if (StringUtils.isAnyString(distriUrl)) {
                            AbstractXMLImport distri = new XMLImporterDistri(logger, hexGroupId, distriUrl, value, unit, match1, match21, match22);
                            processingSet = distri.doImport(null, false, processingSet);
                            lastImport = distri;
                        }

                        if (StringUtils.isAnyString(heurekaUrl)) { 
                            XMLImporterHeureka heureka = new XMLImporterHeureka(logger, hexGroupId, heurekaUrl, value, unit, match1, match21, match22);
                            processingSet = heureka.doImport(null, false,processingSet );
                            lastImport = heureka;
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        if (lastImport != null) {
                            QuartzUtils.printDuration(lastImport.getLogger(), start);
                        }
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        UPDATE_IMPORT_STATES {
            @Override
            void doPerform(JSONObject jobData) {
                LocksSupport.SERVICES_LOCK.lock();
                try {
                    String logger = jobData.optString("logger");
                    UpdateStatesForImports updateStatesForImports = new UpdateStatesForImportsImpl(logger);
                    updateStatesForImports.updateImports();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },


        DIFF {
            @Override
            void doPerform(JSONObject jobData) {
                LocksSupport.SERVICES_LOCK.lock();
                try {
                    String logger = jobData.optString("logger");
                    String comparingSolr = jobData.optString("comparingSolr");

                    JSONArray ignoreGranularityFields =  jobData.optJSONArray("ignoreGranularityFields", new JSONArray(Set.of("datestamp", "indextime","fetched", "link", "baseUrl","details", "rocnik")));
                    JSONArray ignoreMasterLinksFields =  jobData.optJSONArray("ignoreMaterLinksFields", new JSONArray(Set.of("fetched","link","baseUrl")));
                    CompareServiceImpl service = new CompareServiceImpl(logger, comparingSolr,ignoreGranularityFields,ignoreMasterLinksFields);
                    DifferencesResult check = service.check();
                    Map<String, Pair<RecordFingerprint, RecordFingerprint>> diff1 = check.getDifferences();
                    System.out.println(check.getDifferences());
                    diff1.keySet().forEach(key -> {
                        Pair<RecordFingerprint, RecordFingerprint> pair = diff1.get(key);
                        String diffReport = pair.getLeft().getDiffReport(pair.getRight());
                        service.getLogger().info("Difference report for " + key + ": " + diffReport+"\n");
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        DIFFHTML {
            @Override
            void doPerform(JSONObject jobData) {
                LocksSupport.SERVICES_LOCK.lock();
                try {
                    String logger = jobData.optString("logger");
                    String comparingSolr = jobData.optString("comparingSolr");
                    JSONArray ignoreGranularityFields =  jobData.optJSONArray("ignoreGranularityFields", new JSONArray(Set.of("datestamp", "indextime","fetched", "link", "baseUrl","details", "rocnik")));
                    JSONArray ignoreMasterLinksFields =  jobData.optJSONArray("ignoreMaterLinksFields", new JSONArray(Set.of("fetched","link","baseUrl")));
                    CompareServiceImpl service = new CompareServiceImpl(logger, comparingSolr,ignoreGranularityFields,ignoreMasterLinksFields);
                    DifferencesResult check = service.check();
                    Map<String, Pair<RecordFingerprint, RecordFingerprint>> diffs = check.getDifferences();

                    StringBuilder html = new StringBuilder();
                    html.append("<html><head><meta charset='UTF-8'><style>")
                            .append(".diff-table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }")
                            .append(".diff-table td, .diff-table th { border: 1px solid #ddd; padding: 8px; }")
                            .append(".old-val { background-color: #ffdce0; } .new-val { background-color: #cdffd8; }")
                            .append("h2 { font-family: sans-serif; background: #eee; padding: 5px; }")
                            .append("</style></head><body>");

                    html.append("<h1>Report rozdílů ze dne: ").append(new Date()).append("</h1>");

                    diffs.forEach((key, pair) -> {
                        String  sdnntHost = Options.getInstance().getString("sdnnt.host");
                        String baseUrl = sdnntHost != null ? sdnntHost.trim() : "";
                        if (!baseUrl.isEmpty() && !baseUrl.endsWith("/")) {
                            baseUrl += "/";
                        }
                        if (StringUtils.isAnyString(sdnntHost)) {
                            String fullUrl = baseUrl + "search?q=" + key + "&page=0";
                            html.append("<h2>ID Záznamu: <a href='").append(fullUrl)
                                    .append("' target='_blank' style='text-decoration: none; color: #0366d6;'>")
                                    .append(key).append(" 🔗</a></h2>");
                        } else {
                            html.append("<h2>ID Záznamu: ").append(key).append("</h2>");
                        }
                        html.append(pair.getLeft().getDiffReportHtml(pair.getRight()));
                    });

                    html.append("</body></html>");

                    String DEFAULT_OUTPUT_FOLDER = System.getProperty("user.home") + File.separator + ".sdnnt/diff";
                    File dir = new File(DEFAULT_OUTPUT_FOLDER);
                    if (!dir.exists()) dir.mkdirs();

                    String filename = "diff_" + System.currentTimeMillis() + ".html";
                    Files.writeString(Path.of(dir.getAbsolutePath(), filename), html.toString());

                    service.getLogger().info("HTML report uložen jako: " + filename);

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        PNREQ {

            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();
                    long start = System.currentTimeMillis();


                    List<Pair<String,String>> processors = new ArrayList<>();
                    
                    JSONObject checkPn = jobData.optJSONObject("checkPN");
                    int value = checkPn != null ? checkPn.optInt("value",6) : 6;
                    String unit = checkPn != null ? checkPn.optString("unit","month") : "month";
                    
                    
                    JSONObject distriConf = jobData.optJSONObject("districz");
                    String distriUrl = distriConf != null ? distriConf.optString("url") : null;
                    if (distriUrl != null) {
                        processors.add(Pair.of("districz", distriUrl));
                    }
                    
                    JSONObject kosmasConf = jobData.optJSONObject("kosmas");
                    String kosmasUrl = kosmasConf != null ? kosmasConf.optString("url") : null;
                    if (kosmasUrl != null) {
                        processors.add(Pair.of("kosmas", kosmasUrl));
                    }

                    JSONObject heurekaConf = jobData.optJSONObject("heureka");
                    String heurekaUrl = heurekaConf != null ? heurekaConf.optString("url"):null;
                    if (heurekaUrl != null) {
                        processors.add(Pair.of("heureka", heurekaUrl));
                    }

                    
                    String logger = jobData.optString("logger");
                    PNCheckStatesService pnCheckStateService = null;
                    try {
                        
                        pnCheckStateService = new PNCheckStatesServiceImpl(logger, processors, value, unit);
                        List<Pair<String, String>> found = pnCheckStateService.check();
                        
                        
                        int maximum = 100;
                        int numberOfBatch = found.size() / maximum;
                        if (found.size() % maximum > 0) {
                            numberOfBatch = numberOfBatch + 1;
                        }
                        for (int i = 0; i < numberOfBatch; i++) {
                            int startIndex = i * maximum;
                            int endIndex = Math.min((i + 1) * maximum, found.size());
                            
                            List<Pair<String, String>> sublist = found.subList(startIndex, endIndex);
                            List<String> sublistIdentifiers = sublist.stream().map(Pair::getKey).collect(Collectors.toList());
                            
                            
                            pnCheckStateService.getLogger().info("Creating request for sublist "+sublistIdentifiers);
                            pnCheckStateService.request(sublistIdentifiers);

                        }
                        
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    } finally {
                        if (pnCheckStateService != null) {
                            QuartzUtils.printDuration(pnCheckStateService.getLogger(), start);
                        }
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
                
            }
            
        },
        
        
        
        WORKFLOW {
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LocksSupport.SERVICES_LOCK.lock();

                    try {
                        LOGGER.fine(name()+":configuration is "+jobData);
                        AccountServiceImpl accountService = new AccountServiceImpl(null, null);
                        accountService.schedulerSwitchStates();
                    } catch (ConflictException | AccountException | IOException | SolrServerException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                } finally {
                    LocksSupport.SERVICES_LOCK.unlock();
                }
            }
        },

        
        // Posila email o zmenu stavu zaznamu podle nastaveni uzivatelu
        NOTIFICATIONS {
            @Override
            void doPerform(JSONObject jobData) {
                try {
                    LOGGER.fine(name()+":configuration is "+jobData);
                    MailService mailService = new MailServiceImpl();
                    UserController controler = new UserControlerImpl(/* no reqest */ null);
                    ShibUsersControllerImpl shibUserController = new ShibUsersControllerImpl();
                    NotificationsService notificationsService = new NotificationServiceImpl(controler,shibUserController, mailService);
                    notificationsService.processNotifications(NotificationInterval.valueOf(jobData.getString("interval")));
                } catch (UserControlerException | NotificationsException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        };

        abstract void doPerform(JSONObject jobData);
    }

}
