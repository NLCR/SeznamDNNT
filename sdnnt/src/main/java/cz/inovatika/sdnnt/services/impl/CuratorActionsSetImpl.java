package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.services.CuratorActionsSet;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class CuratorActionsSetImpl implements CuratorActionsSet {
    
    private Logger logger = Logger.getLogger(CuratorActionsSetImpl.class.getName());
    
    public static final String DEFAULT_ACTION_NAME="selected";
    public static final String ACTION_KEY = "action";
    
    public static final String SELECTION_NAME_KEY="name";
    
    public static enum Action {
        set, unset;
    }
    
    
    protected List<Pattern> compiledPatterns = new ArrayList<>();
    private List<String> filters = new ArrayList<>();
    private Action action = Action.set;
    
    protected int updateBatchLimit = AbstractEUIPOService.UPDATE_BATCH_LIMIT;
    private String actionName;

    
    
    public CuratorActionsSetImpl(String logger, JSONObject iteration, JSONObject results) {
        if (iteration != null) {
            iterationConfig(iteration);
        } else {
            throw configurationPropertyIsMissing("iteration", null);
        }
        if (results != null) {
            iterationResults(results);
        } else {
            throw configurationPropertyIsMissing("results", null);
        }

        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }
    }

    
    public void iterationResults(JSONObject results) {
        if (results != null) {
            String actKey = results.optString(ACTION_KEY, Action.set.name());
            
            this.action = Action.valueOf(actKey);
            this.actionName = StringUtils.isAnyString(results.optString(SELECTION_NAME_KEY))  ? results.optString(SELECTION_NAME_KEY): DEFAULT_ACTION_NAME ;
            if (!results.has(SELECTION_NAME_KEY)) {
                throw configurationPropertyIsMissing(SELECTION_NAME_KEY, "iteration");
            }
            if (!isValidAsciiWithoutWhitespace(this.actionName)) {
                throw new IllegalStateException(String.format("Invalid action name %s", this.actionName));
            }
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
            if (iteration.has(AbstractEUIPOService.FILTERS_KEY)) {
                JSONArray filters = iteration.optJSONArray(AbstractEUIPOService.FILTERS_KEY);
                List<String> listFilters = new ArrayList<>();
                if (filters != null) {
                    filters.forEach(f -> {
                        listFilters.add(f.toString());
                    });
                }
                if (listFilters != null) {
                    this.filters = listFilters;
                }
            }
            if (iteration.has(AbstractEUIPOService.NONPARSABLE_DATES_KEY)) {
                JSONArray nonParsble = iteration.optJSONArray(AbstractEUIPOService.NONPARSABLE_DATES_KEY);
                List<String> listExpressions = new ArrayList<>();
                if (listExpressions != null) {
                    nonParsble.forEach(f -> {
                        listExpressions.add(f.toString());
                    });
                }
                if (listExpressions != null) {
                    this.compiledPatterns = listExpressions.stream().map(Pattern::compile).collect(Collectors.toList());
                }
            }
        }
    }

    
    public Logger getLogger() {
        return logger;
    }
    
    private static boolean isValidAsciiWithoutWhitespace(String input) {
        for (char c : input.toCharArray()) {
            if (c > 127 || Character.isWhitespace(c)) {
                return false; 
            }
        }
        return true; 
    }
    
    @Override
    public List<String> check() throws AccountException, IOException, SolrServerException {
        getLogger().info(String.format(" Config for iteration ->  filters %s; nonparsable dates %s ",  this.filters, this.compiledPatterns));
        
        AccountServiceImpl acService = new AccountServiceImpl();
        
        //TODO: To configuration
        List<JSONObject> vnzAndVnl = acService.findAllRequests(null, Arrays.asList("VNL","VNZ"), null);
        List<String> vnzAndVnlIds = vnzAndVnl.stream().map(j-> {
            return j.optString("id");
        }).collect(Collectors.toList());
        
        
        List<String> foundCandidates = new ArrayList<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + AbstractEUIPOService.FETCH_LIMIT);

        List<String> plusFilter = new ArrayList<>();
        if (this.filters != null && !this.filters.isEmpty()) {
            plusFilter.addAll(this.filters);
        }
        logger.info("Current iteration filter " + plusFilter);
       
        try (SolrClient solrClient = buildClient()) {
            support.iterate(
                    solrClient, reqMap, null, plusFilter, new ArrayList<>(),

                    Arrays.asList(IDENTIFIER_FIELD, "date1","date1_int","date2","date2_int", "historie_kurator_stavu"), (rsp) -> {
                        Object identifier = rsp.getFieldValue("identifier");
                        
                        String zadostId = null;
                        
                        if (rsp.containsKey(MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD)) {
                            
                            Object historie = rsp.getFieldValue(MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD);
                            JSONArray historieJSONArr = new JSONArray(historie.toString());
                            if (historieJSONArr.length() > 0) {
                                JSONObject jsonObject = historieJSONArr.getJSONObject(historieJSONArr.length() -1);
                                if(jsonObject.has("zadost")) {
                                    zadostId =  jsonObject.getString("zadost");
                                }
                            }
                        }
                        
                        if (zadostId != null && vnzAndVnlIds.contains(zadostId)) {
                            getLogger().info(String.format("Skipping %s", identifier));
                        } else {
                            
                            Object date1Int = rsp.getFieldValue("date1_int");
                            if (date1Int == null && this.compiledPatterns.size() > 0) {
                                
                                if (matchNonparsableDate(date1Int, date1Int)) {
                                    foundCandidates.add(identifier.toString());
                                }
                            } else {
                                // parsovatelne datum
                                foundCandidates.add(identifier.toString());
                            }
                        }
                    }, IDENTIFIER_FIELD);
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return foundCandidates;
    }

    @Override
    public int update(List<String> identifiers) throws IOException, ConflictException, SolrServerException {
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

                    UpdateRequest uReq = new UpdateRequest();
                    for (String identifier : subList) {
                        SolrInputDocument idoc = new SolrInputDocument();
                        idoc.setField(IDENTIFIER_FIELD, identifier);
                        switch(this.action) {
                            case  set: 
                                SolrJUtilities.atomicAddDistinct(idoc, this.actionName, MarcRecordFields.CURATOR_ACTIONS);
                                break;
                            case unset:
                                SolrJUtilities.atomicRemove(idoc, this.actionName, MarcRecordFields.CURATOR_ACTIONS);
                                break;
                        }
                        uReq.add(idoc);
                    }
                    if (!uReq.getDocuments().isEmpty()) {
                        UpdateResponse response = uReq.process(solrClient, DataCollections.catalog.name());
                        retVal.addAndGet(subList.size());
                    }
                    logger.info("Updating identifier "+identifiers);
                }
                logger.info("Commiting ");
                SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
            }
        }
        return retVal.get();
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }
    
    private boolean matchNonparsableDate(Object date1int, Object date1) {
        // nonparsable date
        if (date1int == null) {
            String date1str = date1.toString();
            for (Pattern pattern : this.compiledPatterns) {
                if (pattern.matcher(date1str).matches()) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }


    public static void main(String[] args) throws ConflictException, IOException, SolrServerException, AccountException {
        JSONObject iteration = new JSONObject();

        JSONArray flist = new JSONArray();
        flist.put("fmt:BK");
        flist.put("dntstav:A");
        //flist.put("kuratorstav:DX");

        //flist.put("-kuratorstav:DX");
        
        iteration.put("filters", flist);
        
        JSONObject results =new JSONObject();
        results.put("name", "PresunStavu2023");
        results.put("action", "set");
        
        
        CuratorActionsSetImpl set = new CuratorActionsSetImpl("", iteration, results);
        List<String> check = set.check();
        //int update = set.update(check);

        System.out.println("CHECKED "+check.size());
        //System.out.println("UPDATED "+update);
        
    }
}
