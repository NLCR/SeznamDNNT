package cz.inovatika.sdnnt.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.model.DeadlineType;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;
import cz.inovatika.sdnnt.model.workflow.zadost.AbstractZadostWorkflow;
import cz.inovatika.sdnnt.model.workflow.zadost.NZNWorkflow;
import cz.inovatika.sdnnt.model.workflow.zadost.ZadostProxy;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.AccountServiceInform;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.SolrUtils;
import cz.inovatika.sdnnt.utils.VersionStringCast;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;

public class AccountServiceImpl implements AccountService {

    public static Logger LOGGER = Logger.getLogger(AccountServiceImpl.class.getName());

    private static final int DEFAULT_SEARCH_RESULT_SIZE = 20;

    private UserControler userControler;
    public AccountServiceImpl(UserControler userControler) {
        this.userControler = userControler;
    }

    public AccountServiceImpl() {
    }

    @Override
    public JSONObject search(String q, String state, List<String> navrhy, String institution, String priority, String delegated, User user, int rows, int page) throws SolrServerException, IOException {
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

            if (Role.kurator.name().equals(user.role) || Role.mainKurator.name().equals(user.role)) {
                query.addFilterQuery("-state:open");
                query.addSort(SolrQuery.SortClause.desc("state"));
                query.addSort(SolrQuery.SortClause.asc("indextime"));
            } else {
                query.addFilterQuery("user:" + user.username);

                // sort
                query.addSort(SolrQuery.SortClause.asc("state"));
                query.addSort(SolrQuery.SortClause.desc("datum_zadani"));
            }
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            qresp = solr.request(qreq, "zadost");
            solr.close();
            ret = new JSONObject((String) qresp.get("response"));
        }

        return VersionStringCast.cast(ret);
    }

    @Override
    public JSONObject saveRequest(String payload, User user, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException {
        Zadost zadost = Zadost.fromJSON(payload);
        zadost.setUser(user.username);
        zadost.setInstitution(user.institution);
        return saveRequest(zadost, inform);
    }

    public JSONObject saveCuratorRequest(String payload, AccountServiceInform inform) throws JsonProcessingException, ConflictException {
       Zadost zadost = Zadost.fromJSON(payload);
       return saveRequest(zadost, inform);
    }

    // moved from Zadost
    private JSONObject saveRequest(Zadost zadost,AccountServiceInform inform) throws ConflictException {
        //SolrClient solr = null;
        try (SolrClient solr = buildClient()) {

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
            return zadost.toJSON();
        } catch(BaseHttpSolrClient.RemoteSolrException ex) {
            if (ex.code() == 409) {
                //LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                throw new ConflictException("Cannot save, conflict");
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
    public JSONObject sendRequest(String payload) throws ConflictException{
        Zadost zadost = Zadost.fromJSON(payload);
        zadost.setUser(this.userControler.getUser().username);
        zadost.setDatumZadani(new Date());
        //zadost.setState("waiting");

        Workflow wfl = AbstractZadostWorkflow.create(zadost);

        WorkflowState wflState = wfl.nextState();
        // prepnuti do stavu
        wflState.applyState();

        DeadlineType deadlineType = DeadlineType.valueOf(zadost.getTypeOfDeadline());
        switch (deadlineType) {
            case scheduler:
                zadost.setState("waiting_for_automatic_process");
                zadost.setTypeOfPeriod(wflState.getPeriod().name());
            case kurator:
                zadost.setState("waiting");
                zadost.setTypeOfPeriod(wflState.getPeriod().name());
        }

        return  saveRequest(zadost, null);
    }



    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }


}
