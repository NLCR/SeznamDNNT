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
import cz.inovatika.sdnnt.services.CurratorActionsSet;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class CurratorActionsSetImpl implements CurratorActionsSet {
    
    private Logger logger = Logger.getLogger(CurratorActionsSetImpl.class.getName());
    
    public static final String DEFAULT_ACTION_NAME="selected";
    public static final String ACTION_KEY = "action";
    
    public static final String ACTION_NAME_KEY="actionName";
    
    public static enum Action {
        set, unset;
    }
    
    
    protected List<Pattern> compiledPatterns = AbstractEUIPOService.DEFAULT_REGULAR_EXPRESSIONS_NONPARSABLE_DATES;
    private List<String> filters = new ArrayList<>();
    private Action action = Action.set;
    
    protected int updateBatchLimit = AbstractEUIPOService.UPDATE_BATCH_LIMIT;

    private String actionName;

    
    
    public CurratorActionsSetImpl(String logger, JSONObject iteration, JSONObject results) {
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
            String actKey = results.optString(ACTION_KEY);
            this.action = Action.valueOf(actKey);
            this.actionName = results.optString(ACTION_NAME_KEY) == null ? DEFAULT_ACTION_NAME : results.optString(ACTION_NAME_KEY);
            if (!isValidAsciiWithoutWhitespace(this.actionName)) {
                throw new IllegalStateException(String.format("Invalid action name %s", this.actionName));
            }
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
    public List<String> check() {
        getLogger().info(String.format(" Config for iteration ->  filters %s; nonparsable dates %s ",  this.filters, this.compiledPatterns));

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
                    solrClient, reqMap, null, plusFilter, Arrays.asList(KURATORSTAV_FIELD + ":X",
                            KURATORSTAV_FIELD + ":D", MarcRecordFields.ID_EUIPO + ":*"),
                    Arrays.asList(IDENTIFIER_FIELD), (rsp) -> {
                        Object identifier = rsp.getFieldValue("identifier");
                        foundCandidates.add(identifier.toString());
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
            //List<String> toRemove = new ArrayList<>();
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
                        SolrJUtilities.atomicAddDistinct(idoc, this.actionName, MarcRecordFields.CURRATOR_ACTIONS);
                    }
                    if (!uReq.getDocuments().isEmpty()) {
                        uReq.process(solrClient, DataCollections.catalog.name());
                    }
                    logger.info("Updating identifier "+identifiers);
                }
            }
        }
        return 0;
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    public static void main(String[] args) {
        
    }
}
