package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.exports.ExportType;

public interface ExportService {

        
    /**
     * Return one export
     */
    public JSONObject getExport(String exportName) throws SolrServerException, IOException,AccountException;
 
    /** 
     * Search inside exports. Result is filtered content of one export
     */
    public JSONObject searchInExport(Map<String, String>  request, String expName,String q ,  int page, int rows) throws SolrServerException, IOException;

    /**
     * Returns all exported files
     */
    public JSONObject exportFiles(String expName) throws SolrServerException, IOException;
    
    /**
     * Return conrent of one exported file
     */
    public byte[] exportedFile(String expName, String path) throws IOException;

    /**
     *  Search exports. 
     */
    public JSONObject search(String q, ExportType type,  int rows, int page) throws SolrServerException, IOException;

    public JSONObject setExportProcessed(String exportName) throws SolrServerException, IOException;

    public JSONObject createExport(String id, ExportType type, int numberOfDoc) throws SolrServerException, IOException;
    
    
    
    
    
    public JSONObject approveExportItem(String exportId, String identifier) throws SolrServerException, IOException;

    public JSONObject approveExport(String exportId) throws SolrServerException, IOException;
}
