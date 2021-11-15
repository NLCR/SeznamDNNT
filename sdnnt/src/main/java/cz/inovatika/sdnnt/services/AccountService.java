package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public interface AccountService {

    public JSONObject getRequest(String id) throws SolrServerException, IOException;
    public JSONObject search(String q, String state, List<String> navrhy, String institution, String priority, String delegated, User user, int rows, int page) throws SolrServerException, IOException;
    public JSONObject saveRequest(String payload, User user, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException;
    public JSONObject saveCuratorRequest(String payload, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException;
    public JSONObject saveRequestWithFRBR(String payload, User user , String frbr, AccountServiceInform inform) throws SolrServerException, IOException, ConflictException;


    public JSONObject sendRequest(String payload) throws ConflictException;

}
