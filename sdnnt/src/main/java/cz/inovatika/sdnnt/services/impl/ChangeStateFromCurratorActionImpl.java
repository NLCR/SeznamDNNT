package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.ChangeStateFromCurratorAction;
import cz.inovatika.sdnnt.services.impl.CuratorActionsSetImpl.Action;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class ChangeStateFromCurratorActionImpl implements ChangeStateFromCurratorAction{

    private Logger logger = Logger.getLogger(CuratorActionsSetImpl.class.getName());

    protected int updateBatchLimit = AbstractEUIPOService.UPDATE_BATCH_LIMIT;

    public static final String SELECTION_NAME_KEY = "name";

    private String action;
    private String curState;
    private String license;
    
    public ChangeStateFromCurratorActionImpl(String logger, JSONObject iteration, JSONObject results) {
        if (iteration != null) {
            iterationConfig(iteration);
        }
        if (results != null) {
            iterationResults(results);
        }

        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }
    }

    public void iterationResults(JSONObject results) {
        if (results != null) {
            if (results.has("license")) {
                this.license = results.optString("license");
            }
            if (!results.has("curatorstate")) {
                throw configurationPropertyIsMissing("curatorstate", "results");
            }
            this.curState = results.optString("curatorstate");
        } else {
            throw configurationPropertyIsMissing("results", null);
        }
    }

    private IllegalStateException configurationPropertyIsMissing(String field, String structName) {
        if (structName != null) {
            return new IllegalStateException(String.format(" Configuration key '%s' is missing in the '%s' section", field, structName));
        } else {
            return new IllegalStateException(String.format(" Configuration key '%s' is missing", field));
        }
    }

    /**
     * Configuration method for 
     * @param iteration
     */
    public void iterationConfig(JSONObject iteration) {
        if (iteration != null) {
            if (iteration.has(SELECTION_NAME_KEY)) {
                this.action = iteration.optString(SELECTION_NAME_KEY);
            } else {
                throw configurationPropertyIsMissing(SELECTION_NAME_KEY,"iteration");
            }
        }
    }

    public Logger getLogger() {
        return this.logger;
    }
    
    @Override
    public List<String> check() {
        getLogger().info(String.format(" Config for iteration ->  action %s",  this.action));

        List<String> foundCandidates = new ArrayList<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + AbstractEUIPOService.FETCH_LIMIT);

        List<String> plusFilter = new ArrayList<>();
        plusFilter.add(String.format("c_actions:%s", this.action));
       
        try (SolrClient solrClient = buildClient()) {
            support.iterate(
                    solrClient, reqMap, null, plusFilter, new ArrayList<>(), Arrays.asList(IDENTIFIER_FIELD), (rsp) -> {
                        Object identifier = rsp.getFieldValue("identifier");
                        foundCandidates.add(identifier.toString());

                    }, IDENTIFIER_FIELD);
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return foundCandidates;
        
    }

    @Override
    public void update(List<String> identifiers) throws IOException, SolrServerException {
        AtomicInteger retVal = new AtomicInteger();
        if (!identifiers.isEmpty()) {
            try (SolrClient solrClient = buildClient()) {
                int numberOfUpdateBatches = identifiers.size() / updateBatchLimit;
                if (identifiers.size() % updateBatchLimit > 0) {
                    numberOfUpdateBatches += 1;
                }
                for (int i = 0; i < numberOfUpdateBatches; i++) {
                    int startIndex = i * updateBatchLimit;
                    int endIndex = (i + 1) * updateBatchLimit;
                    List<String> subList = identifiers.subList(startIndex, Math.min(endIndex, identifiers.size()));
                    long fetchStart = System.currentTimeMillis();
                    Pair<List<MarcRecord>,List<String>> mrFound = MarcRecord.fromIndex(solrClient, subList);
                    if (!mrFound.getRight().isEmpty()) {
                        // error 
                    }
                    long fetchStop = System.currentTimeMillis();
                    
                    long reqStart = System.currentTimeMillis();
                    UpdateRequest stateReq = new UpdateRequest();
                    mrFound.getLeft().forEach(mr-> {
                        SolrInputDocument uDoc = ChangeProcessStatesUtility.changeProcessState(this.curState, this.license, mr,"scheduler", this.action);
                        
                        // change.. remove action
                        List fieldValues = (List) uDoc.getFieldValues(MarcRecordFields.CURATOR_ACTIONS);
                        if (fieldValues.contains(this.action)) {
                            fieldValues.remove(this.action);
                        }
                        if (fieldValues.isEmpty()) {
                            uDoc.removeField(MarcRecordFields.CURATOR_ACTIONS);
                        } else {
                            uDoc.setField(MarcRecordFields.CURATOR_ACTIONS, fieldValues);
                        }
                        
                        
                        stateReq.add(uDoc);
                    });

                    long reqStop = System.currentTimeMillis();

                    long reqProcessStart = System.currentTimeMillis();
                    if (!stateReq.getDocuments().isEmpty()) {
                        UpdateResponse response = stateReq.process(solrClient, DataCollections.catalog.name());
                        retVal.addAndGet(subList.size());
                    }
                    long reqProcessStop = System.currentTimeMillis();

                    long commitStart = System.currentTimeMillis();
                    SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
                    long commitStop = System.currentTimeMillis();
                    
                    logger.info("Updating identifiers size:"+subList.size());
                    long whole = commitStop - fetchStart;
                    
                    logger.info(String.format("Whole update %d  (fetch %d, prepare req %d, process req %d, commit %d)", whole, (fetchStop - fetchStart), (reqStop - reqStart), (reqProcessStop - reqProcessStart), (commitStop - commitStart)));
                    

                    
                }
                logger.info("Commiting ");
                SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
            }
        }
    }
    
    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    public static void main(String[] args) throws IOException, SolrServerException {
        JSONObject iteration = new JSONObject();
        iteration.put("action", "PresunStavu2023");

        //flist.put("-kuratorstav:DX");
        
        
        JSONObject results =new JSONObject();
        results.put("curatorState", "A");
        results.put("license", "dnntt");
        
        
        
        ChangeStateFromCurratorActionImpl set = new ChangeStateFromCurratorActionImpl("", iteration, results);
        List<String> check = set.check();
        System.out.println(check);
        set.update(check);
        
    }
}
