package cz.inovatika.sdnnt.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.indexer.models.Zadost;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.UserControler;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public JSONObject search(String q, String state, String navrh, String institution, User user, int rows, int page) throws SolrServerException, IOException {
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
                    .setFacet(true).addFacetField("typ", "state", "navrh","institution")
                    .setParam("json.nl", "arrntv")
                    .setFields("*,process:[json]");

            if (rows >0 ) query.setRows(rows);  else query.setRows(DEFAULT_SEARCH_RESULT_SIZE);
            if (page >= 0) query.setStart(rows*page);


            if (state != null) {
                query.addFilterQuery("state:" + state);
            }
            if (navrh != null) {
                query.addFilterQuery("navrh:" + navrh);
            }

            if (institution != null) {
                query.addFilterQuery("institution:"+institution);
            }

            if ("kurator".equals(user.role)) {
                query.addFilterQuery("-state:open");
                query.addSort(SolrQuery.SortClause.desc("state"));
                query.addSort(SolrQuery.SortClause.asc("indextime"));
            } else {
                query.addFilterQuery("user:" + user.username);
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

        return ret;
    }

    @Override
    public JSONObject saveRequest(String payload, User user) throws SolrServerException, IOException {
        Zadost zadost = Zadost.fromJSON(payload);
        zadost.user = user.username;
        zadost.institution = user.institution;
        return saveRequest(zadost);
    }

    // moved from Zadost
    private JSONObject saveRequest(Zadost zadost) {
        try (SolrClient solr = buildClient()) {
            DocumentObjectBinder dob = new DocumentObjectBinder();
            SolrInputDocument idoc = dob.toSolrInputDocument(zadost);
            ObjectMapper mapper = new ObjectMapper();
            JSONObject p = new JSONObject(mapper.writeValueAsString(zadost.process));
            idoc.setField("process", p.toString());
            //solr.addBean("zadost", zadost);
            solr.add("zadost", idoc);
            solr.commit("zadost");
            solr.close();
            return zadost.toJSON();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
