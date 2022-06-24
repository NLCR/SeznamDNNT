
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import static cz.inovatika.sdnnt.index.Indexer.getClient;
import cz.inovatika.sdnnt.indexer.models.Import;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author alberto
 */
public class XMLImporterHeureka {

  public static final Logger LOGGER = Logger.getLogger(XMLImporterHeureka.class.getName());

  final String IMPORTS = "imports";
  final String IMPORTS_DOCUMENTS = "imports_documents";

  String import_id;
  String import_date;
  String import_url;
  int in_sdnnt;
  final String import_origin = "heureka";
  String first_id;
  String last_id;

  int from_id = -1;

  int total;
  int skipped;
  int indexed;

  Map<String, String> fieldsMap = Map.ofEntries(
          new AbstractMap.SimpleEntry<>("NAME", "PRODUCTNAME"),
          new AbstractMap.SimpleEntry<>("ISBN", "ISBN"));
  List<String> elements = Arrays.asList("NAME", "EAN");

  public JSONObject doImport(String path, String from_id, boolean resume) {
    JSONObject ret = new JSONObject();
    long start = new Date().getTime();
    // solr = new ConcurrentUpdateSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    import_date = now.format(DateTimeFormatter.ISO_INSTANT);
    import_url = path;
    import_id = now.toEpochSecond() + "";
    if (resume) {
      this.from_id = getLastId();
    } else if (from_id != null) {
      this.from_id = Integer.parseInt(from_id);
    }
    if (path.startsWith("http")) {
      ret = fromUrl(path, from_id);
    } else {
      ret = fromFile(path, from_id);
    }
    ret.put("indexed", indexed);
    ret.put("total", total);
    ret.put("skipped", skipped);
    ret.put("file", path);
    ret.put("origin", import_origin);
    String ellapsed = DurationFormatUtils.formatDurationHMS(new Date().getTime() - start);
    ret.put("ellapsed", ellapsed);
    LOGGER.log(Level.INFO, "FINISHED. Total: {0}, Indexed: {1}, Skipped: {2}", new Object[]{total, indexed, skipped});
    return ret;
  }

  private int getLastId() {
    int last = -1;
    Options opts = Options.getInstance();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("origin:" + import_origin)
              .setFields("last_id")
              .setSort("indextime", SolrQuery.ORDER.desc);
      SolrDocumentList docs = solr.query("imports", q).getResults();
      if (docs.getNumFound() > 0) {
        last = Integer.parseInt((String) docs.get(0).getFirstValue("last_id")) + 1;
      }
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return last;
  }

  private void indexImportSummary() throws SolrServerException, IOException {
    SolrInputDocument idoc = new SolrInputDocument();
    idoc.setField("id", import_id);
    idoc.setField("date", import_date);
    idoc.setField("url", import_url);
    idoc.setField("origin", import_origin);
    idoc.setField("first_id", first_id);
    idoc.setField("last_id", last_id);
    idoc.setField("processed", false);
    idoc.setField("num_items", total);
    idoc.setField("num_docs", indexed);
    idoc.setField("skipped", skipped);
    idoc.setField("num_in_sdnnt", in_sdnnt);
    getClient().add(IMPORTS, idoc);
    getClient().commit(IMPORTS);
  }

  // https://www.palmknihy.cz/heureka.xml
  public JSONObject fromFile(String path, String from_id) {
    LOGGER.log(Level.INFO, "Processing {0}", path);
    JSONObject ret = new JSONObject();
    try {
      File f = new File(path);
      try (InputStream is = new FileInputStream(f)) {
        readXML(is, "SHOPITEM");
      } catch (XMLStreamException | IOException exc) {
        LOGGER.log(Level.SEVERE, null, exc);
        ret.put("error", exc);
      }
      getClient().commit(IMPORTS_DOCUMENTS);
      indexImportSummary();

    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  public JSONObject fromUrl(String uri, String from_id) {
    LOGGER.log(Level.INFO, "Processing {0}", uri);
    JSONObject ret = new JSONObject();
    try {

      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(uri);
      try (CloseableHttpResponse response1 = client.execute(httpGet)) {
        final HttpEntity entity = response1.getEntity();
        if (entity != null) {
          try (InputStream is = entity.getContent()) {
            readXML(is, "SHOPITEM");
          }
        }
      } catch (XMLStreamException | IOException exc) {
        LOGGER.log(Level.SEVERE, null, exc);
        ret.put("error", exc);
      }
      getClient().commit(IMPORTS_DOCUMENTS);
      indexImportSummary();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  private void readXML(InputStream is, String itemName) throws XMLStreamException {

    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(is);
      NodeList nodes = doc.getElementsByTagName(itemName);
      for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i);
        Map<String, String> item = new HashMap<>();
        NodeList fields = node.getChildNodes();
        for (int j = 0; j < fields.getLength(); j++) {
          Node field = fields.item(j);
          if (field.getNodeType() == Node.ELEMENT_NODE) {
            item.put(field.getNodeName(), field.getTextContent());

          }
        }
        toIndex(item);
      }
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private void toIndex(Map<String, String> item) {
    try {
      total++;
      if (first_id == null) {
        first_id = item.get("ITEM_ID");
      }
      last_id = item.get("ITEM_ID");
      if (from_id > Integer.parseInt(item.get("ITEM_ID"))) {
        return;
      }
      
      if (item.containsKey(fieldsMap.get("NAME")) && item.get(fieldsMap.get("NAME")).toLowerCase().contains("[audiokniha]")) {
        LOGGER.log(Level.INFO, "{0} ma format audioknihy, vynechame", last_id);
        skipped++;
        return;
      }

      SolrInputDocument idoc = new SolrInputDocument();
      String item_id = item.get("ITEM_ID") + "_" + import_origin;
      idoc.setField("import_id", import_id);
      idoc.setField("import_date", import_date);
      idoc.setField("id", import_id + "_" + item_id);
      idoc.setField("item_id", item_id); 

      if (item.containsKey("ISBN")) {
        ISBNValidator isbn = ISBNValidator.getInstance();
        String ean = item.get("ISBN");
        ean = isbn.validate(ean);
        if (ean != null) {
          item.put("EAN", ean);
        } else {
          item.put("EAN", item.get("ISBN"));
        }
      }

      idoc.setField("item", new JSONObject(item).toString());

      addDedup(item);
      addFrbr(item);
      findInCatalog(item);
      
      SolrDocument isControlled = Import.isControlled(item_id);

      if (isControlled != null) {
        idoc.setField("controlled", true);
        idoc.setField("controlled_note", isControlled.get("controlled_note"));
        idoc.setField("controlled_date", isControlled.get("controlled_date"));
        idoc.setField("controlled_user", isControlled.get("controlled_user"));
      }
      
      if (item.containsKey("found")) {
        idoc.setField("ean", item.get("EAN"));
        idoc.setField("name", item.get(fieldsMap.get("NAME")));
        if (item.containsKey(fieldsMap.get("AUTHOR"))) {
          idoc.setField("author", item.get(fieldsMap.get("AUTHOR")));
        }

        idoc.setField("identifiers", item.get("identifiers"));
        idoc.setField("na_vyrazeni", item.get("na_vyrazeni"));
        idoc.setField("hits_na_vyrazeni", item.get("hits_na_vyrazeni"));
        idoc.setField("catalog", item.get("catalog"));
        idoc.setField("num_hits", item.get("num_hits"));
        idoc.setField("hit_type", item.get("hit_type"));
        idoc.setField("dntstav", item.get("dntstav"));

        getClient().add(IMPORTS_DOCUMENTS, idoc);
        indexed++;
      }

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private void addDedup(Map item) {
    item.put("dedup_fields", "");
  }

  private void addFrbr(Map item) {
    String frbr = "";
    if (item.containsKey(fieldsMap.get("AUTHOR"))) {
      frbr += item.get(fieldsMap.get("AUTHOR"));
    }

    frbr += "/" + item.get(fieldsMap.get("NAME"));

    item.put("frbr", MD5.normalize(frbr));
  }

  public void findInCatalog(Map item) {
    // JSONObject ret = new JSONObject();
    try {
      String name = ((String) item.get(fieldsMap.get("NAME"))).replaceAll("\\s*\\[[^\\]]*\\]\\s*", "");

      String[] parts = name.split(" - ");
      String nazev = parts[0].trim();
      String title = "";
      if (nazev.isEmpty()) {
        if (parts.length > 1) {
          title = " nazev:(" + ClientUtils.escapeQueryChars(parts[1].trim()) + ")";
        }
      } else {
        title = "nazev:(" + ClientUtils.escapeQueryChars(nazev) + ")";
        if (parts.length > 1) {
          title += " AND author:(" + ClientUtils.escapeQueryChars(parts[1].trim()) + ")";
        }
      }

      String q = "ean:\"" + item.get("EAN") + "\"^10.0 OR (" + title + ")";

      SolrQuery query = new SolrQuery(q)
              .setRows(100)
              .setParam("q.op", "AND")
              // .addFilterQuery("dntstav:A OR dntstav:PA OR dntstav:NL")
              .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a");
//      SolrDocumentList docs = getClient().query("catalog", query).getResults();
//      for (SolrDocument doc : docs) {
//      }
      QueryRequest qreq = new QueryRequest(query);
      NoOpResponseParser rParser = new NoOpResponseParser();
      rParser.setWriterType("json");
      qreq.setResponseParser(rParser);
      NamedList<Object> qresp = getClient().request(qreq, "catalog");
      JSONObject jresp = (new JSONObject((String) qresp.get("response"))).getJSONObject("response");
      JSONArray docs = jresp.getJSONArray("docs");
      List<String> identifiers = new ArrayList<>();
      List<String> na_vyrazeni = new ArrayList<>();
      boolean isEAN = false;
      if (docs.length() == 0) {
        return;
      }
      for (Object o : docs) {
        JSONObject doc = (JSONObject) o;

        if (doc.has("dntstav")) {
          item.put("dntstav", doc.getJSONArray("dntstav").toList());
          List<Object> stavy = doc.getJSONArray("dntstav").toList();
          if (stavy.contains("A") || stavy.contains("PA") || stavy.contains("NL")) {
            na_vyrazeni.add(doc.getString("identifier"));
          }
          in_sdnnt++;
        }
        identifiers.add(doc.toString());

        if (doc.has("ean")) {
          List<Object> eans = doc.getJSONArray("ean").toList();
          if (eans.contains(item.get("EAN"))) {
            isEAN = true;

          }
        }
      }
      item.put("found", true);
      item.put("hit_type", isEAN ? "ean" : "noean");
      item.put("num_hits", isEAN ? identifiers.size() : jresp.getInt("numFound"));
      item.put("identifiers", identifiers);
      item.put("na_vyrazeni", na_vyrazeni);
      item.put("hits_na_vyrazeni", na_vyrazeni.size());

    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    // return ret;
  }

}
