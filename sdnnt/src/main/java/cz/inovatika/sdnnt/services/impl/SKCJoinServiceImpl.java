package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.ALTERNATIVE_ALEPH_LINK;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.FOLLOWERS;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.HISTORIE_STAVU_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.DatabaseConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateSKCUtils;
import cz.inovatika.sdnnt.services.SKCDeleteService;
import cz.inovatika.sdnnt.services.impl.AbstractCheckDeleteService.Process;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class SKCJoinServiceImpl extends AbstractCheckDeleteService implements SKCDeleteService {

    protected Logger logger = Logger.getLogger(SKCTypeServiceImpl.class.getName());

    public SKCJoinServiceImpl(String loggerName, JSONObject results) {
        super(loggerName, results);
        if (loggerName != null) {
            this.logger = Logger.getLogger(loggerName);
        }
    }

    @Override
    public void updateDeleteInfo(List<String> deleteInfo) {
        // TODO Auto-generated method stub
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }
    
    protected Options getOptions() {
        return Options.getInstance();
    }


    @Override
    protected Map<Case, List<Pair<String, List<String>>>> checkUpdate() throws IOException, SolrServerException {
        Map<Case, List<Pair<String, List<String>>>> retvals = new HashMap<>();
        try {
            CatalogIterationSupport support = new CatalogIterationSupport();
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "10000");
            try (final SolrClient solrClient = buildClient()) {
                noFollowers(retvals, support, reqMap, solrClient);
            } catch(IOException e) {
                getLogger().log(Level.SEVERE,e.getMessage(),e);
            }
        } catch (Exception  e) {
            getLogger().log(Level.SEVERE,e.getMessage(),e);
        }
        return retvals;
    }

    private void noFollowers(Map<Case, List<Pair<String, List<String>>>> retvals, CatalogIterationSupport support,
        Map<String, String> reqMap, final SolrClient solrClient) {
        List<String> plusFilter = Arrays.asList(
                KURATORSTAV_FIELD + ":DX"
        );
        
        List<String> minusFilter = Arrays.asList(
                FOLLOWERS + ":*"
        );
        
        support.iterate(solrClient, reqMap, null, plusFilter, minusFilter, 
                Arrays.asList(
                        MarcRecordFields.IDENTIFIER_FIELD,
                        MarcRecordFields.FMT_FIELD, 
                        "place_of_pub",
                        KURATORSTAV_FIELD,
                        HISTORIE_STAVU_FIELD,
                        HISTORIE_KURATORSTAVU_FIELD
                        ), (rsp) -> {

            String fieldValue = (String) rsp.getFirstValue(HISTORIE_KURATORSTAVU_FIELD);
            JSONArray jsonArray = new JSONArray(fieldValue);
            List<String> comments = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jObject = jsonArray.getJSONObject(i);
                String state = jObject.optString("stav");
                String comment = jObject.optString("comment");
                if (state.equals("DX")) {
                    if (comment != null) {
                        if (comment.startsWith("scheduler/")) {
                            comments.add(comment.substring("scheduler/".length()));
                        }
                    }
                }
            }
            // last is SKC_4b
            if (comments.size() >= 1 && (lastComment(comments).equals("SKC_4b") || lastComment(comments).equals("SKC_4"))) {
                try {
                    String identifier = (String) rsp.getFieldValue(MarcRecordFields.IDENTIFIER_FIELD);
                    SolrDocument document = solrClient.getById(DataCollections.catalog.name(),identifier);
                    MarcRecord fromIndex = MarcRecord.fromSolrDoc(document);
                    Pair<Case,List<String>> followers = DuplicateSKCUtils.findSKCFollowers(solrClient, fromIndex);
                    if (!followers.getKey().equals(Case.SKC_4a) && !followers.getKey().equals(Case.SKC_4b)) {
                        if (!retvals.containsKey(followers.getKey())) {
                            retvals.put(followers.getKey(),new ArrayList<>());
                        }
                        retvals.get(followers.getKey()).add(Pair.of(fromIndex.identifier, followers.getRight()));
                    } else {
                        getLogger().log(Level.INFO, String.format(" SKC_4 again %s", identifier));
                    }
                } catch (SolrException | SolrServerException | IOException e) {
                    getLogger().log(Level.SEVERE, e.getMessage(),e);
                }
//            } else {
//                getLogger().log(Level.INFO, String.format(" TEST "));
            }
        }, IDENTIFIER_FIELD);
        
    }

    private String lastComment(List<String> comments) {
        return comments.get(comments.size() -1);
    }

    @Override
    protected List<String> checkDelete() throws IOException, SolrServerException {
        // TODO Auto-generated method stub
        return new ArrayList<>();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }
    
    
    public void updateFollowers() throws IOException {
        try {

            Map<Case,List<Pair<String,List<String>>>> cases = checkUpdate();
            getLogger().log(Level.INFO, String.format("Cases %s", cases.keySet().toString()));
            for (Case cs : cases.keySet()) {
                //String confProc = getProcess(cs);
                List<Pair<String, List<String>>> updates = cases.get(cs);
                getLogger().log(Level.INFO, String.format("Updating records, Case %s and number of records %d", cs.name(), updates.size()));
                switch (cs) {
                    case SKC_1:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());

                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                    case SKC_2a:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());
                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                    case SKC_2b:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());
                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                    case SKC_3:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());
                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                        
                    default:
                        break;
                }
            }
            //updateRecords(checkUpdate());
            //deleteRecords(checkDelete());
        } catch(Exception e) {
            getLogger().log(Level.SEVERE,e.getMessage(),e);
        } finally {
            try (SolrClient solrClient = buildClient()) {
                SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
                SolrJUtilities.quietCommit(solrClient, DataCollections.zadost.name());
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        SKCJoinServiceImpl joinService = new SKCJoinServiceImpl("tt", null);
        joinService.updateFollowers();
    }
    
    
}
