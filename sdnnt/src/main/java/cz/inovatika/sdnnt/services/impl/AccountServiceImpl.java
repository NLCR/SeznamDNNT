package cz.inovatika.sdnnt.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.model.TransitionType;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;
import cz.inovatika.sdnnt.model.workflow.zadost.ZadostWorkflowFactory;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.AccountServiceInform;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.*;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AccountServiceImpl implements AccountService {

    public static Logger LOGGER = Logger.getLogger(AccountServiceImpl.class.getName());

    private static final int DEFAULT_SEARCH_RESULT_SIZE = 20;

    private UserControler userControler;
    private ResourceServiceService resourceServiceService;

    public AccountServiceImpl(UserControler userControler, ResourceServiceService res) {
        this.userControler = userControler;
        this.resourceServiceService = res;
    }

    public AccountServiceImpl(ResourceServiceService res) {
        this.resourceServiceService = res;
    }

    public AccountServiceImpl(UserControler userControler) {
        this.userControler = userControler;
    }

    public AccountServiceImpl() {}

    @Override
    public JSONObject search(String q, String state, List<String> navrhy, String institution, String priority, String delegated, String sort, User user, int rows, int page) throws SolrServerException, IOException {
        NamedList<Object> qresp = null;
        JSONObject ret = new JSONObject();
        Options opts = Options.getInstance();
        try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
            //String q = req.getParameter("q");
            if (q == null) {
                q = "*";
            }

            SolrQuery query = new SolrQuery(q)
                    .setParam("df", "fullText")
                    .setFacet(true).addFacetField("typ", "state", "navrh","institution", "delegated", "priority")
                    .setParam("json.nl", "arrntv")
                    .setFields("*,process:[json]");

            if (rows >0 ) query.setRows(rows);  else query.setRows(DEFAULT_SEARCH_RESULT_SIZE);
            if (page >= 0) query.setStart(rows*page);


            if (state != null) {
                query.addFilterQuery("state:" + state);
            }
            if (navrhy != null && !navrhy.isEmpty()) {
                navrhy.stream().filter(Objects::nonNull).forEach(navrh -> {
                    query.addFilterQuery("navrh:" + navrh);
                });
            }

            if (institution != null) {
                query.addFilterQuery("institution:"+institution);
            }

            if (delegated != null) {
                query.addFilterQuery("delegated:\""+delegated+"\"");
            }
            if (priority != null) {
                query.addFilterQuery("priority:\""+priority+"\"");
            }

            if (sort != null) {
                String[] s = sort.split(" ");
                if (s.length == 2) {
                    query.addSort(s[0], SolrQuery.ORDER.valueOf(s[1].toLowerCase().trim()));
                }
            }

            if (Role.kurator.name().equals(user.role) || Role.mainKurator.name().equals(user.role)) {
                query.addFilterQuery("-state:open");
                query.addSort(SolrQuery.SortClause.desc("state"));
                if (sort == null) {
                    query.addSort(SolrQuery.SortClause.asc("indextime"));
                }

            } else {
                query.addFilterQuery("user:" + user.username);
                query.addSort(SolrQuery.SortClause.asc("state"));
                if (sort == null) {
                    query.addSort(SolrQuery.SortClause.desc("datum_zadani"));
                }
            }
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            qresp = solr.request(qreq, "zadost");
            solr.close();
            ret = new JSONObject((String) qresp.get("response"));

            JSONArray docs = ret.getJSONObject("response").getJSONArray("docs");
            for (int i = 0; i < docs.length(); i++) {
                JSONObject zadostJSON = docs.getJSONObject(i);
                Zadost zadost = Zadost.fromJSON(zadostJSON.toString());
                if (zadost.isExpired()) {
                    zadostJSON.put("expired", true);
                } else if (zadost.isEscalated()) {
                    zadostJSON.put("escalated", true);
                }
            }
        }

        return VersionStringCast.cast(ret);
    }

    @Override
    public JSONObject saveRequest(String payload, User user, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException, AccountException {
        Zadost zadost = Zadost.fromJSON(payload);
        if (zadost.getState().equals("open")) {
            zadost.setUser(user.username);
            zadost.setInstitution(user.institution);
            JSONObject jsonObject = saveRequest(zadost, inform);
            return VersionStringCast.cast(jsonObject);
        } else {
            if (this.resourceServiceService != null) {
                throw new AccountException("account.notopened", this.resourceServiceService.getBundle().getString("account.notopened"));
            } else {
                throw new AccountException("account.notopened", "account.notopened");
            }
        }
    }

    public JSONObject saveCuratorRequest(String payload, AccountServiceInform inform) throws JsonProcessingException, ConflictException {
       Zadost zadost = Zadost.fromJSON(payload);
       return saveRequest(zadost, inform);
    }

    // moved from Zadost
    private JSONObject saveRequest(Zadost zadost,AccountServiceInform inform) throws ConflictException {
        //SolrClient solr = null;
        try (SolrClient solr = buildClient()) {


            if (zadost.getIdentifiers().size() > MAXIMUM_ITEMS_IN_ZADOST) {
                throw new AccountException("account.maximum_items_exceed", "Maximalni pocet polozek prekrocen");
            }

            SolrInputDocument idoc = zadost.toSolrInputDocument();

            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.add(idoc);
            updateRequest.setCommitWithin(200);
            if (zadost.getVersion() != null) {
                updateRequest.setParam("_version_", "" + zadost.getVersion());
            }

            UpdateResponse uResponse = updateRequest.process(solr, "zadost");
            if (inform != null) {
                // inform saved
                inform.saved(zadost);
            }
            SolrUtils.quietCommit(solr, "zadost");
            return VersionStringCast.cast(getRequest(zadost.getId()));
            //return VersionStringCast.cast(zadost.toJSON());
        } catch(BaseHttpSolrClient.RemoteSolrException ex) {
            if (ex.code() == 409) {
                if (this.resourceServiceService != null) {
                    throw new ConflictException("account.conflict", this.resourceServiceService.getBundle().getString("account.conflict"));
                } else {
                    throw new ConflictException("account.conflict", "account.conflict");
                }
            } else {
                LOGGER.log(Level.SEVERE, null, ex);
                return new JSONObject().put("error", ex);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }


    @Override
    //public static JSONObject saveWithFRBR(String js, String username, String frbr) {
    public JSONObject saveRequestWithFRBR(String payload, User user , String frbr, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException {
        try {
            Zadost zadost = Zadost.fromJSON(payload);
            zadost.setUser(user.username);
            try (SolrClient solr = buildClient()) {
                SolrQuery query = new SolrQuery("frbr:\"" + frbr + "\"")
                        .setFields("identifier")
                        .setRows(10000);
                SolrDocumentList docs = solr.query("catalog", query).getResults();
                for (SolrDocument doc : docs) {
                    zadost.addIdentifier((String) doc.getFirstValue("identifier"));
                }
                return saveRequest(zadost, inform);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    @Override
    public JSONObject getRequest(String id) throws SolrServerException, IOException {
      try (SolrClient solr = buildClient()) {

          SolrQuery query = new SolrQuery("id:" + id)
                  .setRows(1).setFields("*,process:[json]");

          QueryRequest qreq = new QueryRequest(query);
          NoOpResponseParser rParser = new NoOpResponseParser();
          rParser.setWriterType("json");
          qreq.setResponseParser(rParser);
          NamedList<Object> qresp = solr.request(qreq, "zadost");
          String response = (String) qresp.get("response");

          JSONArray docs = new JSONObject(response).getJSONObject("response").getJSONArray("docs");
          return docs.length() > 0 ? VersionStringCast.cast(docs.getJSONObject(0)) : null;
        }
    }

    @Override
    public JSONObject curatorCloseRequest(String payload) throws ConflictException, AccountException {
        Zadost zadost = Zadost.fromJSON(payload);
        zadost.setKurator(this.userControler.getUser().username);
        zadost.setDatumVyrizeni(new Date());
        return closeRequest(zadost, "processed");
    }

    @Override
    public JSONObject userCloseRequest(String payload) throws ConflictException{
        Zadost zadost = Zadost.fromJSON(payload);
        zadost.setUser(this.userControler.getUser().username);
        zadost.setDatumZadani(new Date());

        return closeRequest(zadost, "waiting");
    }

    private JSONObject closeRequest(Zadost zadost, String requestCloseState) throws ConflictException {
        Workflow wfl = ZadostWorkflowFactory.create(zadost);
        if (!wfl.isClosableState()) {
            WorkflowState wflState = wfl.nextState();
            wflState.switchState( zadost.getId() , zadost.getUser(), zadost.getId());
            TransitionType transitionType = TransitionType.valueOf(zadost.getTransitionType());
            switch (transitionType) {
                case scheduler:
                    zadost.setState("waiting_for_automatic_process");
                    zadost.setTypeOfPeriod(wflState.getPeriod().name());
                    break;
                case kurator:
                    zadost.setState(requestCloseState);
                    zadost.setTypeOfPeriod(wflState.getPeriod().name());
                    break;
            }
        } else {
            zadost.setState(requestCloseState);
            zadost.setTypeOfPeriod(null);
            zadost.setDeadline(null);
            zadost.setDesiredItemState(null);
            zadost.setDesiredLicense(null);
        }
        LOGGER.info(String.format("Deadline for zadost %s", zadost.getDeadline() != null ? zadost.getDeadline().toString() : null));
        return  VersionStringCast.cast(saveRequest(zadost, null));
    }


    @Override
    public JSONObject deleteRequest(String payload) throws ConflictException, AccountException, IOException, SolrServerException {
        try (SolrClient solr = buildClient()) {
            Zadost zadost = Zadost.fromJSON(payload);
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.deleteById(zadost.getId());
            updateRequest.setCommitWithin(200);
            if (zadost.getVersion() != null) {
                updateRequest.setParam("_version_", "" + zadost.getVersion());
            }
            UpdateResponse uResponse = updateRequest.process(solr, "zadost");
            SolrUtils.quietCommit(solr, "zadost");
        }
        return  new JSONObject(payload);
    }

    @Override
    public JSONObject getRecords(String id, int rows, int start) throws SolrServerException, IOException, ConflictException, AccountException {
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("*:*")
                    .setRows(rows)
                    .setStart(start)
                    .addFilterQuery("{!join fromIndex=zadost from=identifiers to=identifier} id:" + id)
                    .setSort(SolrQuery.SortClause.asc("title_sort"))
                    .setFields("*,raw:[json],granularity:[json],historie_stavu:[json],historie_kurator_stavu:[json]");
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "catalog");
            JSONObject ret = new JSONObject((String) qresp.get("response"));

            List<String> ids = SearchResultsUtils.getIdsFromResult(ret);
            if (this.userControler.getUser() != null) {
                JSONArray notifications = NotificationUtils.findNotifications(ids, this.userControler.getUser().username, solr);
                ret.put("notifications", notifications);
            }

            return ret;
        }
    }

    @Override
    public JSONObject curatorRejectSwitchState(String zadostId, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException {
        JSONObject zadostJSON = getRequest(zadostId);
        if (zadostJSON != null) {
            try {
                String username = userControler.getUser().username;
                Zadost.reject(documentId,zadostJSON.toString(),  reason, username);
                return VersionStringCast.cast(getRequest(zadostId));
            } catch (JSONException e) {
                throw new AccountException("account.rejecterror", "account.rejecterror");
            }
        } else {
            throw new AccountException("account.notfound", "account.notfound");
        }
    }

    @Override
    public JSONObject curatorSwitchAlternativeState(String alternative, String zadostId, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException {
        JSONObject zadostJSON = getRequest(zadostId);
        if (zadostJSON != null) {
            Zadost zadost = Zadost.fromJSON(zadostJSON.toString());
            try (SolrClient solr = buildClient()) {
                MarcRecord marcRecord = MarcRecord.fromIndex(solr, documentId);

                Workflow workflow = DocumentWorkflowFactory.create(marcRecord, zadost);
                if (workflow.isAlternativeSwitchPossible(alternative)) {

                    // prene se a
                    WorkflowState workflowState = workflow.nextAlternativeState(alternative);
                    workflowState.switchState(zadostId, userControler.getUser().username, reason);

                    solr.add("catalog", marcRecord.toSolrDoc());
                    solr.commit("catalog");

                    Zadost.approve(solr, marcRecord.identifier, zadostJSON.toString(),
                            reason,userControler.getUser().username,null);

                }
            }
        }

        return getRequest(zadostId);
    }

    public JSONObject curatorSwitchState(String zadostId, String documentId, String reason) throws ConflictException, AccountException, IOException, SolrServerException {
        JSONObject zadostJSON = getRequest(zadostId);
        if (zadostJSON != null) {
            LOGGER.info(String.format("Processing zadost id %s", zadostId));
            Zadost zadost = Zadost.fromJSON(zadostJSON.toString());
            try (SolrClient solr = buildClient()) {
                MarcRecord marcRecord = MarcRecord.fromIndex(solr, documentId);

                Workflow workflow = DocumentWorkflowFactory.create(marcRecord, zadost);
                if (workflow != null) {
                    if (workflow.isSwitchPossible()) {

                        // prene se a
                        WorkflowState workflowState = workflow.nextState();
                        if (workflowState.getPeriod()  == null || workflowState.getPeriod().getTransitionType().equals(TransitionType.kurator)) {
                            workflowState.switchState(zadostId, userControler.getUser().username, reason);

                            solr.add("catalog", marcRecord.toSolrDoc());
                            solr.commit("catalog");

                            Zadost.approve(solr, marcRecord.identifier, zadostJSON.toString(),
                                    reason,userControler.getUser().username,null);

                        } else  throw new AccountException("account.nocuratorworkflow", "account.nocuratorworkflow");
                    } else throw new AccountException("account.noworkflowstates", "account.noworkflowstates");
                } else throw new AccountException("account.noworkflow", "account.noworkflow");

            }

        }

        return getRequest(zadostId);

    }

    public void schedulerSwitchStates() throws ConflictException, AccountException, IOException, SolrServerException {
        List<String> identifiers = new ArrayList<>();
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("*")
                    .setFilterQueries("deadline:[* TO NOW]")
                    .setRows(1000)
                    .setFields("id")
                    .addFilterQuery("state:waiting_for_automatic_process")
                    .setSort(SolrQuery.SortClause.asc("id"));


            //query.addFilterQuery("state:waiting_for_automatic_process");
            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            boolean done = false;
            while (!done) {
                query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse rsp = solr.query("zadost", query);
                String nextCursorMark = rsp.getNextCursorMark();
                for (SolrDocument resultDoc : rsp.getResults()) {
                    Object id = resultDoc.getFieldValue("id");
                    identifiers.add(id.toString());
                }
                if (cursorMark.equals(nextCursorMark)) {
                    done = true;
                }
                cursorMark = nextCursorMark;
            }
        }

        if (!identifiers.isEmpty()) {
            for (String id :  identifiers) {  schedulerSwitchStates(id); }
        }

    }

    @Override
    public void schedulerSwitchStates(String id) throws ConflictException, AccountException, IOException, SolrServerException {
        JSONObject request = getRequest(id);
        if ( request != null) {
            Zadost zadost = Zadost.fromJSON(request.toString());
            List<SolrDocument> docsFromResult = new ArrayList<>();
            try (SolrClient solr = buildClient()) {

                SolrQuery query = new SolrQuery("*:*")
                        .setRows(MAXIMUM_ITEMS_IN_ZADOST)
                        .addFilterQuery("{!join fromIndex=zadost from=identifiers to=identifier} id:"+'"' + id+'"');

                QueryResponse response =  solr.query("catalog", query);
                SolrDocumentList results = response.getResults();
                for (long i=0,ll=results.getNumFound();i<ll;i++) {
                    docsFromResult.add(results.get((int)i));
                }
            }

            LOGGER.info(String.format("Switching states for request %s", id));

            if (docsFromResult != null) {
                docsFromResult.stream().forEach(j-> {
                    try {
                        MarcRecord marcRecord = MarcRecord.fromDoc(j);
                        //MarcRecord.fromDoc()
                        Workflow workflow = DocumentWorkflowFactory.create(marcRecord, zadost);
                        if(workflow != null) {
                            boolean switchPossible = workflow.isSwitchPossible();
                            if (switchPossible) {
                                WorkflowState workflowState = workflow.nextState();

                                // stav musi existovat a musi byt typu scheduler
                                if (workflowState != null && workflowState.getPeriod() !=null && workflowState.getPeriod().getTransitionType().equals(TransitionType.scheduler)) {


                                    //marcRecord.toSolrDoc();
                                    String oldRaw = marcRecord.toJSON().toString();
                                    // prepnuti stavu
                                    workflowState.switchState(id, zadost.getUser(), "");
                                    try {
                                        try (SolrClient docClient = buildClient()) {
                                            new HistoryImpl(docClient).log(marcRecord.identifier, oldRaw, marcRecord.toJSON().toString(), zadost.getId(), "catalog");
                                            docClient.add("catalog", marcRecord.toSolrDoc());
                                            docClient.commit("catalog");
                                        }
                                    } catch (IOException | SolrServerException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(),e);
                                    }

                                    LOGGER.info(String.format("\tautomatic switch,  request(%s, %s)  = doc(%s, %s)", zadost.getNavrh(), zadost.getId(), marcRecord.identifier ,""+(marcRecord.dntstav+" "+marcRecord.kuratorstav +( marcRecord.license != null  ? " / "+marcRecord.license : ""))));
                                }
                            } else {
                                LOGGER.info(String.format("\tnot accepte, request(%s, %s) =   doc(%s)", zadost.getNavrh(), zadost.getId(), marcRecord.identifier ));
                            }
                        }
                    } catch (JsonProcessingException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(),e);
                    }
                });
            }

            // close whole request
            Workflow wfl = ZadostWorkflowFactory.create(zadost);
            if (wfl.isSwitchPossible()) {
                WorkflowState wflState = wfl.nextState();
                wflState.switchState( zadost.getId() , zadost.getUser(), zadost.getId());
                TransitionType transitionType = TransitionType.valueOf(zadost.getTransitionType());
                switch (transitionType) {
                    case scheduler:
                        // ve finalnim stavu po prepnuti
                        zadost.setState( "waiting_for_automatic_process");
                        zadost.setTypeOfPeriod(wflState.getPeriod().name());
                        break;
                    case kurator:
                        zadost.setState("waiting");
                        zadost.setTypeOfPeriod(wflState.getPeriod().name());
                        break;
                }
            } else {
                zadost.setState("processed");
                zadost.setTypeOfPeriod(null);
                zadost.setDeadline(null);
                zadost.setDesiredLicense(null);
                zadost.setDesiredItemState(null);
            }

            try (SolrClient solr = buildClient()) {
                Zadost.save(solr, zadost);
            }
        }
    }

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }


}
