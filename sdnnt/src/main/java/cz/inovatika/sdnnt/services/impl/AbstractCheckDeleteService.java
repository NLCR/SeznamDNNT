package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.MarcRecordDependencyStore;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateUtils;
import cz.inovatika.sdnnt.services.LoggerAware;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public abstract class AbstractCheckDeleteService extends AbstractRequestService implements LoggerAware {

    public static final String TYPE_OF_REQ = "DXN";
    protected JSONObject results;
    
    public AbstractCheckDeleteService(String logger,JSONObject results) {
        this.typeOfRequest = TYPE_OF_REQ;
        this.results = results;
        //if (results != null) {
            
            /*
            if (results.has("state")) {
                process = Process.updateState;
            } else  if (results.has("request")) {
                process = Process.createRequest;
                JSONObject request = results.getJSONObject("request");
                if (request.has("items")) {
                    this.numberOfItems = request.optInt("items");
                }
            }
            */
        //}
    }

    
    //protected abstract 
    protected abstract SolrClient buildClient();

    protected static enum Process {
        state {
            @Override
            protected void update(SolrClient solrClient, AbstractCheckDeleteService reqService,
                    List<Pair<String, List<String>>> batch, Case cs) throws SolrServerException, IOException {
                    MarcRecordDependencyStore depStore = new MarcRecordDependencyStore();
                    try {
                        for (Pair<String, List<String>> pair : batch) {
                            
                            
                            // pokud 
                            MarcRecord origin = MarcRecord.fromIndex(solrClient, pair.getKey());
                            List<MarcRecord> followers = pair.getRight().stream().map(id -> {
                                try {
                                    return MarcRecord.fromIndex(solrClient, id);
                                } catch (IOException | SolrServerException  e) {
                                    reqService.getLogger().log(Level.SEVERE,e.getMessage(),e);
                                    return null;
                                }
                            }).filter(mr-> Objects.nonNull(mr)).collect(Collectors.toList());

                            if (depStore != null) {
                                depStore.addMarcRecord(origin);
                                followers.stream().forEach(depStore::addMarcRecord);
                            }
                            
                            
                            // pri zmene stavu se taky musi menit v zadostech
                            AccountServiceImpl accountService = reqService.buildAccountService();
                            //DuplicateUtils.changeRequests(origin, followers, null);

                            List<String> navrhy = Arrays.asList("NZN","VN","VNL","VNZ","PXN");
                            List<JSONObject> foundRequests = accountService.findAllRequestForGivenIds(null, navrhy, null,  Arrays.asList(origin.identifier));
                            List<Zadost> allRequests = foundRequests.stream().map(Object::toString).map(Zadost::fromJSON).collect(Collectors.toList());
                            
                            DuplicateUtils.changeRequests(origin, followers, allRequests);
                            DuplicateUtils.moveProperties(depStore, origin, followers, null);
                            
                            origin.followers = pair.getRight();
                            SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.D.name(), origin, "scheduler/"+cs);
                            solrClient.add(DataCollections.catalog.name(), document);
                            for (MarcRecord smr : followers) {
                                solrClient.add(DataCollections.catalog.name(), smr.toSolrDoc());
                            }
                        }
                    } catch (AccountException e) {
                        throw new IOException(e);
                    }
            }   
        },
        request{

            protected void update(SolrClient solrClient, AbstractCheckDeleteService reqService,
                    List<Pair<String, List<String>>> batch, Case cs) throws SolrServerException, IOException {
                try {
    
                    for (Pair<String, List<String>> pair : batch) {
                        MarcRecord origin = MarcRecord.fromIndex(solrClient, pair.getKey());
                        origin.followers = pair.getRight();
                        // zmena stavu - nutno 
                        SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin, "scheduler/"+cs);
                        solrClient.add(DataCollections.catalog.name(), document);
                    }
                    List<String> identifiers = batch.stream().map(Pair::getKey).collect(Collectors.toList());
                    reqService.request(identifiers);
                } catch (AccountException e) {
                    reqService.getLogger().log(Level.SEVERE,e.getMessage());
                } catch (ConflictException e) {
                    reqService.getLogger().log(Level.SEVERE,e.getMessage());
                }
            }

        };
        
        protected  abstract void update(SolrClient solrClient, AbstractCheckDeleteService reqService, List<Pair<String, List<String>>> batch, Case cs) throws SolrServerException, IOException;
    }

    // ty ktere se resi au
    protected abstract Map<Case, List<Pair<String, List<String>>>> checkUpdate() throws IOException, SolrServerException;
    protected abstract List<String> checkDelete() throws IOException, SolrServerException;
    
    protected String getProcess(Case cs) {
        if (results !=null && results.has(cs.name())) {
            String optString = results.optString(cs.name());
            return optString;
        } else return null;
    }
    
    public void update() throws IOException, SolrServerException {
        try {
            //getLogger().log(Level.INFO, "Updating");
            Map<Case,List<Pair<String,List<String>>>> cases = checkUpdate();
            getLogger().log(Level.INFO, String.format("Cases %s", cases.keySet().toString()));
            for (Case cs : cases.keySet()) {
                String confProc = getProcess(cs);
                List<Pair<String, List<String>>> updates = cases.get(cs);
                getLogger().log(Level.INFO, String.format("Updating records, Case %s and number of records %d", cs.name(), updates.size()));
                switch (cs) {
                    case DNT_1:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) : Process.state, getLogger(),cs);
                        break;
                    case DNT_2:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.request,getLogger(),cs);
                        break;
                    case DNT_3:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.request,getLogger(),cs);
                        break;
                    case SKC_1:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.state,getLogger(),cs);
                        break;
                    case SKC_2a:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.request,getLogger(),cs);
                        break;
                    case SKC_2b:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.request,getLogger(),cs);
                        break;
                    case SKC_3:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.request,getLogger(),cs);
                        break;
                    case SKC_4:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.request,getLogger(),cs);
                        break;
                    case SKC_4a:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.state,getLogger(),cs);
                        break;
                    case SKC_4b:
                        updateRecords(updates, confProc != null ? Process.valueOf(confProc) :Process.request,getLogger(),cs);
                        break;
                    default:
                        break;
                }
            }
            
            //updateRecords(checkUpdate());
            deleteRecords(checkDelete());
        } catch(Exception e) {
            getLogger().log(Level.SEVERE,e.getMessage(),e);
        } finally {
            try (SolrClient solrClient = buildClient()) {
                SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
                SolrJUtilities.quietCommit(solrClient, DataCollections.zadost.name());
            }
        }
    }

    
    protected void deleteRecords(List<String> identifiers) throws IOException, SolrServerException{
        if (identifiers != null && !identifiers.isEmpty()) {
            try(SolrClient sClient = buildClient()) {
                sClient.deleteById(DataCollections.catalog.name(), identifiers);
            }
        }
    }

    protected void updateRecords(List<Pair<String, List<String>>> toUpdateRecords, Process process, Logger logger, Case cs) throws IOException, SolrServerException {
        long startUpdating = System.currentTimeMillis();
        //AtomicInteger counter = new AtomicInteger();
        int counter = 0;
        try(SolrClient sClient = buildClient()) {
            if(toUpdateRecords.size() > 0) {
                int batchSize = process.equals(Process.request) ? this.numberOfItems : DEFAULT_NUMBER_ITEMS_FOR_UPDATE;
                int numberOfBatches = toUpdateRecords.size() / batchSize;
                int modulo = toUpdateRecords.size() % batchSize;
                if (modulo > 0) { numberOfBatches += 1; }
                logger.info("Number of batches  "+numberOfBatches);
                for (int i = 0; i < numberOfBatches; i++) {
                    int fromIndex = i*batchSize;
                    int toIndex = Math.min((i+1)*batchSize, toUpdateRecords.size());
                    List<Pair<String,List<String>>> subList = toUpdateRecords.subList(fromIndex, toIndex);
                    try {
                        logger.info("Sublist size "+subList.size()+ " from index "+fromIndex +" to index "+toIndex );
                        process.update(sClient, this, subList,cs);
                        counter = counter+ subList.size();
                        long took = System.currentTimeMillis() - startUpdating;
                        debugMesage(counter, took,getLogger());
                    } catch (SolrServerException | IOException e) {
                        getLogger().log(Level.SEVERE,e.getMessage(),e);
                    }
                }
            }
        }
    }


    protected void debugMesage(int number, long took, Logger logger) {
        double seconds = took/1000;
        double docsPerSec = number / seconds;
        logger.log(Level.INFO, String.format("Counter: %d, seconds: %f, docsPerSec: %f",number,seconds, docsPerSec));
    }

}
