package cz.inovatika.sdnnt.indexer;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
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
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author alberto
 */
public class OAIHarvester {

  public static final Logger LOGGER = Logger.getLogger(OAIHarvester.class.getName());
  JSONObject ret = new JSONObject();
  String collection = "catalog";
  List<SolrInputDocument> recs = new ArrayList();
  List<String> toDelete = new ArrayList();
  int indexed = 0;
  int deleted = 0;
  int batchSize = 1000;

  public JSONObject full(String set, String core) {
    collection = core;
    long start = new Date().getTime();
    Options opts = Options.getInstance();
    String url = String.format("%s?verb=ListRecords&metadataPrefix=marc21&set=%s",
            opts.getJSONObject("OAI").getString("url"),
            set);
    getRecords(url);
    ret.put("indexed", indexed);
    String ellapsed = DurationFormatUtils.formatDurationHMS(new Date().getTime() - start);
    ret.put("ellapsed", ellapsed);
    LOGGER.log(Level.INFO, "full FINISHED. Indexed {0} in {1}", new Object[]{ellapsed, indexed});
    return ret;
  }

  private String lastIndexDate(String set) {
    String last = null;
    Options opts = Options.getInstance();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("setSpec:" + set)
              .setFields("datestamp")
              .setSort("datestamp", SolrQuery.ORDER.desc);
      TimeZone tz = TimeZone.getTimeZone("UTC");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
      df.setTimeZone(tz);
      last = df.format((Date) solr.query(collection, q).getResults().get(0).getFirstValue("datestamp"));
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return last;
  }

  public JSONObject update(String set, String core) {
    collection = core;
    Options opts = Options.getInstance();
    long start = new Date().getTime();
    String from = lastIndexDate(set);// "2021-03-14T00:00:00Z";
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
    df.setTimeZone(tz);
    String until = df.format(new Date());
    String url = String.format("%s?verb=ListRecords&metadataPrefix=marc21&from=%s&until=%s&set=%s",
            opts.getJSONObject("OAI").getString("url"),
            from,
            until,
            set);
    getRecords(url);
    ret.put("indexed", indexed);
    String ellapsed = DurationFormatUtils.formatDurationHMS(new Date().getTime() - start);
    ret.put("ellapsed", ellapsed);
    LOGGER.log(Level.INFO, "update FINISHED. Indexed {0} in {1}", new Object[]{ellapsed, indexed});
    return ret;
  }

  public JSONObject updateFrom(String set, String core, String from) {
    collection = core;
    Options opts = Options.getInstance();
    long start = new Date().getTime();
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
    df.setTimeZone(tz);
    String until = df.format(new Date());
    String url = String.format("%s?verb=ListRecords&metadataPrefix=marc21&from=%s&until=%s&set=%s",
            opts.getJSONObject("OAI").getString("url"),
            from,
            until,
            set);
    getRecords(url);
    ret.put("indexed", indexed);
    ret.put("deleted", deleted);
    String ellapsed = DurationFormatUtils.formatDurationHMS(new Date().getTime() - start);
    ret.put("ellapsed", ellapsed);
    LOGGER.log(Level.INFO, "update FINISHED. Indexed {0} in {1}", new Object[]{ellapsed, indexed});
    return ret;
  }

  private void getRecords(String url) {
    LOGGER.log(Level.INFO, "ListRecords from {0}...", url);
    Options opts = Options.getInstance();
    String resumptionToken = null;
    try (SolrClient solr = new ConcurrentUpdateSolrClient.Builder(opts.getString("solr.host")).build()) {
      try {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response1 = client.execute(httpGet)) {
          final HttpEntity entity = response1.getEntity();
          if (entity != null) {
            try (InputStream is = entity.getContent()) {
              resumptionToken = readFromXML(is);
              if (!recs.isEmpty()) {
                solr.add(collection, recs);
                indexed += recs.size();
                recs.clear();
              }
              if (!toDelete.isEmpty()) {
                solr.deleteById(collection, toDelete);
                deleted += toDelete.size();
                toDelete.clear();
              }
              is.close();
            }
          }
        }

        while (resumptionToken != null) {
          url = "http://aleph.nkp.cz/OAI?verb=ListRecords&resumptionToken=" + resumptionToken;
          httpGet = new HttpGet(url);
          try (CloseableHttpResponse response1 = client.execute(httpGet)) {
            final HttpEntity entity = response1.getEntity();
            if (entity != null) {
              try (InputStream is = entity.getContent()) {
                resumptionToken = readFromXML(is);
                if (recs.size() > batchSize) {
                  solr.add(collection, recs);
                  indexed += recs.size();
                  LOGGER.log(Level.INFO, "Current indexed: {0}", indexed);
                  recs.clear();
                }
                is.close();
              }
            }
          }

        }

        if (!recs.isEmpty()) {
          solr.add(collection, recs);
          indexed += recs.size();
          recs.clear();
        }

        if (!toDelete.isEmpty()) {
          solr.deleteById(collection, toDelete);
          deleted += toDelete.size();
          toDelete.clear();
        }
      } catch (XMLStreamException | IOException exc) {
        LOGGER.log(Level.SEVERE, null, exc);
        ret.put("error", exc);
      }
      solr.commit(collection);
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
  }

  public String readFromXML(InputStream is) throws XMLStreamException {

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
              recs.add(mr.toSolrDoc());
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
            if (MarcRecord.tagsToIndex.contains(tag)) {
              mr.sdoc.addField("marc_" + tag + code, val);
            }
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
