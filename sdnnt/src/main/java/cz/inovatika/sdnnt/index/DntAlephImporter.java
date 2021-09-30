package cz.inovatika.sdnnt.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Importuje set DNT z Alephu pomoci OAI Hleda zaznam z SKC (catalog) a nastavi
 * dntstav Info o stavu z pole 990 a 992
 *
 * @author alberto
 */
public class DntAlephImporter {

  public static final Logger LOGGER = Logger.getLogger(OAIHarvester.class.getName());

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

  public JSONObject run() {
    getRecords("http://aleph.nkp.cz/OAI?verb=ListRecords&metadataPrefix=marc21&set=DNT-ALL");
    return ret;
  }

  public JSONObject run(String from) {
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
    try {
      try {
        long start = new Date().getTime();
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response1 = client.execute(httpGet)) {
          final HttpEntity entity = response1.getEntity();
          if (entity != null) {
            try (InputStream is = entity.getContent()) {
              reqTime += new Date().getTime() - start;
              start = new Date().getTime();
              resumptionToken = readFromXML(is);
              procTime += new Date().getTime() - start;
              start = new Date().getTime();
              if (!recs.isEmpty()) {
                addToCatalog(recs);
                indexed += recs.size();
                recs.clear();
              }

              solrTime += new Date().getTime() - start;
              is.close();
            }
          }
        }

        while (resumptionToken != null) {
          start = new Date().getTime();
          url = "http://aleph.nkp.cz/OAI?verb=ListRecords&resumptionToken=" + resumptionToken;
          LOGGER.log(Level.INFO, "Getting {0}...", resumptionToken);
          ret.put("resumptionToken", resumptionToken);
          httpGet = new HttpGet(url);
          try (CloseableHttpResponse response1 = client.execute(httpGet)) {
            final HttpEntity entity = response1.getEntity();
            if (entity != null) {
              try (InputStream is = entity.getContent()) {
                reqTime += new Date().getTime() - start;
                start = new Date().getTime();
                resumptionToken = readFromXML(is);
                procTime += new Date().getTime() - start;
                start = new Date().getTime();
                if (recs.size() > batchSize) {
                  addToCatalog(recs);
                  indexed += recs.size();
                  solrTime += new Date().getTime() - start;
                  LOGGER.log(Level.INFO, "Current indexed: {0}. reqTime: {1}. procTime: {2}. solrTime: {3}", new Object[]{
                    indexed,
                    DurationFormatUtils.formatDurationHMS(reqTime),
                    DurationFormatUtils.formatDurationHMS(procTime),
                    DurationFormatUtils.formatDurationHMS(solrTime)});
                  recs.clear();
                }
                is.close();
              }
            }
          }

        }
        start = new Date().getTime();
        if (!recs.isEmpty()) {
          addToCatalog(recs);
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
      } catch (XMLStreamException | IOException exc) {
        LOGGER.log(Level.SEVERE, null, exc);
        ret.put("error", exc);
      }
      Indexer.getClient().commit(collection);
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
  }

  private void addToCatalog(List<MarcRecord> recs) throws JsonProcessingException, SolrServerException, IOException {
    List<SolrInputDocument> idocs = new ArrayList<>();

    for (MarcRecord rec : recs) {
      idocs.add(rec.toSolrDoc());
    }
    if (!idocs.isEmpty()) {
      Indexer.getClient().add("catalog", idocs);
      idocs.clear();
    }
  }

  private void mergeWithCatalog(List<MarcRecord> recs) throws JsonProcessingException, SolrServerException, IOException {
    List<SolrInputDocument> idocs = new ArrayList<>();

    DateFormat dformat = new SimpleDateFormat("yyyyMMdd");
    for (MarcRecord rec : recs) {
      SolrDocumentList docs = find(rec);
      if (docs == null) {

      } else if (docs.getNumFound() == 0) {
        LOGGER.log(Level.WARNING, "Record " + rec.identifier + " not found in catalog");
        ret.append("errors", "Record " + rec.identifier + " not found in catalog.");
        idocs.add(rec.toSolrDoc());
      } else if (docs.getNumFound() > 1) {
        LOGGER.log(Level.WARNING, "For" + rec.identifier + " found more than one record in catalog: " + docs.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining()));
        // ret.append("errors", "For" + rec.identifier + " found more than one record in catalog: " + docs.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining()));
      }

      for (SolrDocument doc : docs) {
        SolrInputDocument idoc = new SolrInputDocument();
        for (String name : doc.getFieldNames()) {
          idoc.addField(name, doc.getFieldValue(name));
        }

        idoc.removeField("dntstav");
        idoc.removeField("indextime");
        idoc.removeField("_version_");

        JSONArray hs = new JSONArray();
        if (rec.dataFields.containsKey("992")) {
          Date datum_stavu = new Date();
          datum_stavu.setTime(0);
          for (DataField df : rec.dataFields.get("992")) {
            JSONObject h = new JSONObject();
            String stav = df.getSubFields().get("s").get(0).getValue();
            if (df.getSubFields().containsKey("s")) {
              h.put("stav", stav);
            }
            if (df.getSubFields().containsKey("a")) {
              String ds = df.getSubFields().get("a").get(0).getValue();
              try {
                Date d = dformat.parse(ds);
                h.put("date", ds);
                if (d.after(datum_stavu)) {
                  idoc.setField("datum_stavu", d);
                  datum_stavu = d;
                }
              } catch (ParseException pex) {
                LOGGER.warning(pex.getMessage());
              }

            }
            if (df.getSubFields().containsKey("b")) {
              h.put("user", df.getSubFields().get("b").get(0).getValue());
            }
            if (df.getSubFields().containsKey("p")) {
              h.put("comment", df.getSubFields().get("p").get(0).getValue());
            }

            if ("NZ".equals(stav)) {
              h.put("license", "dnntt");
            } else if ("A".equals(stav) && !idoc.containsKey("license")) {
              h.put("license", "dnnto");
            }
            // System.out.println(h);
            hs.put(h);
          }
          idoc.setField("historie_stavu", hs.toString());
        }

        // String dntstav = rec.dataFields.get("990").get(0).subFields.get("a").get(0).value;
        if (rec.dataFields.containsKey("990")) {
          for (DataField df : rec.dataFields.get("990")) {
            //JSONObject h = new JSONObject();
            if (df.getSubFields().containsKey("a")) {
              String stav = df.getSubFields().get("a").get(0).getValue();
              //h.put("dntstav", dntstav);
              idoc.addField("dntstav", stav);
              if ("NZ".equals(stav)) {
                idoc.setField("license", "dnntt");
              } else if ("A".equals(stav) && !idoc.containsKey("license")) {
                idoc.setField("license", "dnnto");
              }
            }
          }
        }

        idocs.add(idoc);

      }

      if (!idocs.isEmpty()) {
        Indexer.getClient().add("catalog", idocs);
        idocs.clear();
      }

    }
    if (!idocs.isEmpty()) {
      Indexer.getClient().add("catalog", idocs);
      idocs.clear();
    }
  }

  private SolrDocumentList find(MarcRecord mr) {
    // JSONObject ret = new JSONObject();
    try {

      // MarcRecord mr = MarcRecord.fromRAWJSON(source);
      mr.toSolrDoc();
      String q = "(controlfield_001:\"" + mr.sdoc.getFieldValue("controlfield_001") + "\""
              + " AND marc_040a:\"" + mr.sdoc.getFieldValue("marc_040a") + "\""
              + " AND controlfield_008:\"" + mr.sdoc.getFieldValue("controlfield_008") + "\")"
              // + " OR marc_020a:\"" + mr.sdoc.getFieldValue("marc_020a") + "\""
              + " OR marc_015a:\"" + mr.sdoc.getFieldValue("marc_015a") + "\""
              + " OR dedup_fields:\"" + mr.sdoc.getFieldValue("dedup_fields") + "\"";
      if (mr.dataFields.containsKey("020")) {
        for (DataField df : mr.dataFields.get("020")) {
          if (df.getSubFields().containsKey("a")) {
            q += " OR marc_020a:\"" + df.getSubFields().get("a").get(0).getValue() + "\"";
          }
        }
      }
      if (mr.dataFields.containsKey("902")) {
        for (DataField df : mr.dataFields.get("902")) {
          if (df.getSubFields().containsKey("a")) {
            q += " OR marc_020a:\"" + df.getSubFields().get("a").get(0).getValue() + "\"";
          }
        }
      }

      SolrQuery query = new SolrQuery(q)
              .setRows(20)
              .setFields("*");
      SolrDocumentList docs = Indexer.getClient().query("catalog", query).getResults();
      if (docs.getNumFound() == 0) {
        LOGGER.log(Level.WARNING, "Query " + q + " not found in catalog");
      }
      return docs;

    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
    // return ret;
  }

  private String readFromXML(InputStream is) throws XMLStreamException {

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
  private String readDocument(XMLStreamReader reader) throws XMLStreamException, IOException {
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

  private void readMarcRecords(XMLStreamReader reader) throws XMLStreamException, IOException {
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
            readDatafields(reader, mr);
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

  private MarcRecord readDatafields(XMLStreamReader reader, MarcRecord mr) throws XMLStreamException {
    String tag = reader.getAttributeValue(null, "tag");
    if (!mr.dataFields.containsKey(tag)) {
      mr.dataFields.put(tag, new ArrayList());
    }
    List<DataField> dfs = mr.dataFields.get(tag);

    DataField df = new DataField(tag, reader.getAttributeValue(null, "ind1"), reader.getAttributeValue(null, "ind2"));
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
            sfs.add(new SubField(code, val));
            mr.sdoc.addField("marc_" + tag + code, val);
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
