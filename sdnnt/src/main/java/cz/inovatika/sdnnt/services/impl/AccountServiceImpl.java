package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.indexer.models.Zadost;
import cz.inovatika.sdnnt.services.AccountService;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;

public class AccountServiceImpl implements AccountService {

    @Override
    public JSONObject search(String q, String state, String navrh, User user) throws SolrServerException, IOException {
        NamedList<Object> qresp = null;
        JSONObject ret = new JSONObject();
        Options opts = Options.getInstance();
        try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
            //String q = req.getParameter("q");
            if (q == null) {
                q = "*";
            }

            SolrQuery query = new SolrQuery(q)
                    .setRows(20)
                    .setParam("df", "fullText")
                    .setFacet(true).addFacetField("typ", "state", "navrh")
                    .setParam("json.nl", "arrntv")
                    .setFields("*,process:[json]");
            if (state != null) {
                query.addFilterQuery("state:" + state);
            }
            if (navrh != null) {
                query.addFilterQuery("navrh:" + navrh);
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
        // validace pidu
        JSONObject json = Zadost.save(payload, user.username);
        return json;
    }
}
