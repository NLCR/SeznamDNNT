/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.oai;

import cz.inovatika.sdnnt.Options;
import static cz.inovatika.sdnnt.index.Indexer.getClient;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import static cz.inovatika.sdnnt.oai.OAIServlet.LOGGER;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;

import cz.inovatika.sdnnt.model.DataCollections;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class OAIRequest {

  // Muze byt indextime nebo datestamp
  static String SORT_FIELD = "indextime";
  static String CURSOR_FIELD = "identifier";

  public static String headerOAI() {
    return "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n";
  }

  public static String responseDateTag() {
    return "<responseDate>" + ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT) + "</responseDate>";
  }

  public static String requestTag(HttpServletRequest req) {
    StringBuilder ret = new StringBuilder();
    ret.append("<request ");
    for (String p : req.getParameterMap().keySet()) {
      ret.append(p).append("=\"").append(req.getParameter(p)).append("\" ");
    }
    ret.append(">").append(req.getRequestURL()).append("</request>");
    return ret.toString();
  }

  public static String identify(HttpServletRequest req) {
    JSONObject conf = Options.getInstance().getJSONObject("OAI");
    String xml = headerOAI() + responseDateTag() + requestTag(req)
            + "<Identify>"
            + "<repositoryName>" + conf.getString("repositoryName") + "</repositoryName>"
            + "<baseURL>" + req.getRequestURL() + "</baseURL>"
            + "<protocolVersion>2.0</protocolVersion>"
            + "<adminEmail>" + conf.getString("adminEmail") + "</adminEmail>"
            + "<earliestDatestamp>2012-06-30T22:26:40Z</earliestDatestamp>"
            + "<deletedRecord>transient</deletedRecord>"
            + "<granularity>YYYY-MM-DDThh:mm:ssZ</granularity>"
            + "<description>"
            + "<oai-identifier xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">"
            + "<scheme>oai</scheme>"
            + "<repositoryIdentifier>aleph-nkp.cz</repositoryIdentifier>"
            + "<delimiter>:</delimiter>"
            + "<sampleIdentifier>oai:aleph-nkp.cz:NKC01-000000001</sampleIdentifier>"
            + "</oai-identifier>"
            + "</description>"
            + "</Identify>"
            + "</OAI-PMH>";
    return xml;
  }

  public static String listSets(HttpServletRequest req) {
    String xml = headerOAI() + responseDateTag() + requestTag(req)
            + "<ListSets>";
    JSONObject sets = Options.getInstance().getJSONObject("OAI").getJSONObject("sets");
    for (Object spec: sets.keySet()) {
      JSONObject set = sets.getJSONObject((String) spec);
      xml += "<set><setSpec>"+spec+"</setSpec><setName>"+set.getString("name")+"</setName></set>\n";
    }
    xml += "</ListSets>\n"
           + "</OAI-PMH>";
    return xml;
  }

  public static String metadataFormats(HttpServletRequest req) {
    String xml = headerOAI() + responseDateTag() + requestTag(req)
            + "<ListMetadataFormats>"
            + "<metadataFormat><metadataPrefix>marc21</metadataPrefix><schema>http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd</schema>"
            + "<metadataNamespace>http://www.loc.gov/MARC21/slim</metadataNamespace>"
            + "</metadataFormat>"
            + "</ListMetadataFormats>"
            + "</OAI-PMH>";
    return xml;
  }

  public static String listRecords(HttpServletRequest req, boolean onlyIdentifiers) {
    String verb = onlyIdentifiers ? "ListIdentifiers" : "ListRecords";
    Options opts = Options.getInstance();
    StringBuilder ret = new StringBuilder();
    int rows = opts.getJSONObject("OAI").getInt("rowsPerPage");
    ret.append(headerOAI())
            .append(responseDateTag())
            .append(requestTag(req));
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
      
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      SolrQuery query = new SolrQuery("*")
              .setRows(rows)
              .addSort(SORT_FIELD, SolrQuery.ORDER.asc)
              .addSort(CURSOR_FIELD, SolrQuery.ORDER.asc)
              .setFields(SORT_FIELD, "identifier,raw,dntstav,datum_stavu,license,license_history,historie_stavu,granularity");
      if (req.getParameter("from") != null) {
        String from = req.getParameter("from");
        String until = "*";
        if (req.getParameter("until") != null) {
          until = req.getParameter("until");
        }
        query.addFilterQuery(SORT_FIELD + ":[" + from + " TO " + until + "]");
      }

      String set = req.getParameter("set");
      if (set != null) {
        query.addFilterQuery(Options.getInstance().getJSONObject("OAI").getJSONObject("sets").getJSONObject(set).getString("filter"));
      }

      if (req.getParameter("resumptionToken") != null) {
        String rt = req.getParameter("resumptionToken").replaceAll(" ", "+");
        // CHANGED by PS - NPE checks
        String[] parts = rt.split(":");
        if (parts.length > 2) {
            set = parts[1];
            query.addFilterQuery(Options.getInstance().getJSONObject("OAI").getJSONObject("sets").getJSONObject(set).getString("filter"));
            query.setParam(CursorMarkParams.CURSOR_MARK_PARAM, parts[0]);
        } else {
            query.setParam(CursorMarkParams.CURSOR_MARK_PARAM, parts[0]);
        }
      } else {
        query.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
      }

      QueryResponse qr = getClient().query(DataCollections.catalog.name(), query);
      String nextCursorMark = qr.getNextCursorMark();
      SolrDocumentList docs = qr.getResults();
      if (docs.getNumFound() == 0) {
        ret.append("<error code=\"noRecordsMatch\">no record match the search criteria</error>");
      } else {
        ret.append("<" + verb + ">");

        for (SolrDocument doc : docs) {

          Date datestamp = (Date) doc.getFirstValue(SORT_FIELD);
          ret.append("<record>");
          ret.append("<header>");
          ret.append("<identifier>").append(doc.getFirstValue("identifier")).append("</identifier>");
          ret.append("<datestamp>")
                  .append(DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()).format(datestamp.toInstant()))
                  .append("</datestamp>");
          ret.append("<setSpec>").append(set).append("</setSpec>");
          ret.append("</header>");

          String raw = (String) doc.getFirstValue("raw");
          MarcRecord mr = MarcRecord.fromDoc(doc);
          ret.append(mr.toXml(onlyIdentifiers));

          ret.append("</record>");
        }
        solr.close();
        if (docs.size() == rows) {
          Date last = (Date) docs.get(docs.size() - 1).getFieldValue(SORT_FIELD);

          ret.append("<resumptionToken completeListSize=\"" + docs.getNumFound() + "\">");
                  //.append(DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS").withZone(ZoneId.systemDefault()).format(last.toInstant()))

          //CHANGED by PS - NPE checks
          if (set != null) {
              ret.append(nextCursorMark).append(":").append(set).append(":").append("marc21");
          } else {
              ret.append(nextCursorMark).append(":").append("marc21");
          }

          ret.append("</resumptionToken>");
        }
        ret.append("</" + verb + ">");
      }
      ret.append("</OAI-PMH>");
      return ret.toString();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }

  public static String getRecord(HttpServletRequest req) {
    Options opts = Options.getInstance();
    StringBuilder ret = new StringBuilder();
    ret.append(headerOAI())
            .append(responseDateTag())
            .append(requestTag(req));
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {

      SolrQuery query = new SolrQuery("*")
              .setRows(1)
              .addFilterQuery("identifier:\"" + req.getParameter("identifier") + "\"")
              .setFields(SORT_FIELD, "identifier,raw,dntstav,datum_stavu,license,license_history,historie_stavu,granularity");

      String set = req.getParameter("set");
      if ("SDNNT-A".equals(set)) {
        query.addFilterQuery("dntstav:A");
      } else if ("SDNNT-N".equals(set)) {
        query.addFilterQuery("dntstav:N");
      } else {
        query.addFilterQuery("dntstav:*");
      }

      SolrDocumentList docs = solr.query(DataCollections.catalog.name(), query).getResults();
      if (docs.getNumFound() == 0) {
        ret.append("<error code=\"idDoesNotExist\">No matching identifier</error>");
      } else {
        ret.append("<GetRecord>");
        for (SolrDocument doc : docs) {

          Date datestamp = (Date) doc.getFirstValue(SORT_FIELD);

          ret.append("<record>");
          ret.append("<header>");
          ret.append("<identifier>").append(doc.getFirstValue("identifier")).append("</identifier>");
          ret.append("<datestamp>")
                  // .append(DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(datestamp.toInstant()))
                  .append(DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault()).format(datestamp.toInstant()))
                  .append("</datestamp>");
          ret.append("<setSpec>").append(set).append("</setSpec>");
          ret.append("</header>");
          //String raw = (String) doc.getFirstValue("raw");
          //MarcRecord mr = MarcRecord.fromRAWJSON(raw);
          
          MarcRecord mr = MarcRecord.fromDoc(doc);
          
          ret.append(mr.toXml(false));
          ret.append("</record>");
        }
        ret.append("</GetRecord>");
      }
      ret.append("</OAI-PMH>");
      solr.close();

      return ret.toString();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }

}
