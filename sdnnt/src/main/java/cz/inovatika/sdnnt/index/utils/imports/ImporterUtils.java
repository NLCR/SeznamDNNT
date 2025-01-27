package cz.inovatika.sdnnt.index.utils.imports;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.CURATOR_ACTIONS;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DATUM_KURATOR_STAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DATUM_STAVU_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DIGITAL_LIBRARIES;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.EXPORT;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.FLAG_PUBLIC_IN_DL;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.FMT_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.FOLLOWERS;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.GRANULARITY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.HISTORIE_STAVU_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_EUIPO;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_EUIPO_CANCELED;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_EUIPO_EXPORT;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_EUIPO_EXPORT_ACTIVE;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.ID_EUIPO_LASTACTIVE;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.LICENSE_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.LICENSE_HISTORY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MASTERLINKS_DISABLED_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MASTERLINKS_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.RAW_FIELD;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
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
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.index.AbstractXMLImport.XMLImportDesc;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

/***
 * 
 * @author happy
 *
 */
public class ImporterUtils {
    
    public static final int BATCHS_SIZE = 80;
    
    public static final String IMPORT_ELLAPSED_KEY = "ellapsed";
    public static final String IMPORT_ORIGIN_KEY = "origin";
    public static final String IMPORT_FILE_KEY = "file";
    public static final String IMPORT_SKIPPED_KEY = "skipped";
    public static final String IMPORT_TOTAL_KEY = "total";
    public static final String IMPORT_INDEXED_KEY = "indexed";
    public static final String IMPORT_NUMINSDNNT_KEY = "num_in_sdnnt";
    public static final String IMPORT_NUMDOCS_KEY = "num_docs";
    public static final String IMPORT_NUMITEMS_KEY = "num_items";
    public static final String IMPORT_PROCESSED_KEY = "processed";
    public static final String IMPORT_LASTID_KEY = "last_id";
    public static final String IMPORT_FIRSTID_KEY = "first_id";
    public static final String IMPORT_URL_KEY = "url";
    public static final String IMPORT_DATE_KEY = "date";
    public static final String IMPORT_ID_KEY = "id";
    public static final String IMPORT_GROUP_KEY = "group";
    
    private ImporterUtils() {}


    public static Path download(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS) 
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            Path tmpFile = Files.createTempFile("download", ".xml");
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmpFile));
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Download ended with status code: " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    
    
    
    /** Change all given documents to PN curator state */
    public static void changePNState(Logger logger, SolrClient solrJClient, List<String> allIdentifiersToPN, String importIdentification)
            throws JsonProcessingException, SolrServerException, IOException {
        int numberOfIterations = allIdentifiersToPN.size() / BATCHS_SIZE
                + (allIdentifiersToPN.size() % BATCHS_SIZE == 0 ? 0 : 1);
        for (int i = 0; i < numberOfIterations; i++) {
            logger.info(String.format("Batch number %d",i));
            int start = i * BATCHS_SIZE;
            int stop = Math.min((i + 1) * BATCHS_SIZE, allIdentifiersToPN.size());
            List<String> sublist = allIdentifiersToPN.subList(start, stop);
            for (String identifier : sublist) {
                MarcRecord mr = MarcRecord.fromIndex(solrJClient, identifier);

                SolrInputDocument idoc = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.PN.name(), mr, importIdentification, "import/"+importIdentification);
                if (idoc != null) {
                    solrJClient.add(DataCollections.catalog.name(), idoc);
                }
            }

            SolrJUtilities.quietCommit(solrJClient, DataCollections.catalog.name());
        }
    }

    /** Change all import docs 
     * @throws IOException 
     * @throws SolrServerException */
    public static void changeImportDocs(Logger logger, SolrClient solrClient, XMLImportDesc importDesc) throws SolrServerException, IOException {
        String fq = "import_id:\""+importDesc.getImportId()+"\"";

        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        SolrQuery q = new SolrQuery("*").setRows(1000)
                .setSort("id", SolrQuery.ORDER.desc)
                .addFilterQuery(fq)
                .setFields("id,item_id,identifiers");
        
        boolean done = false;
        while (!done) {
            q.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse qr = solrClient.query(DataCollections.imports_documents.name(), q);
            String nextCursorMark = qr.getNextCursorMark();
            SolrDocumentList docs = qr.getResults();

            for (SolrDocument doc : docs) {
                
                List<String> identifiers = doc.getFieldValues("identifiers").stream().map(Object::toString).map(JSONObject::new).map(json -> {
                    return json.optString("identifier");
                }).collect(Collectors.toList());
                
                String qident = identifiers.stream().map(it-> {
                    return "\""+it+"\"";
                }).collect(Collectors.joining(" OR "));

                SolrQuery query = new SolrQuery("identifier:("+qident+")")
                        .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,datum_kurator_stav,granularity:[json],marc_998a,id_euipo,c_actions");

                
                List<String> updateVals = new ArrayList<>();
                
                QueryRequest qreq = new QueryRequest(query);
                NoOpResponseParser rParser = new NoOpResponseParser();
                rParser.setWriterType("json");
                qreq.setResponseParser(rParser);
                NamedList<Object> qresp = solrClient.request(qreq, "catalog");
                JSONObject ret = (new JSONObject((String) qresp.get("response"))).getJSONObject("response");

                JSONArray catalogdocs = ret.getJSONArray("docs");
                for (int i = 0; i < catalogdocs.length(); i++) {
                    updateVals.add(catalogdocs.getJSONObject(i).toString());
                }
                
                SolrInputDocument sdoc = new SolrInputDocument();
                sdoc.setField("id", doc.getFieldValue("id"));
                SolrJUtilities.atomicSet(sdoc, updateVals, "identifiers");

                UpdateRequest uReq = new UpdateRequest();
                uReq.add(sdoc);

                if (!uReq.getDocuments().isEmpty()) {
                    UpdateResponse response = uReq.process(solrClient, DataCollections.imports_documents.name());
                    int status = response.getStatus();
                    if (status !=  0) {
                        logger.log(Level.SEVERE,"Bad status "+status);
                    }
                }
            }   
            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            }
            cursorMark = nextCursorMark;
            SolrJUtilities.quietCommit(solrClient, DataCollections.imports_documents.name());
        }

    }


    public static long calculateInterval(Instant start, Instant end, ChronoUnit unit) {

        LocalDateTime startDate = start.atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime endDate = end.atZone(ZoneId.systemDefault()).toLocalDateTime();
        
        return unit.between(startDate, endDate);
    }
    
    
    
}
