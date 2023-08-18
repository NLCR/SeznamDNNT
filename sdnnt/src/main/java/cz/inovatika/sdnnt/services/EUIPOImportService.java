package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;

/** 
 * 
 * @author happy
 */
public interface EUIPOImportService extends LoggerAware{

    public List<String> check(String format);

    public int update(String format, String exortIdentifier, List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

    public void createExport(String exportIdentifier, int numberOfDocs) throws AccountException, IOException, ConflictException, SolrServerException;
    
    public String getLastExportedDate();

    void iterationConfig(JSONObject iteration);

    void iterationResults(JSONObject results);

}
