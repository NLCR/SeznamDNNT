package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.*;
import cz.inovatika.sdnnt.model.workflow.ZadostTyp;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.PXKrameriusService.CheckResults;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.hackcerts.HttpsTrustManager;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration.KramVersion;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.KrameriusFields;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SimpleGET;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

import static cz.inovatika.sdnnt.utils.PIDUtils.*;

/**
 * Sluzba kontroluje zda je v krameriovi dilo volne ci nikoliv 
 * Vysledkem je kontextova informace a zadost PXN
 * @author happy
 *
 */
public class PXKrameriusServiceImpl extends AbstractPXService implements PXKrameriusService {

    boolean contextInformation = false;

    private AtomicInteger fetchedInfoCounter = new AtomicInteger();
    private AtomicInteger requestedInfoCounter = new AtomicInteger();

    private Logger logger = Logger.getLogger(PXKrameriusService.class.getName());
    private CheckKrameriusConfiguration checkConf;
    
    private PXKrameriusService.DataTypeCheck dataType = PXKrameriusService.DataTypeCheck.live;
    
    public PXKrameriusServiceImpl(String logger, JSONObject iteration, JSONObject results) {
        if (iteration != null) {
            super.iterationConfig(iteration);
            if (iteration.has("data"))  {
                this.dataType = PXKrameriusService.DataTypeCheck.valueOf(iteration.getString("data"));
            }
        }
        if (results != null) {
            contextInformation = results.optBoolean("ctx");
            super.requestsConfig(results);
        }
        
        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }
    }

    public  Set<String> usedInRequest(SolrClient solr, List<String> identifiers, String typeOfRequest) {
        try {
            Set<String> retval = new HashSet<>();
            SolrQuery query = new SolrQuery("*")
                    .setFields("id", "identifiers")
                    .addFilterQuery("navrh:" + typeOfRequest)
                    .setRows(3000);

            String collected = identifiers.stream().map(id -> '"' + id + '"').collect(Collectors.joining(" OR "));
            query.addFilterQuery("identifiers:(" + collected + ")");

            SolrDocumentList zadost = solr.query("zadost", query).getResults();
            zadost.stream().forEach(solrDoc -> {
                Collection<Object> identsFromZadost = solrDoc.getFieldValues("identifiers");
                identsFromZadost.stream().map(Object::toString).forEach(foundInZadost -> {
                    if (identifiers.contains(foundInZadost)) {
                        retval.add(foundInZadost);
                    }
                });
            });
            return retval;
        } catch (SolrServerException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return new HashSet<>();
    }

    

    
    public void initialize() {
        JSONObject checkKamerius = getOptions().getJSONObject("check_kramerius");
        this.checkConf = CheckKrameriusConfiguration.initConfiguration(checkKamerius);
    }


    @Override
    public Map<CheckResults,Set<String>> check() {

        logger.info("Initializing px process");

        this.initialize();
        // true candidates
        LinkedHashSet<String> foundTrueCandidates = new LinkedHashSet<>();
        LinkedHashSet<String> foundFalseCandidates = new LinkedHashSet<>();
        
        Map<String, List<String>> identifiers2Links = new HashMap<>();
        // preprocessed links
        Map<String, List<String>> identifiers2MasterLinks = new HashMap<>();
        
        Map<String, Boolean> identifiers2FlagInDl = new HashMap<>();
        
        
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + LIMIT);

        List<String> plusFilter = new ArrayList<>(Arrays.asList(
                "id_pid:uuid", 
                FMT_FIELD + ":BK"
                ));
        
        
        if (yearConfiguration != null && !this.yearConfiguration.trim().equals("")) {
            plusFilter.add(yearFilter());
        }

        if (!this.states.isEmpty()) {
            String collected = states.stream().collect(Collectors.joining(" OR "));
            plusFilter.add(DNTSTAV_FIELD + ":(" + collected + ")");
        }
        
        List<String> minusFilter = new ArrayList<>(Arrays.asList(KURATORSTAV_FIELD + ":X", 
                KURATORSTAV_FIELD + ":PX", // pak resit px stavy a kontextove informace 
                KURATORSTAV_FIELD + ":DX", 
                DNTSTAV_FIELD+":D")
        );
        
        if (!this.states.isEmpty()) {
            if (states.contains("PX")) {
                minusFilter.remove(KURATORSTAV_FIELD + ":PX");
            }
            if (states.contains("DX")) {
                minusFilter.remove(KURATORSTAV_FIELD + ":DX");
            }
        }
        
        
        logger.info("Current iteration filter " + plusFilter);
        try (final SolrClient solrClient = buildClient()) {
            support.iterate(solrClient, reqMap, null, plusFilter, minusFilter, Arrays.asList(
                    IDENTIFIER_FIELD,
                    SIGLA_FIELD,
                    MARC_911_U,
                    MARC_956_U,
                    MARC_856_U,
                    GRANULARITY_FIELD,
                    MASTERLINKS_FIELD,
                    MASTERLINKS_DISABLED_FIELD,
                    FLAG_PUBLIC_IN_DL
            ), (rsp) -> {
                
                Object identifier = rsp.getFieldValue("identifier");
                
                
                Collection<Object> masterLinks = rsp.getFieldValues(MASTERLINKS_FIELD);
                
                Object flag = rsp.getFieldValue(FLAG_PUBLIC_IN_DL);
                if (flag != null) {
                    identifiers2FlagInDl.put(identifier.toString(), Boolean.valueOf(flag.toString()));
                }
                
                
                Collection<Object> links1 = rsp.getFieldValues(MARC_911_U);
                Collection<Object> links2 = rsp.getFieldValues(MARC_856_U);
                Collection<Object> links3 = rsp.getFieldValues(MARC_956_U);
                
                
                // find 
                if (masterLinks != null) {
                    List<String> ll = masterLinks.stream().map(Object::toString).collect(Collectors.toList());
                    identifiers2MasterLinks.put(identifier.toString(), ll);
                }
                
                if (links1 != null && !links1.isEmpty()) {
                    List<String> ll = links1.stream().map(Object::toString).collect(Collectors.toList());
                    identifiers2Links.put(identifier.toString(), ll);
                } else if (links2 != null && !links2.isEmpty()) {
                    List<String> ll = links2.stream().map(Object::toString).collect(Collectors.toList());
                    identifiers2Links.put(identifier.toString(), ll);
                } else if (links3 != null && !links3.isEmpty()) {
                    List<String> ll = links3.stream().map(Object::toString).collect(Collectors.toList());
                    identifiers2Links.put(identifier.toString(), ll);
                }
            }, IDENTIFIER_FIELD);
        } catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        
        logger.info("Found candidates: "+identifiers2Links.size());
        
        try (final SolrClient solr = buildClient()) {

            List<String> keys = new ArrayList<>(identifiers2Links.keySet());
            int batchSize = 40;
            int numberOfBatches = keys.size() / batchSize;
            if (keys.size() % batchSize > 0) {
                numberOfBatches += 1;
            }
            for (int i = 0; i < numberOfBatches; i++) {
                int startIndex = i * batchSize;
                int endIndex = (i + 1) * batchSize;
                List<String> batch = keys.subList(startIndex, Math.min(endIndex, keys.size()));
                if (this.typeOfRequest != null) {
                    // used in reuqest; must not be in case of context information
                    Set<String> used = usedInRequest(solr, batch, this.typeOfRequest);
                    used.stream().forEach(identifiers2Links::remove);
                    used.stream().forEach(identifiers2MasterLinks::remove);
                    used.stream().forEach(identifiers2FlagInDl::remove);
                    
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        logger.info("Found candidates after used check: "+identifiers2Links.keySet().size());
        
        if (this.dataType.equals(PXKrameriusService.DataTypeCheck.live)) {
            Map<String, List<Pair<String, String>>> buffer = new HashMap<>();
            for (String key : identifiers2Links.keySet()) {
                List<String> links = identifiers2Links.get(key);
                // key je klic, pokud mame masterlinks, muzeme 
                for (String link : links) {
                    String pid = pid(link);
                    
                    if (pid != null) {
                        //String baseUrl = baseUrl(link);
                        String baseUrl = checkConf.baseUrl(link);
                        InstanceConfiguration configuration = checkConf.match(baseUrl);
                        
                        if (configuration !=  null && !configuration.isShouldSkip()) {
                            if (!buffer.containsKey(baseUrl)) {
                                buffer.put(baseUrl, new ArrayList<>());
                            }
                            
                            getLogger().fine("baseurl " + baseUrl + " pair " + Pair.of(key, pid));
                            buffer.get(baseUrl).add(Pair.of(key, pid));

                            checkBuffer(buffer, foundTrueCandidates, foundFalseCandidates);
                        } else {
                            getLogger().fine("Skipping instance '"+configuration+"'");
                        }
                    }
                    
                }
            }

            if (!buffer.isEmpty()) {
                clearBuffer(buffer, foundTrueCandidates, foundFalseCandidates);
            }
            logger.info(String.format("Requested info count %d, Fetched info count %d", this.requestedInfoCounter.get(), this.fetchedInfoCounter.get()));

        } else {
            for (String key : identifiers2MasterLinks.keySet()) {
                
                List<String> masterlinks = identifiers2MasterLinks.get(key);
                List<Boolean> collected = masterlinks.stream().map(str-> {
                    JSONObject obj = new JSONObject(str);
                    if (obj.has("dostupnost")) {
                        return obj.getString("dostupnost").equals("public") ? Boolean.TRUE : Boolean.FALSE;
                    } else return Boolean.FALSE;
                }).collect(Collectors.toList());
                
                if (collected.contains(Boolean.TRUE)) {
                    foundTrueCandidates.add(key);
                } else {
                    foundFalseCandidates.add(key);
                }
            }
        }
        
        Map<CheckResults, Set<String>> retval = new HashMap<>();
        retval.put(CheckResults.public_dl_results, foundTrueCandidates);
        
        Set<String> filtered = foundFalseCandidates.stream().filter(id -> identifiers2FlagInDl.containsKey(id) && identifiers2FlagInDl.get(id)).collect(Collectors.toSet());
        retval.put(CheckResults.disable_ctx_results, filtered);
        return retval;
    }

    
    protected void checkBuffer(Map<String, List<Pair<String, String>>> buffer, LinkedHashSet<String> foundTrueCandidates,LinkedHashSet<String> foundFalseCandidates) {
        Integer sum = buffer.values().stream().map(List::size).reduce(0, Integer::sum);
        if (sum > bufferSize()) {
            clearBuffer(buffer, foundTrueCandidates, foundFalseCandidates);
        }
    }
    
    protected int bufferSize() {
        JSONObject jsonObject = getOptions().getJSONObject("check_kramerius");
        if (jsonObject != null) {
            return jsonObject.optInt("buffersize", CHECK_SIZE);
        } else return CHECK_SIZE;
    }

    protected void clearBuffer(Map<String, List<Pair<String, String>>> buffer, LinkedHashSet<String> foundTrueCadidates,LinkedHashSet<String> foundFalseCadidates) {
        try {
            for (String baseUrl : buffer.keySet()) {
                if (baseUrl == null) continue;

                InstanceConfiguration configuration = this.checkConf.match(baseUrl);
                // TODO:Disable 
                if (configuration == null || configuration.isShouldSkip()) {
                    getLogger().warning("Skipping url "+baseUrl+"'");
                    continue;
                } 

                List<Pair<String, String>> pairs = buffer.get(baseUrl);
                String condition = pairs.stream().map(Pair::getRight).filter(Objects::nonNull).map(p -> {
                    return p.replace(":", "\\:");
                }).collect(Collectors.joining(" OR "));
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }

                this.requestedInfoCounter.addAndGet(pairs.size());
                
                if (configuration.getVersion().equals(KramVersion.V5)) {
                    //TODO: Support kramerius 7 - licenses 
                    String encodedCondition = URLEncoder.encode("PID:(" + condition + ")", "UTF-8");
                    String encodedFieldList = URLEncoder.encode("PID dostupnost","UTF-8");
                    String url = baseUrl + "api/v5.0/search?q=" +  encodedCondition + "&wt=json&rows="+pairs.size()+"&fl="+encodedFieldList;
                    
                    logger.fine(String.format("Kramerius url is %s and list of identifiers are %s", url, pairs.stream().map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));

                    try {
                        String result = simpleGET(url);
                        JSONObject object = new JSONObject(result);
                        JSONObject response = object.getJSONObject("response");
                        JSONArray jsonArray = response.getJSONArray("docs");
                        for (int i = 0, ll = jsonArray.length(); i < ll; i++) {
                            JSONObject oneDoc = jsonArray.getJSONObject(i);
                            this.fetchedInfoCounter.addAndGet(1);
                            
                            if (oneDoc.has(KrameriusFields.DOSTUPNOST_V5)) {
                                String pid = oneDoc.getString(KrameriusFields.PID_V5);
                                String dostupnost = oneDoc.getString( /*"dostupnost"*/ KrameriusFields.DOSTUPNOST_V5);
                                if (dostupnost != null && dostupnost.equals("public")) {

                                    Optional<Pair<String, String>> any = pairs.stream().filter(p -> {
                                        return p.getRight().endsWith(pid);
                                    }).findAny();

                                    if (any.isPresent()) {
                                        Pair<String, String> pair = any.get();
                                        logger.info(String.format("Found public document. Identifier %s and pid %s", pair.getLeft(), pair.getRight()));
                                        if (!foundTrueCadidates.contains(pair.getLeft())) {
                                            foundTrueCadidates.add(pair.getLeft());
                                        }
                                    }
                                } else {
                                    Optional<Pair<String, String>> any = pairs.stream().filter(p -> {
                                        return p.getRight().endsWith(pid);
                                    }).findAny();

                                    if (any.isPresent()) {
                                        Pair<String, String> pair = any.get();
                                        if (!foundFalseCadidates.contains(pair.getLeft())) {
                                            foundFalseCadidates.add(pair.getLeft());
                                        }
                                    }
                                    
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, String.format("Error while accessing %s, error: %s ", url, e.getMessage()) , e);
                    }
                } else {
                    //TODO: Support kramerius 7 - licenses 
                    String encodedCondition = URLEncoder.encode("pid:(" + condition + ")", "UTF-8");
                    String encodedFieldList = URLEncoder.encode(String.format("%s %s %s", KrameriusFields.PID_V7, KrameriusFields.LICENSES_V7, KrameriusFields.LICENSES_OF_ANCESTORS_V7),"UTF-8");
                    String url = baseUrl + "api/client/v7.0/search?q=" +  encodedCondition + "&wt=json&rows="+pairs.size()+"&fl="+encodedFieldList;


                    
                    logger.fine(String.format("Kramerius url is %s and list of identifiers are %s", url, pairs.stream().map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));

                    try {
                        String result = simpleGET(url);
                        JSONObject object = new JSONObject(result);
                        JSONObject response = object.getJSONObject("response");
                        JSONArray jsonArray = response.getJSONArray("docs");
                        for (int i = 0, ll = jsonArray.length(); i < ll; i++) {
                            JSONObject oneDoc = jsonArray.getJSONObject(i);
                            this.fetchedInfoCounter.addAndGet(1);
                            
                            List<String> v7licenses = v7licenses(oneDoc);
                            String pid = oneDoc.getString(KrameriusFields.PID_V7);

                            String dostupnost = "private";
                            if (v7licenses.contains("public")) { dostupnost = "public"; }
                            if (dostupnost != null && dostupnost.equals("public")) {

                                Optional<Pair<String, String>> any = pairs.stream().filter(p -> {
                                    return p.getRight().endsWith(pid);
                                }).findAny();

                                if (any.isPresent()) {
                                    Pair<String, String> pair = any.get();
                                    logger.info(String.format("Found public document. Identifier %s and pid %s", pair.getLeft(), pair.getRight()));
                                    if (!foundTrueCadidates.contains(pair.getLeft())) {
                                        foundTrueCadidates.add(pair.getLeft());
                                    }
                                }
                            } else {
                                
                                Optional<Pair<String, String>> any = pairs.stream().filter(p -> {
                                    return p.getRight().endsWith(pid);
                                }).findAny();

                                if (any.isPresent()) {
                                    Pair<String, String> pair = any.get();
                                    if (!foundFalseCadidates.contains(pair.getLeft())) {
                                        foundFalseCadidates.add(pair.getLeft());
                                    }
                                }

                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, String.format("Error while accessing %s, error: %s ", url, e.getMessage()) , e);
                    }

                }
                

            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        buffer.clear();
    }

    protected String simpleGET(String url) throws IOException {
        return SimpleGET.get(url);
    }

    private List<String> v7licenses(JSONObject doc) {
        JSONArray licensesJSONArray = new JSONArray();
        if (doc.has(KrameriusFields.LICENSES_V7)) {
            doc.getJSONArray(KrameriusFields.LICENSES_V7).forEach(licensesJSONArray::put);
        } 
        if (doc.has(KrameriusFields.LICENSES_OF_ANCESTORS_V7)) {
            doc.getJSONArray(KrameriusFields.LICENSES_OF_ANCESTORS_V7).forEach(licensesJSONArray::put);
        }
        List<String> licensesList = new ArrayList<>();
        for (Object licObj : licensesJSONArray) {
            licensesList.add(licObj.toString());
        }
        return licensesList;
    }

    
    @Override
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException {
        if (!identifiers.isEmpty()) {

            CuratorItemState cState = null;
            PublicItemState pState = null;
            if (this.destinationState != null && StringUtils.isAnyString(this.destinationState)) {
                cState = CuratorItemState.valueOf(this.destinationState);
                pState = cState.getPublicItemState(null);
            }
            try (final SolrClient solr = buildClient()) {
                // diff documents - use to have field
                for (String identifier : identifiers) {
                    SolrInputDocument idoc = null;
                    logger.fine(String.format("Updating identifier %s", identifier));
                    if (cState != null) {
                        idoc = ChangeProcessStatesUtility.changeProcessState(solr, identifier, cState.name(), "scheduler/kramerius check");
                    }
                    // history information
                    if (contextInformation) {
                        logger.fine("Setting context information ");
                        if (idoc != null) {
                            super.enahanceContextInformation(idoc, true);
                        } else {
                            idoc = super.changeContextInformation(solr, identifier, true);
                        }
                    }
                    if (idoc != null) {
                        solr.add(DataCollections.catalog.name(), idoc);
                    }
                }
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
            }
        }
    }

    
    
    @Override
    public void disableContext(List<String> identifiers)
            throws AccountException, IOException, ConflictException, SolrServerException {
        if (!identifiers.isEmpty()) {
            try (final SolrClient solr = buildClient()) {
                UpdateRequest uReq = new UpdateRequest();
                for (String identifier : identifiers) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identifier);
                    SolrJUtilities.atomicSetNull(idoc, MarcRecordFields.FLAG_PUBLIC_IN_DL);
                    uReq.add(idoc);
                }
                if (!uReq.getDocuments().isEmpty()) {
                    UpdateResponse response = uReq.process(solr, DataCollections.catalog.name());
                }
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
            }
        }
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }
    
        

    
}
