package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.Streams;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.index.utils.QueryUtils;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.AccountServiceInform;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ExportService;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.exports.ExportType;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SearchResultsUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.VersionStringCast;
import cz.inovatika.sdnnt.utils.ZadostUtils;

public class ExportServiceImpl implements ExportService {

    public static final Logger LOGGER = Logger.getLogger(ExportServiceImpl.class.getName());
    
    private static final int DEFAULT_SEARCH_RESULT_SIZE = 20;

    private ApplicationUserLoginSupport loginSupport;
    private ResourceServiceService resourceServiceService;
    
    public ExportServiceImpl( ApplicationUserLoginSupport loginSupport, ResourceServiceService res) {
        this.loginSupport = loginSupport;
        this.resourceServiceService = res;
    }


    
    @Override
    public JSONObject searchInExport(String exportName,String q, int page, int rows)
            throws SolrServerException, IOException {
        
        CatalogSearcher searcher = new CatalogSearcher();
        Map<String,String> req = new HashMap<>();
        req.put("rows", ""+rows);
        req.put("page", ""+page);
        req.put("q", q);
        
        JSONObject result = searcher.search(req, Arrays.asList(String.format("id_euipo_export:%s", exportName)), loginSupport.getUser());
        return result;
    }


    

    @Override
    public JSONObject getExport(String exportName) throws SolrServerException, IOException {
        NamedList<Object> qresp = null;
        JSONObject ret = new JSONObject();
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("*")
                    .setFacet(true).addFacetField("export_type")
                    .setParam("json.nl", "arrntv")
                    .setFields("*,process:[json]")
                    .setFilterQueries("id:"+exportName);

            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            qresp = solr.request(qreq, DataCollections.exports.name());
            solr.close();
            ret = new JSONObject((String) qresp.get("response"));
        }
        return ret;
    }



    @Override
    public JSONObject exportFiles(String expName) throws IOException {
        List<File> exportedFiles = exportFilesImpl(expName);
        JSONArray jsonArr = new JSONArray();
        for (File file : exportedFiles) {
            JSONObject obj = new JSONObject();
            obj.put("name", file.getName());
            obj.put("path", file.getAbsolutePath());
            jsonArr.put(obj);
        }
        JSONObject retval = new JSONObject();
        retval.put("files", jsonArr);
        return retval;
}

    private List<File> exportFilesImpl(String expName) throws IOException {
        try {
            List<File> exportFiles = new ArrayList<>();
            JSONObject export = getExport(expName);
            JSONObject response = export.getJSONObject("response");
            int numFound = response.getInt("numFound");
            if (numFound == 1) {
                JSONArray docs = response.getJSONArray("docs");
                JSONObject exportDoc = docs.getJSONObject(0);
                String expFolder = exportDoc.optString("export_folder");
                if (expFolder != null) {
                    File folder = new File(expFolder);
                    if (folder.exists() && folder.isDirectory()) {
                        File[] listFiles = folder.listFiles();
                        if (listFiles != null) {
                            for (File file : listFiles) {
                                exportFiles.add(file);
                            }
                        }
                    }
                }
            }
            return exportFiles;
        } catch (SolrServerException e) {
            throw new IOException(e.getMessage());
        }
    }


    @Override
    public byte[] exportedFile(String expName, String path) throws IOException {
        List<File> exportFilesImpl = exportFilesImpl(expName);
        List<String> absPaths = exportFilesImpl.stream().map(File::getAbsolutePath).collect(Collectors.toList());
        if (absPaths.contains(path) ) {
            File f = new File(path);
            return IOUtils.toByteArray(new FileInputStream(f));
        } throw new IOException(String.format("Path not found %s", path));
    }

    
    


    @Override
    public JSONObject setExportProcessed(String exportName) throws SolrServerException, IOException {
        JSONObject export = getExport(exportName);
        JSONObject responseJSON = export.getJSONObject("response");
        int optInt = responseJSON.optInt("numFound",0);
        if (optInt > 0) {
            try (SolrClient solr = buildClient()) {
                UpdateRequest uReq = new UpdateRequest();

                SolrInputDocument idoc = new SolrInputDocument();
                idoc.setField("id", exportName);

                
                SolrJUtilities.atomicSet(idoc, true, "export_processed");

                uReq.add(idoc);
                UpdateResponse response = uReq.process(solr, DataCollections.exports.name());
                SolrJUtilities.quietCommit(solr, DataCollections.exports.name());
            }
        }
        return new JSONObject();
    }



    @Override
    public JSONObject search(String q, ExportType type, int rows, int page)
            throws SolrServerException, IOException {
        
        CatalogSearcher searcher = new CatalogSearcher();
        Map<String,String> req = new HashMap<>();
        req.put("rows", "0");
        req.put("q", q);
        
        List<String> allFacets  = new ArrayList<>();

        JSONObject result = searcher.search(req, new ArrayList<>(), loginSupport.getUser());
        if (result.has(SearchResultsUtils.FACET_COUNTS_KEY)) {
            // filtr public state from kurator state
            JSONObject fCounts = result.getJSONObject(SearchResultsUtils.FACET_COUNTS_KEY);
            if (fCounts.has(SearchResultsUtils.FACET_FIELDS_KEY)) {
                JSONObject fFields = fCounts.getJSONObject(SearchResultsUtils.FACET_FIELDS_KEY);
                if (fFields.has(MarcRecordFields.ID_EUIPO_EXPORT)) {
                    JSONArray idEuipoArray = fFields.getJSONArray(MarcRecordFields.ID_EUIPO_EXPORT);
                    idEuipoArray.forEach(obj-> {
                        JSONObject valObj = (JSONObject) obj;
                        allFacets.add(valObj.getString("name"));
                    });
                }
            }
        }

        
        
        NamedList<Object> qresp = null;
        JSONObject ret = new JSONObject();
        try (SolrClient solr = buildClient()) {
            // pokud je null nebo neco naslo hledani v katalogu
            if (q == null || allFacets.size() > 0) {
                q = "*";
            } else {
                q = QueryUtils.query(q);
            }
            SolrQuery query = new SolrQuery(q)
                    .setFacet(true).addFacetField("export_type")
                    .setParam("json.nl", "arrntv")
                    .setFields("*,process:[json]");
            //query.setSort("export_processed", )
            //query.addSort("export_processed", );
            query.addSort(SortClause.asc("export_processed"));
            query.addSort(SortClause.desc("indextime"));
            
            
            if (rows >0 ) query.setRows(rows);  else query.setRows(DEFAULT_SEARCH_RESULT_SIZE);
            if (page >= 0) query.setStart(rows*page);
            
            if (type != null) {
                query.addFilterQuery("export_type:"+type.name());
            }

            if (allFacets.size() > 0) {
                String exportIdQuery = allFacets.stream().collect(Collectors.joining(" OR "));
                query.addFilterQuery("id:("+exportIdQuery+")");
            }
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            
            
            qreq.setResponseParser(rParser);

            qresp = solr.request(qreq, DataCollections.exports.name());
            solr.close();
            ret = new JSONObject((String) qresp.get("response"));
        }
        
        return ret;
    }
    

    @Override
    public JSONObject createExport(String id, ExportType type, int numberOfDoc)
            throws SolrServerException, IOException {
        try (SolrClient solr = buildClient()) {
            
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.addField("id", id);
            idoc.addField("export_num_docs", numberOfDoc);
            idoc.addField("export_type", type.name());

            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.add(idoc);
            updateRequest.setCommitWithin(200);

            UpdateResponse uResponse = updateRequest.process(solr, DataCollections.exports.name());
            SolrJUtilities.quietCommit(solr, "zadost");
            return new JSONObject();

        } catch(BaseHttpSolrClient.RemoteSolrException ex) {
            if (ex.code() == 409) {
                LOGGER.log(Level.SEVERE, null, ex);
                return new JSONObject().put("error", ex);
            } else {
                LOGGER.log(Level.SEVERE, null, ex);
                return new JSONObject().put("error", ex);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }
    


    public SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
