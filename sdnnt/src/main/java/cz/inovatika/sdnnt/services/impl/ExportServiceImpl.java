package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.AccountServiceInform;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.EUIPOImportService;
import cz.inovatika.sdnnt.services.ExportService;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.exports.ExportType;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
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
    public JSONObject searchInExport(Map<String, String>  givenReq,  String exportName,String q, int page, int rows)
            throws SolrServerException, IOException {
        
        CatalogSearcher searcher = new CatalogSearcher();
        Map<String,String> searchReq = new HashMap<>();
        
        givenReq.keySet().forEach(key-> {searchReq.put(key, givenReq.get(key));});
        
        searchReq.put("rows", ""+rows);
        searchReq.put("page", ""+page);
        searchReq.put("q", q);
        searchReq.put("fullCatalog","true");
        // search 
        
        
        JSONObject result = searcher.search(searchReq, Arrays.asList(String.format("id_euipo_export:%s", exportName)), loginSupport.getUser());
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
        // validace ??  Vsechny objekty, ktere maji aktivni export 'exportName' a zaroven musi byt vsechny identifikatory 
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
        return getExport(exportName);
    }

    
    





    public JSONObject approveExportItemUOCP(String exportId, String identifier) throws SolrServerException, IOException {
        List<Pair<String, List<String>>> discardedEuipoIdentifiers = discardedEuipoIdentifiers(exportId);
        JSONObject export = getExport(exportId);
        //String type = "IOCP";
        List<String> exportedIdentifiers = new ArrayList<>();
        JSONArray jsonArray = export.getJSONObject("response").getJSONArray("docs");
        if (jsonArray.length() > 0) {
            JSONArray exporetedIdents = jsonArray.getJSONObject(0).optJSONArray("exported_identifiers");
            //type = jsonArray.getJSONObject(0).optString("export_type");
            if (exporetedIdents != null) {
                exporetedIdents.forEach(e-> exportedIdentifiers.add(e.toString()));
            }
        }
        try (SolrClient solr = buildClient()) {
            if (!exportedIdentifiers.contains(identifier)) {
                
                exportedIdentifiers.add(identifier);
                
                /** catalog update */
                UpdateRequest recordItem = new UpdateRequest();
                SolrInputDocument idoc = new SolrInputDocument();
                idoc.setField(IDENTIFIER_FIELD, identifier);
                
                SolrJUtilities.atomicRemove(idoc, "euipo", MarcRecordFields.EXPORT);

//                if ("IOCP".equals(type)) {
//                } else {
//                    SolrJUtilities.atomicRemove(idoc, "euipo", MarcRecordFields.EXPORT);
//                    
//                }
                

                recordItem.add(idoc);
                UpdateResponse cResponse = recordItem.process(solr, DataCollections.catalog.name());
                
                /** Export; exported identifiers */
                UpdateRequest exportReq = new UpdateRequest();
                SolrInputDocument exportDoc = new SolrInputDocument();
                exportDoc.setField("id", exportId);
                // skc identifier
                SolrJUtilities.atomicAddDistinct(exportDoc, identifier, "exported_identifiers");
                
                List<String> discardedSKCIdentifiers = discardedEuipoIdentifiers.stream().map(Pair::getLeft).collect(Collectors.toList());

                Collections.sort(discardedSKCIdentifiers);
                Collections.sort(exportedIdentifiers);
                if (discardedSKCIdentifiers.equals(exportedIdentifiers)) {
                    // muze se zavrit 
                    SolrJUtilities.atomicSet(exportDoc, true, "all_exported_identifiers_flag");
                }
                
                exportReq.add(exportDoc);
                
                UpdateResponse eResponse = exportReq.process(solr, DataCollections.exports.name());
                SolrJUtilities.quietCommit(solr, DataCollections.exports.name());
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());

                exportedIdentifiers.add(identifier);
                

            }
        }
        
        return getExport(exportId);
    }

    @Override
    public JSONObject approveExportItemIOCP(String exportId, String identifier) throws SolrServerException, IOException {

        List<Pair<String, String>> euipoIdentifiers = euipoIdentifiers(exportId);
        JSONObject export = getExport(exportId);
        //String type = "IOCP";
        List<String> exportedIdentifiers = new ArrayList<>();
        JSONArray jsonArray = export.getJSONObject("response").getJSONArray("docs");
        if (jsonArray.length() > 0) {
            JSONArray exporetedIdents = jsonArray.getJSONObject(0).optJSONArray("exported_identifiers");
            //type = jsonArray.getJSONObject(0).optString("export_type");
            if (exporetedIdents != null) {
                exporetedIdents.forEach(e-> exportedIdentifiers.add(e.toString()));
            }
        }
        
        try (SolrClient solr = buildClient()) {
            if (!exportedIdentifiers.contains(identifier)) {
                /** catalog update */
                UpdateRequest recordItem = new UpdateRequest();
                SolrInputDocument idoc = new SolrInputDocument();
                idoc.setField(IDENTIFIER_FIELD, identifier);
                
                SolrJUtilities.atomicAddDistinct(idoc, "euipo", MarcRecordFields.EXPORT);
                exportedIdentifiers.add(identifier);
                
//                if ("IOCP".equals(type)) {
//                } else {
//                    SolrJUtilities.atomicRemove(idoc, "euipo", MarcRecordFields.EXPORT);
//                    
//                }
                

                recordItem.add(idoc);
                UpdateResponse cResponse = recordItem.process(solr, DataCollections.catalog.name());
                
                /** Export; exported identifiers */
                UpdateRequest exportReq = new UpdateRequest();
                SolrInputDocument exportDoc = new SolrInputDocument();
                exportDoc.setField("id", exportId);
                // skc identifier
                SolrJUtilities.atomicAddDistinct(exportDoc, identifier, "exported_identifiers");
                
                List<String> exportedSKCIdentifiers = euipoIdentifiers.stream().map(Pair::getLeft).collect(Collectors.toList());
                
                
                Collections.sort(exportedIdentifiers);
                Collections.sort(exportedSKCIdentifiers);
                if (exportedSKCIdentifiers.equals(exportedIdentifiers)) {
                    // muze se zavrit 
                    SolrJUtilities.atomicSet(exportDoc, true, "all_exported_identifiers_flag");
                }
                
                exportReq.add(exportDoc);
                
                UpdateResponse eResponse = exportReq.process(solr, DataCollections.exports.name());
                SolrJUtilities.quietCommit(solr, DataCollections.exports.name());
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());

                //exportedIdentifiers.add(identifier);
                
            }
        }
        return getExport(exportId);
    }


    
    
    @Override
    public JSONObject approveExportUOCP(String exportId) throws SolrServerException, IOException {
        List<Pair<String, List<String>>> discardedEuipoIdentifiers = discardedEuipoIdentifiers(exportId);
        JSONObject export = getExport(exportId);
        List<String> exportedIdentifiers = new ArrayList<>();
        JSONArray jsonArray = export.getJSONObject("response").getJSONArray("docs");
        if (jsonArray.length() > 0) {
            JSONArray exporetedIdents = jsonArray.getJSONObject(0).optJSONArray("exported_identifiers");
            if (exporetedIdents != null) {
                exporetedIdents.forEach(e-> exportedIdentifiers.add(e.toString()));
            }
        }

        try (SolrClient solr = buildClient()) {

            int batchSize = 500;
            int numberOfBatches = discardedEuipoIdentifiers.size() /batchSize;
            if (discardedEuipoIdentifiers.size() % batchSize > 0) {
                numberOfBatches += 1;
            }
            for (int i = 0; i < numberOfBatches; i++) {
                int startExportIndex = i * batchSize;
                int endExportIndex = (i + 1) * batchSize;
                List<Pair<String, List<String>>> exportPids = discardedEuipoIdentifiers.subList(startExportIndex, Math.min(endExportIndex, discardedEuipoIdentifiers.size()));
                UpdateRequest uReq = new UpdateRequest();
                for (Pair<String, List<String>> identPair : exportPids) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identPair.getLeft());

                    SolrJUtilities.atomicRemove(idoc, "euipo", MarcRecordFields.EXPORT);
                    uReq.add(idoc);
                }
            
                UpdateResponse response = uReq.process(solr, DataCollections.catalog.name());
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
            }
            
            
            UpdateRequest exportReq = new UpdateRequest();
            SolrInputDocument exportDoc = new SolrInputDocument();
            exportDoc.setField("id", exportId);
            //euipo identifiers
            List<String> result = discardedEuipoIdentifiers.stream().map(Pair::getLeft).collect(Collectors.toList());
            
            SolrJUtilities.atomicSet(exportDoc, result, "exported_identifiers");

            SolrJUtilities.atomicSet(exportDoc, true, "all_exported_identifiers_flag");
            
            exportReq.add(exportDoc);
            UpdateResponse response = exportReq.process(solr, DataCollections.exports.name());
            SolrJUtilities.quietCommit(solr, DataCollections.exports.name());
        }
        // return result
        return getExport(exportId);

    }

    @Override
    public JSONObject approveExportIOCP(String exportId) throws SolrServerException, IOException {
        List<Pair<String, String>> euipoIdentifiers = euipoIdentifiers(exportId);
        JSONObject export = getExport(exportId);
        List<String> exportedIdentifiers = new ArrayList<>();
        JSONArray jsonArray = export.getJSONObject("response").getJSONArray("docs");
        if (jsonArray.length() > 0) {
            JSONArray exporetedIdents = jsonArray.getJSONObject(0).optJSONArray("exported_identifiers");
            if (exporetedIdents != null) {
                exporetedIdents.forEach(e-> exportedIdentifiers.add(e.toString()));
            }
        }

        try (SolrClient solr = buildClient()) {

            int batchSize = 500;
            int numberOfBatches = euipoIdentifiers.size() /batchSize;
            if (euipoIdentifiers.size() % batchSize > 0) {
                numberOfBatches += 1;
            }
            for (int i = 0; i < numberOfBatches; i++) {
                int startExportIndex = i * batchSize;
                int endExportIndex = (i + 1) * batchSize;
                List<Pair<String, String>> exportPids = euipoIdentifiers.subList(startExportIndex, Math.min(endExportIndex, euipoIdentifiers.size()));
                UpdateRequest uReq = new UpdateRequest();
                for (Pair<String, String> identPair : exportPids) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identPair.getLeft());

                    SolrJUtilities.atomicAddDistinct(idoc, "euipo", MarcRecordFields.EXPORT);
                    uReq.add(idoc);
                }
            
                UpdateResponse response = uReq.process(solr, DataCollections.catalog.name());
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
            }
            
            
            UpdateRequest exportReq = new UpdateRequest();
            SolrInputDocument exportDoc = new SolrInputDocument();
            exportDoc.setField("id", exportId);
            
            //TODO: Zkusit 
            SolrJUtilities.atomicSet(exportDoc, euipoIdentifiers.stream().map(Pair::getLeft).collect(Collectors.toList()), "exported_identifiers");

            SolrJUtilities.atomicSet(exportDoc, true, "all_exported_identifiers_flag");
            
            exportReq.add(exportDoc);
            UpdateResponse response = exportReq.process(solr, DataCollections.exports.name());
            SolrJUtilities.quietCommit(solr, DataCollections.exports.name());
        }
        // return result
        return getExport(exportId);
    }


    private List<Pair<String, List<String>>> discardedEuipoIdentifiers(String exportId) throws SolrServerException, IOException {
        List<Pair<String, List<String>>> euipoIdentifiers = new ArrayList<>();
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("*")
                    .setParam("json.nl", "arrntv")
                    .setFields(MarcRecordFields.IDENTIFIER_FIELD+" "+MarcRecordFields.ID_EUIPO_LASTACTIVE);
            query.addFilterQuery("id_euipo_export_active:"+exportId);
            query.setRows(AbstractEUIPOService.DEFAULT_MAX_EXPORT_ITEMS);
            
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);

            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
            //solr.close();
            JSONObject jsonObject = new JSONObject((String) qresp.get("response"));
            JSONArray docs = jsonObject.getJSONObject("response").getJSONArray("docs");
            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);
                String identifier = doc.getString("identifier");
                if (doc.has(MarcRecordFields.ID_EUIPO_LASTACTIVE)) {
                    List<String> euipo = new ArrayList<>();
                    JSONArray jsonArray = doc.getJSONArray(MarcRecordFields.ID_EUIPO_LASTACTIVE);
                    jsonArray.forEach(obj-> {euipo.add(obj.toString());});
                    if (euipo != null) {
                        euipoIdentifiers.add(Pair.of(identifier, euipo));
                    }
                }
            }
        }
        return euipoIdentifiers;
    }


    private List<Pair<String, String>> euipoIdentifiers(String exportId) throws SolrServerException, IOException {
        List<Pair<String, String>> euipoIdentifiers = new ArrayList<>();
        try (SolrClient solr = buildClient()) {
            SolrQuery query = new SolrQuery("*")
                    .setParam("json.nl", "arrntv")
                    .setFields(MarcRecordFields.IDENTIFIER_FIELD+" "+MarcRecordFields.ID_EUIPO);
            query.addFilterQuery("id_euipo_export_active:"+exportId);
            query.setRows(AbstractEUIPOService.DEFAULT_MAX_EXPORT_ITEMS);
            
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);

            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
            //solr.close();
            JSONObject jsonObject = new JSONObject((String) qresp.get("response"));
            JSONArray docs = jsonObject.getJSONObject("response").getJSONArray("docs");
            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.getJSONObject(i);
                String identifier = doc.getString("identifier");
                if (doc.has("id_euipo")) {
                    String euipoIdentifier = doc.getJSONArray("id_euipo").optString(0);
                    if (euipoIdentifier != null) {
                        euipoIdentifiers.add(Pair.of(identifier, euipoIdentifier));
                    }
                }
            }
        }
        return euipoIdentifiers;
    }



    @Override
    public JSONObject search(String q, ExportType type, int rows, int page)
            throws SolrServerException, IOException {

        List<String> idEuExportNames  = new ArrayList<>();
        if (q != null && !q.trim().startsWith("euipo_")) {

            CatalogSearcher searcher = new CatalogSearcher(String.format("%s, %s", MarcRecordFields.IDENTIFIER_FIELD, MarcRecordFields.ID_EUIPO_EXPORT_ACTIVE));
            Map<String,String> req = new HashMap<>();
            req.put("rows", "90");
            req.put("q", q);
            req.put("fullCatalog","true");
            

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
                            idEuExportNames.add(valObj.getString("name"));
                        });
                    }
                }
            }
            
        }
        

        
        
        NamedList<Object> qresp = null;
        JSONObject ret = new JSONObject();
        try (SolrClient solr = buildClient()) {
            // pokud je null nebo neco naslo hledani v katalogu
            if (q == null || idEuExportNames.size() > 0) {
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
            query.addSort(SortClause.desc("export_date"));
            
            
            if (rows >0 ) query.setRows(rows);  else query.setRows(DEFAULT_SEARCH_RESULT_SIZE);
            if (page >= 0) query.setStart(rows*page);
            
            if (type != null) {
                query.addFilterQuery("export_type:"+type.name());
            }

            if (idEuExportNames.size() > 0) {
                String exportIdQuery = idEuExportNames.stream().collect(Collectors.joining(" OR "));
                query.addFilterQuery("id:("+exportIdQuery+")");
            } else {
                if (q.trim().startsWith("euipo_") ||q.trim().startsWith('"'+"euipo_")) {
                    query.set("defType", "edismax");
                    query.set("qf", "id");
                }
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
