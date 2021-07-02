/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.oai;

import cz.inovatika.sdnnt.Options;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 *
 * @author alberto
 */
public class OAIRequest {

  // Muze byt indextime nebo datestamp
  static String SORT_FIELD = "indextime";

  public static String headerOAI() {
    return "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n";
  }

  public static String responseDate() {
    return ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);
  }

  public static String identify(HttpServletRequest req) {
    String xml = headerOAI()
            + "<responseDate>" + responseDate() + "</responseDate>\n"
            + "<request>" + req.getRequestURL() + "</request>\n"
            + "<Identify>\n"
            + "<repositoryName>Seznam děl nedostupných na trhu</repositoryName>\n"
            + "<baseURL>" + req.getRequestURL() + "</baseURL>\n"
            + "<protocolVersion>2.0</protocolVersion>\n"
            + "<adminEmail>sdnnt@nkp.cz</adminEmail>\n"
            + "<earliestDatestamp>2012-06-30T22:26:40Z</earliestDatestamp>\n"
            + "<deletedRecord>transient</deletedRecord>\n"
            + "<granularity>YYYY-MM-DDThh:mm:ssZ</granularity>\n"
            + "<description>\n"
            + "<oai-identifier xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">\n"
            + "<scheme>oai</scheme>\n"
            + "<repositoryIdentifier>aleph-nkp.cz</repositoryIdentifier>\n"
            + "<delimiter>:</delimiter>\n"
            + "<sampleIdentifier>oai:aleph-nkp.cz:NKC01-000000001</sampleIdentifier>\n"
            + "</oai-identifier>\n"
            + "</description>"
            + "</Identify>\n"
            + "</OAI-PMH>";
    return xml;
  }

  public static String listSets(HttpServletRequest req) {
    String xml = headerOAI()
            + "<responseDate>" + responseDate() + "</responseDate>\n"
            + "<request verb=\"ListSets\">" + req.getRequestURL() + "</request>\n"
            + "<ListSets>"
            + "<set><setSpec>SDNNT-ALL</setSpec><setName>Seznam děl nedostupných na trhu (všechny stavy)</setName></set>"
            + "<set><setSpec>SDNNT-A</setSpec><setName>Seznam děl nedostupných na trhu (zařazeno)</setName></set>"
            + "<set><setSpec>SDNNT-N</setSpec><setName>Seznam děl nedostupných na trhu (ne zařazeno)</setName></set>"
            + "</ListSets>\n"
            + "</OAI-PMH>";
    return xml;
  }

  public static String metadataFormats(HttpServletRequest req) {
    String xml = headerOAI()
            + "<responseDate>" + responseDate() + "</responseDate>\n"
            + "<request verb=\"ListMetadataFormats\">" + req.getRequestURL() + "</request>\n"
            + "<ListMetadataFormats>"
            + "<metadataFormat><metadataPrefix>marc21</metadataPrefix><schema>http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd</schema>"
            + "<metadataNamespace>http://www.loc.gov/MARC21/slim</metadataNamespace>"
            + "</metadataFormat>"
            + "</ListMetadataFormats>"
            + "</OAI-PMH>";
    return xml;
  }

  public static String listRecords(HttpServletRequest req, boolean onlyIdentifiers) {
    Options opts = Options.getInstance();
    StringBuilder ret = new StringBuilder();
    int rows = 100;
    ret.append(headerOAI())
            .append("<responseDate>" + responseDate() + "</responseDate>")
            .append("<request verb=\"ListRecords\" metadataPrefix=\"marc21\" set=\"SDNNT\">" + req.getRequestURL() + "</request>")
            .append("<ListRecords>");
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {

      SolrQuery query = new SolrQuery("*")
              .setRows(rows)
              .setSort(SORT_FIELD, SolrQuery.ORDER.asc)
              .setFields(SORT_FIELD, "identifier,raw");
      if (req.getParameter("from") != null) {
        String from = req.getParameter("from");
        String until = "*";
        if (req.getParameter("until") != null) {
          until = req.getParameter("until");
        }
        query.addFilterQuery(SORT_FIELD + ":[" + from + " TO " + until + "]");
      }

      String set = req.getParameter("set");
      if ("SDNNT-A".equals(set)) {
        query.addFilterQuery("marc_990a:A");
      } else if ("SDNNT-N".equals(set)) {
        query.addFilterQuery("marc_990a:N");
      } else {
        query.addFilterQuery("-marc_990a:NNN");
      }

      if (req.getParameter("resumptionToken") != null) {
        String rt = req.getParameter("resumptionToken");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss[.SSS]");
        
        LocalDateTime d = LocalDateTime.parse(rt, formatter);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(d, ZoneId.systemDefault());
        query.addFilterQuery(SORT_FIELD + ":{" + zonedDateTime.format(DateTimeFormatter.ISO_INSTANT) + " TO *]");
      }

      SolrDocumentList docs = solr.query("catalog", query).getResults();
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
        MarcRecord mr = MarcRecord.fromJSON(raw);
        ret.append(mr.toXml(onlyIdentifiers));

        ret.append("</record>");
      }
      solr.close();
      if (docs.size() == rows) {
        Date last = (Date) docs.get(docs.size() - 1).getFieldValue(SORT_FIELD);

        ret.append("<resumptionToken completeListSize=\"" + docs.getNumFound() + "\">")
                .append(DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS").withZone(ZoneId.systemDefault()).format(last.toInstant()))
                .append("</resumptionToken>");
      }
      ret.append("</ListRecords></OAI-PMH>");
      return ret.toString();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }

  public static String getRecord(HttpServletRequest req) {
    Options opts = Options.getInstance();
    StringBuilder ret = new StringBuilder();
    int rows = 100;
    ret.append(headerOAI())
            .append("<responseDate>" + responseDate() + "</responseDate>")
            .append("<request verb=\"getRecord\" metadataPrefix=\"marc21\" set=\"SDNNT\">" + req.getRequestURL() + "</request>")
            .append("<GetRecord>");
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {

      SolrQuery query = new SolrQuery("*")
              .setRows(1)
              .addFilterQuery("identifier:\"" + req.getParameter("identifier") + "\"")
              .setFields("datestamp,raw");

      String set = req.getParameter("set");
      if ("SDNNT-A".equals(set)) {
        query.addFilterQuery("marc_990a:A");
      } else if ("SDNNT-N".equals(set)) {
        query.addFilterQuery("marc_990a:N");
      } else {
        query.addFilterQuery("-marc_990a:NNN");
      }

      SolrDocumentList docs = solr.query("catalog", query).getResults();
      for (SolrDocument doc : docs) {

        Date datestamp = (Date) doc.getFirstValue(SORT_FIELD);

        ret.append("<record>");
        ret.append("<header>");
        ret.append("<identifier>").append(doc.getFirstValue("identifier")).append("</identifier>");
        ret.append("<datestamp>")
                .append(DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(datestamp.toInstant()))
                .append("</datestamp>");
        ret.append("<setSpec>").append(set).append("</setSpec>");
        ret.append("</header>");
        String raw = (String) doc.getFirstValue("raw");
        MarcRecord mr = MarcRecord.fromJSON(raw);
        ret.append(mr.toXml(false));
        ret.append("</record>");
      }
      solr.close();

      ret.append("</GetRecord></OAI-PMH>");
      return ret.toString();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ex.toString();
    }
  }

}
