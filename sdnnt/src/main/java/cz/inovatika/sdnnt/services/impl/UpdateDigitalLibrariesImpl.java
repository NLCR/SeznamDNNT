package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.ALTERNATIVE_ALEPH_LINK;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_CCNB_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SET_SPEC_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.UpdateDigitalLibraries;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.utils.LinksUtilities;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class UpdateDigitalLibrariesImpl implements UpdateDigitalLibraries {

    public static final Logger LOGGER = Logger.getLogger(UpdateAlternativeAlephLinksImpl.class.getName());
    public static final int LIMIT = 30000;
    public static final int BATCH_SIZE = 1000;
    //public static final int CHECK_SIZE = 50;

    public Logger logger;
    
    public UpdateDigitalLibrariesImpl(String loggerName) {
        if (logger != null) {
            this.logger = Logger.getLogger(loggerName);
        } else {
            this.logger = LOGGER;
        }
    }
    
    
    @Override
    public void updateDL() {
        long start = System.currentTimeMillis();
        Options options = getOptions();
        CheckKrameriusConfiguration checkKram = CheckKrameriusConfiguration.initConfiguration(options.getJSONObject("check_kramerius"));


        final Map<String, List<String>> idLibs = new HashMap<>();
        final Map<String, List<String>> granPids = new HashMap<>();
        
        try(SolrClient client = buildClient()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "" + LIMIT);
            CatalogIterationSupport support = new CatalogIterationSupport();
            
            List<String> plusFilter = new ArrayList<>(Arrays.asList(
                    "id_pid:uuid",
                    "setSpec:SKC"));
            
            List<String> minusFilter = Arrays.asList("dntstav:D");
            
            logger.info("Current iteration filter " + plusFilter+ " and negative filter "+minusFilter);
            support.iterate(client, reqMap, null, plusFilter, minusFilter, Arrays.asList(
                    IDENTIFIER_FIELD,
                    ID_CCNB_FIELD,
                    MarcRecordFields.GRANULARITY_FIELD,
                    "marc_911u","marc_856u", "marc_956u"
            ), (rsp) -> {

                Object identifier = rsp.getFieldValue(IDENTIFIER_FIELD);
                List<String> links = LinksUtilities.krameriusMergedLinksFromDocument(rsp);
                List<String> digitalizedKeys = LinksUtilities.digitalizedKeys(checkKram, links);
                //List<String> granularityPids = new ArrayList<>();
                if (!digitalizedKeys.isEmpty()) {
                    idLibs.put(identifier.toString(), digitalizedKeys);
                }
                
                List<String> granularities =  (List<String>) rsp.getFieldValue(MarcRecordFields.GRANULARITY_FIELD);
                if (granularities != null) {
                    
                    List<String> granularityPids =  granularities.stream().map(JSONObject::new).map(it-> {
                        String pid = it.optString("pid");
                        return pid;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                    
                    granPids.put(identifier.toString(), granularityPids);
                }
                
                
            }, IDENTIFIER_FIELD);

            List<String> idList = new ArrayList<>(idLibs.keySet());
            logger.info("Number of records:"+idLibs.size());
            int numberOfBatches = idLibs.keySet().size() / BATCH_SIZE + (idLibs.size() % BATCH_SIZE == 0 ? 0 :1);
            for (int i = 0; i < numberOfBatches; i++) {
                int startList = i*BATCH_SIZE;
                int endList = Math.min(startList + BATCH_SIZE,idLibs.keySet().size());
                List<String> subList = idList.subList(startList, endList);
                this.logger.info("Updating batch: "+(i*BATCH_SIZE + subList.size()));
                this.update(client, subList, idLibs, granPids);
                if (i % 25 == 0) {
                    this.logger.info("Commiting: "+(i*BATCH_SIZE + subList.size()));
                    SolrJUtilities.quietCommit(client, DataCollections.catalog.name());
                }
            }
            SolrJUtilities.quietCommit(client, DataCollections.catalog.name());

        } catch (IOException | SolrServerException e) {
            logger.log(Level.SEVERE, e.getMessage(),e);
        } finally {
            QuartzUtils.printDuration(LOGGER, start);
        }
    }
    
    public void update(SolrClient solr, List<String> identifiers, Map<String,List<String>> dkMapping,  Map<String,List<String>> granPids) throws  IOException,  SolrServerException {
        if (!identifiers.isEmpty()) {
            UpdateRequest uReq = new UpdateRequest();
            for (String identifier : identifiers) {
                if (dkMapping.containsKey(identifier)) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identifier);
                    atomicUpdate(idoc, dkMapping.get(identifier), MarcRecordFields.DIGITAL_LIBRARIES);
                    if (granPids.containsKey(identifier)) {
                        List<String> pids = granPids.get(identifier);
                        pids.forEach(pid-> {
                            SolrJUtilities.atomicAddDistinct(idoc, pid, MarcRecordFields.ID_PID);
                        });
                    }
                    
                    uReq.add(idoc);
                }
            }
            if (!uReq.getDocuments().isEmpty()) {
                uReq.process(solr, DataCollections.catalog.name());
            }
            logger.info("Updating identifier "+identifiers);
        }
    }

    protected void atomicUpdate(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("set", fValue);
        idoc.addField(fName, modifier);
    }


    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    public static void main(String[] args) {
        UpdateDigitalLibrariesImpl digitalLibraries = new UpdateDigitalLibrariesImpl("test");
        digitalLibraries.updateDL();

    }
    
    
}
