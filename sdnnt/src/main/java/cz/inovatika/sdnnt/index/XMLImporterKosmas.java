/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.AbstractXMLImport.XMLImportDesc;
import cz.inovatika.sdnnt.index.utils.imports.ImporterUtils;

import static cz.inovatika.sdnnt.index.Indexer.getClient;
import cz.inovatika.sdnnt.indexer.models.Import;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.LoggerAware;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author alberto
 */
public class XMLImporterKosmas extends AbstractXMLImport {

    // https://www.kosmas.cz/atl_shop/nkp.xml
    private static final String DEFAULT_IMPORT_URL = "https://www.kosmas.cz/atl_shop/nkp.xml";
    private static final String IMPORT_IDENTIFIER = "kosmas";

    private XMLImportDesc importDescription = null;//new XMLImportDesc(IMPORT_IDENTIFIER);
    private Logger logger = Logger.getLogger(XMLImporterKosmas.class.getName());
    private String url = DEFAULT_IMPORT_URL;

    public XMLImporterKosmas(String strLogger, String groupId, String url, int checkPNStates, String chronoUnit) {
        super(strLogger, url, checkPNStates, chronoUnit);
        if (this.url == null) {
            this.url = DEFAULT_IMPORT_URL;
        }
        this.importDescription = new XMLImportDesc(IMPORT_IDENTIFIER, groupId);
    }
    
    @Override
    public String getImportIdentifier() {
        return IMPORT_IDENTIFIER;
    }

     public LinkedHashSet<String> processFromStream(String uri, InputStream is, String fromId, SolrClient solrClient,LinkedHashSet<String> itemsToSkip)
            throws SolrServerException, IOException, XMLStreamException {
        getLogger().log(Level.INFO, "Processing {0}", uri);
        List<String> allIdentifiersToPN = new ArrayList<>();
        allIdentifiersToPN = readXML(is, "ARTICLE", solrClient, itemsToSkip);
        solrClient.commit(DataCollections.imports_documents.name());
        ImporterUtils.changePNState(getLogger(), solrClient, allIdentifiersToPN, IMPORT_IDENTIFIER);
        //ImporterUtils.changeImportDocs(getLogger(), solrClient, importDescription);
        indexImportSummary(solrClient);
        return new LinkedHashSet<>(allIdentifiersToPN);
    }

    private List<String> readXML(InputStream is, String itemName, SolrClient solrClient, LinkedHashSet<String> itemsToSkip)
            throws XMLStreamException, SolrServerException {
        try {
            Set<String> allIdentifiers = new HashSet<>();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            NodeList nodes = doc.getElementsByTagName(itemName);
            getLogger().info("Number of items " + nodes.getLength());
            // UpdateRequest req = null;
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
                allIdentifiers.addAll(toIndex(item, solrClient, itemsToSkip));
            }
            return new ArrayList<>(allIdentifiers);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }

    private List<String> toIndex(Map<String, String> item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {
            this.importDescription.incrementTotal();
            long id = this.importDescription.getTotal();
            if (this.importDescription.getTotal() % 100 == 0) {
                getLogger().info("Item number " + this.importDescription.getTotal());
            }

            if (!item.containsKey("EAN")) {
                getLogger().log(Level.INFO, "{0} nema EAN, vynechame", item.get("NAME"));
                return new ArrayList<>();
            }
            String ean = item.get("EAN");
            // https://github.com/NLCR/SeznamDNNT/issues/443
            if (!(ean.startsWith("977") || ean.startsWith("978") || ean.startsWith("979"))) {
                getLogger().log(Level.INFO, "EAN {0} nezacina s 977 nebo 978 nebo 979, vynechame", ean);
                return new ArrayList<>();
            }
            if (item.containsKey("EAN")) {
                id = Long.parseLong(item.get("EAN"));
            }
            if (importDescription.getFirstId() == null) {
                importDescription.setFirstId(id + "");
            }
            importDescription.setLastId(id + "");
            if (fromId > id) {
                return new ArrayList<>();
            }

            SolrInputDocument idoc = new SolrInputDocument();
            String item_id = ean + "_" + this.importDescription.getImportOrigin();
            idoc.setField("import_id", this.importDescription.getImportId());
            idoc.setField("import_date", this.importDescription.getImportDate());
            idoc.setField(ImporterUtils.IMPORT_ID_KEY, this.importDescription.getImportId() + "_" + id);
            idoc.setField("item_id", item_id);
            item.put("URL", "https://www.kosmas.cz/hledej/?Filters.ISBN_EAN=" + item.get("EAN"));

            idoc.setField("item", new JSONObject(item).toString());

            addDedup(item);
            addFrbr(item);

            idoc.setField("ean", item.get("EAN"));
            idoc.setField("name", item.get("NAME"));
            if (item.containsKey("AUTHOR")) {
                idoc.setField("author", item.get("AUTHOR"));
            }

            /*
            SolrDocument isControlled = isControlled(item_id, solrClient);
            if (isControlled != null) {
                idoc.setField("controlled", true);
                idoc.setField("controlled_note", isControlled.get("controlled_note"));
                idoc.setField("controlled_date", isControlled.get("controlled_date"));
                idoc.setField("controlled_user", isControlled.get("controlled_user"));
            }*/

            List<String> foundIdentifiers = findInCatalogByEan(item, solrClient, itemsToSkip);
            if (item.containsKey("found")) {
                idoc.setField("identifiers", item.get("identifiers"));
                idoc.setField("na_vyrazeni", item.get("na_vyrazeni"));
                idoc.setField("hits_na_vyrazeni", item.get("hits_na_vyrazeni"));
                idoc.setField("num_hits", item.get("num_hits"));
                idoc.setField("hit_type", item.get("hit_type"));
                idoc.setField("item", new JSONObject(item).toString());
                idoc.setField("dntstav", item.get("dntstav"));
                solrClient.add(DataCollections.imports_documents.name(), idoc);
                this.importDescription.incrementIndexed();
            }
            return foundIdentifiers;
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }
    
    
    @Override
    public String getUrl() {
        return this.url;
    }
    
    private void addDedup(Map<String,String> item) {
        item.put("dedup_fields", "");
    }

    private void addFrbr(Map<String, String> item) {
        String frbr = "";
        if (item.containsKey("AUTHOR")) {
            frbr += item.get("AUTHOR");
        }

        frbr += "/" + item.get("NAME");

        item.put("frbr", MD5.normalize(frbr));
    }

//    public List<String> findInCatalogByTitle(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
//        try {
//
//            String title = "nazev:(" + ClientUtils.escapeQueryChars(((String) item.get("NAME")).trim()) + ")";
//            if (item.containsKey("AUTHOR") && !((String) item.get("AUTHOR")).isBlank()) {
//              title += " AND author:(" + ClientUtils.escapeQueryChars((String) item.get("AUTHOR")) + ")";
//            }
//
//            SolrQuery query = new SolrQuery(title)
//                    .setRows(DEFAULT_NUMBER_HITS_BY_TITLE)
//                    .setParam("q.op", "AND")
//                    .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,datum_kurator_stav");
//            
//            return findCatalogItem(item, solrClient, query,  item.get("EAN").toString(), itemsToSkip);
//        } catch (SolrServerException | IOException ex) {
//            getLogger().log(Level.SEVERE, null, ex);
//            return new ArrayList<>();
//        }
//    }
    
    public List<String> findInCatalogByEan(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {


            String q = "ean:\"" + item.get("EAN")+"\"";

            SolrQuery query = new SolrQuery(q)
                    .setRows(100)
                    .setParam("q.op", "AND")
                    .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,datum_kurator_stav");
            
            return findCatalogItem(item, solrClient, query,  item.get("EAN").toString(), itemsToSkip);
        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }

    }
    
    @Override
    public XMLImportDesc getImportDesc() {
        return this.importDescription;
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    public Logger getLogger() {
        return this.logger;
    }
    
    public static void main(String[] args) {
        
        AbstractXMLImport kosmas = new XMLImporterKosmas("logger", "gcd", 
                "c:\\Users\\happy\\Projects\\SeznamDNNT\\nkp.xml", 
                10, 
                "days");
        
        LinkedHashSet<String> doImport = kosmas.doImport(null, false, new LinkedHashSet<>());
        
        
        
        //lastImport = kosmas;
        //XMLImporterKosmas kosmas = new XMLImporterKosmas("test", DEFAULT_IMPORT_URL);
        //kosmas.doImport(null, false);
//        HttpSolrClient build = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
//        try(build) {
//            XMLImportDesc desc = new XMLImportDesc("kosmas");
//            desc.setImportId("1736973060");
//            ImporterUtils.changeImportDocs(Logger.getLogger("test"), build, desc);
//
//        } catch(Exception ex) {
//            ex.printStackTrace();  
//        }
    }

}
