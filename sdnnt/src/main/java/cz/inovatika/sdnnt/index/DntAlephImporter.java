package cz.inovatika.sdnnt.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.exceptions.MaximumIterationExceedException;
import cz.inovatika.sdnnt.index.utils.HarvestUtils;
import cz.inovatika.sdnnt.index.utils.torefactor.MarcRecordUtilsToRefactor;
import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;
import cz.inovatika.sdnnt.indexer.models.utils.MarcRecordUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

/**
 * Importuje set DNT z Alephu pomoci OAI Hleda zaznam z SKC (catalog) a nastavi
 * dntstav Info o stavu z pole 990 a 992
 *
 * @author alberto
 */
public class DntAlephImporter {

    public static final Logger LOGGER = Logger.getLogger(DntAlephImporter.class.getName());
    public static final int DEFAULT_CONNECT_TIMEOUT = 5;
    public static final String CONNECTION_TIMEOUT_KEY = "connectionTimeout";
    public static final String CONNECTION_REQUEST_TIMEOUT_KEY = "connectionRequestTimeout";
    public static final String SOCKET_TIMEOUT_KEY = "socketTimeout";

    protected boolean debug = false;


    JSONObject ret = new JSONObject();
    String collection = "catalog";
    List<MarcRecord> recs = new ArrayList();
    List<String> toDelete = new ArrayList();
    int indexed = 0;
    int deleted = 0;
    int batchSize = 100;

    long reqTime = 0;
    long procTime = 0;
    long solrTime = 0;

    static SolrInputDocument toSolrDoc(MarcRecord rec, boolean debug) {
        SolrInputDocument sDoc = toSolrDoc(rec);
        if (debug) {
            Object identifier = sDoc.containsKey(IDENTIFIER_FIELD) ? sDoc.getFieldValue(IDENTIFIER_FIELD) : "";
            Object dntstav = sDoc.containsKey(DNTSTAV_FIELD) ? sDoc.getFieldValue(DNTSTAV_FIELD) : "";
            Object kuratorstav = sDoc.containsKey(KURATORSTAV_FIELD) ? sDoc.getFieldValue(KURATORSTAV_FIELD) : "";
            Object license = sDoc.containsKey(LICENSE_FIELD) ? sDoc.getFieldValue(LICENSE_FIELD) : "";

            String formatted = String.format("Identifier: %s, dntstav: %s, kuratorstav %s, license %s", identifier, dntstav, kuratorstav, license);
            LOGGER.fine(formatted);
        }
        return sDoc;
    }
    
    static SolrInputDocument toSolrDoc(MarcRecord rec) {
        SolrInputDocument sdoc = new SolrInputDocument();
        if (sdoc.isEmpty()) {
            MarcRecordUtilsToRefactor.fillSolrDoc(sdoc, rec.dataFields, rec.tagsToIndex);
        }
        sdoc.setField(IDENTIFIER_FIELD, rec.identifier);
        MarcRecordUtils.derivedIdentifiers(rec.identifier, sdoc);
        
        sdoc.setField(DATESTAMP_FIELD, rec.datestamp);
        sdoc.setField(SET_SPEC_FIELD, rec.setSpec);
        sdoc.setField(LEADER_FIELD, rec.leader);
        sdoc.setField(RAW_FIELD, rec.toJSON().toString());

        // Control fields
        for (String cf : rec.controlFields.keySet()) {
            sdoc.addField("controlfield_" + cf, rec.controlFields.get(cf));
        }

        sdoc.setField(RECORD_STATUS_FIELD, rec.leader.substring(5, 6));
        sdoc.setField(TYPE_OF_RESOURCE_FIELD, rec.leader.substring(6, 7));
        sdoc.setField(ITEM_TYPE_FIELD, rec.leader.substring(7, 8));

        MarcRecordUtilsToRefactor.setFMT(sdoc, rec.leader.substring(6, 7), rec.leader.substring(7, 8));


        if (sdoc.containsKey(MARC_264_B)) {
            String val = (String) sdoc.getFieldValue(MARC_264_B);
            sdoc.setField(NAKLADATEL_FIELD, MarcRecord.nakladatelFormat(val));
        } else if (sdoc.containsKey(MARC_260_B)) {
            String val = (String) sdoc.getFieldValue(MARC_260_B);
            sdoc.setField(NAKLADATEL_FIELD, MarcRecord.nakladatelFormat(val));
        }

        if (sdoc.containsKey(MARC_910_A)) {
            List<String> collected = sdoc.getFieldValues(MARC_910_A).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
            collected.forEach(it -> sdoc.addField(SIGLA_FIELD, it));
        } else if (sdoc.containsKey(MARC_040_A)) {
            List<String> collected = sdoc.getFieldValues(MARC_040_A).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
            collected.forEach(it -> sdoc.addField(SIGLA_FIELD, it));
        }

        // https://www.loc.gov/marc/bibliographic/bd008a.html
        if (rec.controlFields.containsKey("008") && rec.controlFields.get("008").length() > 37) {
            sdoc.setField("language", rec.controlFields.get("008").substring(35, 38));
            sdoc.setField("place_of_pub", rec.controlFields.get("008").substring(15, 18));
            sdoc.setField("type_of_date", rec.controlFields.get("008").substring(6, 7));
            String date1 = rec.controlFields.get("008").substring(7, 11);
            String date2 = rec.controlFields.get("008").substring(11, 15);
            sdoc.setField("date1", date1);
            sdoc.setField("date2", date2);
            try {
                sdoc.setField("date1_int", Integer.parseInt(date1));
            } catch (NumberFormatException ex) {
            }
            try {
                sdoc.setField("date2_int", Integer.parseInt(date2));
            } catch (NumberFormatException ex) {
            }
        }

        MarcRecordUtilsToRefactor.setIsProposable(sdoc);

        sdoc.setField("title_sort", sdoc.getFieldValue("marc_245a"));

        //245a (název): 245b (podnázev). 245n (číslo dílu/části), 245p (název části/dílu) / 245c (autoři, překlad, ilustrátoři apod.)
        String nazev = "";
        if (sdoc.containsKey("marc_245a")) {
            nazev += sdoc.getFieldValue("marc_245a") + " ";
        }
        if (sdoc.containsKey("marc_245b")) {
            nazev += sdoc.getFieldValue("marc_245b") + " ";
        }
        if (sdoc.containsKey("marc_245p")) {
            nazev += sdoc.getFieldValue("marc_245p") + " ";
        }
        if (sdoc.containsKey("marc_245c")) {
            nazev += sdoc.getFieldValue("marc_245c") + " ";
        }
        if (sdoc.containsKey("marc_245i")) {
            nazev += sdoc.getFieldValue("marc_245i") + " ";
        }
        if (sdoc.containsKey("marc_245n")) {
            nazev += sdoc.getFieldValue("marc_245n") + " ";
        }
        sdoc.setField("nazev", nazev.trim());
        MarcRecordUtilsToRefactor.addRokVydani(sdoc);

        
        
        JSONObject digitalized = Options.getInstance().getJSONObject("digitalized");
        if (digitalized != null) { 

            final List<String> siglas = new ArrayList<>();
            
            Collection<Object> mlinks911u = sdoc.getFieldValues("marc_911u");
            Collection<Object> mlinks856u =  sdoc.getFieldValues("marc_856u");
            Collection<Object> mlinks956u =  sdoc.getFieldValues("marc_956u");
            //Object fmt = sdoc.getFieldValue(FMT_FIELD);
            final List<String> links = new ArrayList<>();
            
            if (mlinks911u != null && !mlinks911u.isEmpty()) {
                mlinks911u.stream().map(Object::toString).forEach(links::add);
            } else if (mlinks956u != null) {
                mlinks956u.stream().map(Object::toString).forEach(links::add);
            } else if (mlinks856u != null) {
                mlinks856u.stream().map(Object::toString).forEach(links::add);
            }
            
            digitalized.keySet().forEach(key -> {
                JSONArray regexps = digitalized.getJSONObject(key).getJSONArray("regexp");
                for (Object oneRegexp : regexps) {
                    // one regexps 
                    if(links.stream().anyMatch(l -> {
                            return l.matches(oneRegexp.toString());
                        })) {
                        siglas.add(key);
                    }
                }
            });
            
            if (!siglas.isEmpty()) {
                sdoc.setField(DIGITAL_LIBRARIES, siglas);
            }
        }
        return sdoc;
    }

    public JSONObject run() {
        getRecords("http://aleph.nkp.cz/OAI?verb=ListRecords&metadataPrefix=marc21&set=DNT-ALL");
        return ret;
    }

    public JSONObject run(String from) {
        JSONObject oaiHavest = Options.getInstance().getJSONObject("OAIHavest");
        if (oaiHavest.has("debug")) {
            debug = oaiHavest.getBoolean("debug");
            LOGGER.info("DNT Aleph importer is in debug mode !");
        }
        String url = "http://aleph.nkp.cz/OAI?verb=ListRecords&metadataPrefix=marc21&set=DNT-ALL";
        if (from != null) {
            url += "&from=" + from;
        }
        getRecords(url);

        return ret;
    }

    public JSONObject resume(String token) {
        String url = "http://aleph.nkp.cz/OAI?verb=ListRecords&resumptionToken=" + token;
        getRecords(url);
        return ret;
    }

    private void getRecords(String url) {
        LOGGER.log(Level.INFO, "ListRecords from {0}...", url);
        String resumptionToken = null;
        File dFile = null;
        InputStream dStream = null;
        try {
            long start = new Date().getTime();
            CloseableHttpClient client = buildOAIClient();
            //HttpGet httpGet = new HttpGet(url);
            dFile = HarvestUtils.throttle(client, "import_dnt", url);
            dStream = new FileInputStream(dFile);

            reqTime += new Date().getTime() - start;
            start = new Date().getTime();
            resumptionToken = readFromXML(dStream);
            procTime += new Date().getTime() - start;
            start = new Date().getTime();
            if (!recs.isEmpty()) {
                addToCatalog(recs, this.debug);
                indexed += recs.size();
                recs.clear();
            }

            solrTime += new Date().getTime() - start;

            if (dStream != null ) {
                IOUtils.closeQuietly(dStream);
            }
            deletePaths(dFile);

            while (resumptionToken != null) {

                start = new Date().getTime();
                url = "http://aleph.nkp.cz/OAI?verb=ListRecords&resumptionToken=" + resumptionToken;

                LOGGER.log(Level.INFO, "Getting {0}...", resumptionToken);
                ret.put("resumptionToken", resumptionToken);

                dFile = HarvestUtils.throttle(client, "import_dnt", url);
                dStream = new FileInputStream(dFile);

                reqTime += new Date().getTime() - start;
                start = new Date().getTime();
                resumptionToken = readFromXML(dStream);

                procTime += new Date().getTime() - start;
                start = new Date().getTime();
                if (recs.size() > batchSize) {
                    LOGGER.fine(String.format("Indexing batch, number of items %d", recs.size()));
                    addToCatalog(recs, this.debug);
                    indexed += recs.size();
                    solrTime += new Date().getTime() - start;
                    LOGGER.log(Level.INFO, "Current indexed: {0}. reqTime: {1}. procTime: {2}. solrTime: {3}", new Object[]{
                            indexed,
                            DurationFormatUtils.formatDurationHMS(reqTime),
                            DurationFormatUtils.formatDurationHMS(procTime),
                            DurationFormatUtils.formatDurationHMS(solrTime)});
                    recs.clear();
                }
                if (dStream != null ) {
                    IOUtils.closeQuietly(dStream);
                }
                deletePaths(dFile);
            }
            start = new Date().getTime();
            if (!recs.isEmpty()) {
                addToCatalog(recs, this.debug);
                indexed += recs.size();
                recs.clear();
            }

            solrTime += new Date().getTime() - start;
            LOGGER.log(Level.INFO, "FINISHED: {0}. reqTime: {1}. procTime: {2}. solrTime: {3}", new Object[]{
                    indexed,
                    DurationFormatUtils.formatDurationHMS(reqTime),
                    DurationFormatUtils.formatDurationHMS(procTime),
                    DurationFormatUtils.formatDurationHMS(solrTime)});
            ret.put("indexed", indexed);
        } catch (SolrServerException | XMLStreamException | MaximumIterationExceedException  | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        } finally {
            LOGGER.fine("Commit to solr");
            SolrJUtilities.quietCommit(Indexer.getClient(), collection);
        }
    }

    private void deletePaths(File dFile) {
        if (!debug) {
            try {
                Files.delete(dFile.toPath());
                Files.delete(dFile.getParentFile().toPath());
            } catch (IOException e) {
                LOGGER.warning("Exception during deleting file");
            }
        }
    }

    private CloseableHttpClient buildOAIClient() {
        JSONObject harvest = Options.getInstance().getJSONObject("OAIHavest");
        int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        if (harvest.has(CONNECTION_TIMEOUT_KEY)) {
            connectTimeout = harvest.getInt(CONNECTION_TIMEOUT_KEY);
        }

        int connectionRequestTimeout = DEFAULT_CONNECT_TIMEOUT;
        if (harvest.has(CONNECTION_REQUEST_TIMEOUT_KEY)) {
            connectionRequestTimeout = harvest.getInt(CONNECTION_REQUEST_TIMEOUT_KEY);
        }

        int socketTimeout = DEFAULT_CONNECT_TIMEOUT;
        if (harvest.has(SOCKET_TIMEOUT_KEY)) {
            socketTimeout = harvest.getInt(SOCKET_TIMEOUT_KEY);
        }


        int ct = connectTimeout * 1000;
        int crt = connectionRequestTimeout * 1000;
        int st = socketTimeout * 1000;
        LOGGER.info(String.format("Creating client with (connectionTimeout=%d, connectionRequestTimeout=%d, socketTimeout=%d", ct, crt, st));
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(ct)
            .setConnectionRequestTimeout(crt)
            .setSocketTimeout(st).build();

        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    void addToCatalog(List<MarcRecord> recs, SolrClient solr, boolean debug) throws JsonProcessingException, SolrServerException, IOException {
        List<SolrInputDocument> idocs = new ArrayList<>();

        for (MarcRecord rec : recs) {
            idocs.add(toSolrDoc(rec, debug));
        }
        if (!idocs.isEmpty()) {
            solr.add("catalog", idocs);
            idocs.clear();
        }
    }

    void addToCatalog(List<MarcRecord> recs, boolean debug) throws JsonProcessingException, SolrServerException, IOException {
        addToCatalog(recs, Indexer.getClient(),  debug);
    }

// Commented by ps ??
//  private void mergeWithCatalog(List<MarcRecord> recs) throws JsonProcessingException, SolrServerException, IOException {
//    List<SolrInputDocument> idocs = new ArrayList<>();
//
//    DateFormat dformat = new SimpleDateFormat("yyyyMMdd");
//    for (MarcRecord rec : recs) {
//      SolrDocumentList docs = find(rec);
//      if (docs == null) {
//
//      } else if (docs.getNumFound() == 0) {
//        LOGGER.log(Level.WARNING, "Record " + rec.identifier + " not found in catalog");
//        ret.append("errors", "Record " + rec.identifier + " not found in catalog.");
//        idocs.add(rec.toSolrDoc());
//      } else if (docs.getNumFound() > 1) {
//        LOGGER.log(Level.WARNING, "For" + rec.identifier + " found more than one record in catalog: " + docs.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining()));
//        // ret.append("errors", "For" + rec.identifier + " found more than one record in catalog: " + docs.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining()));
//      }
//
//      for (SolrDocument doc : docs) {
//        SolrInputDocument idoc = new SolrInputDocument();
//        for (String name : doc.getFieldNames()) {
//          idoc.addField(name, doc.getFieldValue(name));
//        }
//
//        idoc.removeField("dntstav");
//        idoc.removeField("indextime");
//        idoc.removeField("_version_");
//
//        JSONArray hs = new JSONArray();
//        if (rec.dataFields.containsKey("992")) {
//          Date datum_stavu = new Date();
//          datum_stavu.setTime(0);
//          for (DataField df : rec.dataFields.get("992")) {
//            JSONObject h = new JSONObject();
//            String stav = df.getSubFields().get("s").get(0).getValue();
//            if (df.getSubFields().containsKey("s")) {
//              h.put("stav", stav);
//            }
//            if (df.getSubFields().containsKey("a")) {
//              String ds = df.getSubFields().get("a").get(0).getValue();
//              try {
//                Date d = dformat.parse(ds);
//                h.put("date", ds);
//                if (d.after(datum_stavu)) {
//                  idoc.setField("datum_stavu", d);
//                  datum_stavu = d;
//                }
//              } catch (ParseException pex) {
//                LOGGER.warning(pex.getMessage());
//              }
//
//            }
//            if (df.getSubFields().containsKey("b")) {
//              h.put("user", df.getSubFields().get("b").get(0).getValue());
//            }
//            if (df.getSubFields().containsKey("p")) {
//              h.put("comment", df.getSubFields().get("p").get(0).getValue());
//            }
//
//            if ("NZ".equals(stav)) {
//              h.put("license", "dnntt");
//            } else if ("A".equals(stav) && !idoc.containsKey("license")) {
//              h.put("license", "dnnto");
//            }
//            // System.out.println(h);
//            hs.put(h);
//          }
//          idoc.setField("historie_stavu", hs.toString());
//        }
//
//        // String dntstav = rec.dataFields.get("990").get(0).subFields.get("a").get(0).value;
//        if (rec.dataFields.containsKey("990")) {
//          for (DataField df : rec.dataFields.get("990")) {
//            //JSONObject h = new JSONObject();
//            if (df.getSubFields().containsKey("a")) {
//              String stav = df.getSubFields().get("a").get(0).getValue();
//              //h.put("dntstav", dntstav);
//              idoc.addField("dntstav", stav);
//              if ("NZ".equals(stav)) {
//                idoc.setField("license", "dnntt");
//              } else if ("A".equals(stav) && !idoc.containsKey("license")) {
//                idoc.setField("license", "dnnto");
//              }
//            }
//          }
//        }
//
//        idocs.add(idoc);
//
//      }
//
//      if (!idocs.isEmpty()) {
//        Indexer.getClient().add("catalog", idocs);
//        idocs.clear();
//      }
//
//    }
//    if (!idocs.isEmpty()) {
//      Indexer.getClient().add("catalog", idocs);
//      idocs.clear();
//    }
//  }
// Commented by ps
//  private SolrDocumentList find(MarcRecord mr) {
//    try {
//
//      // MarcRecord mr = MarcRecord.fromRAWJSON(source);
//      mr.toSolrDoc();
//      String q = "(controlfield_001:\"" + mr.sdoc.getFieldValue("controlfield_001") + "\""
//              + " AND marc_040a:\"" + mr.sdoc.getFieldValue("marc_040a") + "\""
//              + " AND controlfield_008:\"" + mr.sdoc.getFieldValue("controlfield_008") + "\")"
//              // + " OR marc_020a:\"" + mr.sdoc.getFieldValue("marc_020a") + "\""
//              + " OR marc_015a:\"" + mr.sdoc.getFieldValue("marc_015a") + "\""
//              + " OR dedup_fields:\"" + mr.sdoc.getFieldValue("dedup_fields") + "\"";
//      if (mr.dataFields.containsKey("020")) {
//        for (DataField df : mr.dataFields.get("020")) {
//          if (df.getSubFields().containsKey("a")) {
//            q += " OR marc_020a:\"" + df.getSubFields().get("a").get(0).getValue() + "\"";
//          }
//        }
//      }
//      if (mr.dataFields.containsKey("902")) {
//        for (DataField df : mr.dataFields.get("902")) {
//          if (df.getSubFields().containsKey("a")) {
//            q += " OR marc_020a:\"" + df.getSubFields().get("a").get(0).getValue() + "\"";
//          }
//        }
//      }
//
//      SolrQuery query = new SolrQuery(q)
//              .setRows(20)
//              .setFields("*");
//      SolrDocumentList docs = Indexer.getClient().query("catalog", query).getResults();
//      if (docs.getNumFound() == 0) {
//        LOGGER.log(Level.WARNING, "Query " + q + " not found in catalog");
//      }
//      return docs;
//
//    } catch (SolrServerException | IOException ex) {
//      LOGGER.log(Level.SEVERE, null, ex);
//      return null;
//    }
//    // return ret;
//  }

    String readFromXML(InputStream is) throws XMLStreamException {

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = null;
        try {
            reader = inputFactory.createXMLStreamReader(is);
            return readDocument(reader);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    /**
     * Reads OAI XML document
     *
     * @param reader
     * @return resuptionToken or null
     * @throws XMLStreamException
     * @throws IOException
     */
    String readDocument(XMLStreamReader reader) throws XMLStreamException, IOException {
        String resumptionToken = null;
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("record")) {
                        readMarcRecords(reader);
                    } else if (elementName.equals("resumptionToken")) {
                        resumptionToken = reader.getElementText();
                    } else if (elementName.equals("error")) {
                        ret.put("error", reader.getElementText());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    break;
            }
        }
        return resumptionToken;
        //throw new XMLStreamException("Premature end of file");
    }

    // TODO: JUNIt test
    void readMarcRecords(XMLStreamReader reader) throws XMLStreamException, IOException {
        MarcRecord mr = new MarcRecord();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("header")) {
                        String status = reader.getAttributeValue(null, "status");
                        if (!"deleted".equals(status)) {
                            readRecordHeader(reader, mr);
                        } else {
                            mr.isDeleted = true;
                        }
                    } else if (elementName.equals("metadata")) {
                        readRecordMetadata(reader, mr);
                        if (!mr.isDeleted) {
                            recs.add(mr);
                        } else {
                            LOGGER.log(Level.INFO, "Record {0} is deleted", mr.identifier);
                            toDelete.add(mr.identifier);
                        }
                        // ret.append("records", mr.toJSON());
                    } else {
                        skipElement(reader, elementName);
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return;
            }
        }
        throw new XMLStreamException("Premature end of ListRecords");
    }

    private void readRecordHeader(XMLStreamReader reader, MarcRecord mr) throws XMLStreamException {

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("identifier")) {
                        mr.identifier = reader.getElementText();
                    } else if (elementName.equals("datestamp")) {
                        mr.datestamp = reader.getElementText();
                    } else if (elementName.equals("setSpec")) {
                        mr.setSpec = reader.getElementText();
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("header")) {
                        return;
                    }
            }
        }

        throw new XMLStreamException("Premature end of header");
    }

    private void readRecordMetadata(XMLStreamReader reader, MarcRecord mr) throws XMLStreamException {

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("record")) {
                        readMarcRecord(reader, mr);
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("metadata")) {
                        return;
                    }
            }
        }

        throw new XMLStreamException("Premature end of metadata");
    }

    private MarcRecord readMarcRecord(XMLStreamReader reader, MarcRecord mr) throws XMLStreamException {
      int index = 0;
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("leader")) {
                        mr.leader = reader.getElementText();

                    } else if (elementName.equals("controlfield")) {
                        // <marc:controlfield tag="003">CZ PrDNT</marc:controlfield>
                        String tag = reader.getAttributeValue(null, "tag");
                        String v = reader.getElementText();
                        mr.controlFields.put(tag, v);
                    } else if (elementName.equals("datafield")) {
                        readDatafields(reader, mr, index);
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("record")) {
                        return mr;
                    }
            }
        }
        throw new XMLStreamException("Premature end of marc:record");
    }

    private MarcRecord readDatafields(XMLStreamReader reader, MarcRecord mr, int index) throws XMLStreamException {
        String tag = reader.getAttributeValue(null, "tag");
        if (!mr.dataFields.containsKey(tag)) {
            mr.dataFields.put(tag, new ArrayList());
        }
        List<DataField> dfs = mr.dataFields.get(tag);
        int subFieldIndex = 0;

        DataField df = new DataField(tag, reader.getAttributeValue(null, "ind1"), reader.getAttributeValue(null, "ind2"), index);
        dfs.add(df);
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals("subfield")) {
                        // readSubFields(reader, df);

                        String code = reader.getAttributeValue(null, "code");
                        if (!df.subFields.containsKey(code)) {
                            df.getSubFields().put(code, new ArrayList());
                        }
                        List<SubField> sfs = df.getSubFields().get(code);
                        String val = reader.getElementText();
                        sfs.add(new SubField(code, val, subFieldIndex++));
                        //mr.sdoc.addField("" + tag + code, val);
                    }
                case XMLStreamReader.END_ELEMENT:
                    elementName = reader.getLocalName();
                    if (elementName.equals("datafield")) {
                        return mr;
                    }
            }
        }

        throw new XMLStreamException("Premature end of datafield");
    }

    private void skipElement(XMLStreamReader reader, String name) throws XMLStreamException {

        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.END_ELEMENT:
                    String elementName = reader.getLocalName();
                    if (elementName.equals(name)) {
                        //LOGGER.log(Level.INFO, "eventType: {0}, elementName: {1}", new Object[]{eventType, elementName});
                        return;
                    }
            }
        }
//    throw new XMLStreamException("Premature end of file");
    }
}
