package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.User;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;

import java.io.IOException;

public interface AccountService {

    public JSONObject search(String q, String state, String navrh, String institution, String priority, String delegated, User user,int rows, int page) throws SolrServerException, IOException;
    public JSONObject saveRequest(String payload, User user) throws SolrServerException, IOException;

    public JSONObject saveCuratorRequest(String payload) throws SolrServerException, IOException;

}
