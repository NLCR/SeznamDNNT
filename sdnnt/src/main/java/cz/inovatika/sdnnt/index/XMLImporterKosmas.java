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

    public XMLImporterKosmas(String strLogger, String groupId, String url, int checkPNStates, String chronoUnit, float match1, float match21, float match22) {
        super(strLogger, url, checkPNStates, chronoUnit, match1, match21, match22);
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

            Map<String, Object> eanItem = new HashMap<>(item);
            ImportResult eanResult = findInCatalogByEan(eanItem, solrClient, itemsToSkip);

            Map<String, Object> titlesItem = new HashMap<>(item);
            ImportResult titleResult = findInCatalogByTitle(titlesItem, solrClient, itemsToSkip);

            if (eanResult.found()) {

                SolrInputDocument idoc = new SolrInputDocument();
                String item_id = ean + "_" + this.importDescription.getImportOrigin();
                idoc.setField("import_id", this.importDescription.getImportId());
                idoc.setField("import_date", this.importDescription.getImportDate());
                idoc.setField(ImporterUtils.IMPORT_ID_KEY, this.importDescription.getImportId() + "_" + id);
                idoc.setField("item_id", item_id);
                eanItem.put("URL", "https://www.kosmas.cz/hledej/?Filters.ISBN_EAN=" + eanItem.get("EAN"));

                idoc.setField("item", new JSONObject(eanItem).toString());

                idoc.setField("ean", eanItem.get("EAN"));
                idoc.setField("name", eanItem.get("NAME"));
                if (eanItem.containsKey("AUTHOR")) {
                    idoc.setField("author", eanItem.get("AUTHOR"));
                }

                idoc.setField("identifiers", eanItem.get("identifiers"));
                idoc.setField("na_vyrazeni", eanItem.get("na_vyrazeni"));
                idoc.setField("hits_na_vyrazeni", eanItem.get("hits_na_vyrazeni"));
                idoc.setField("num_hits", eanItem.get("num_hits"));
                idoc.setField("hit_type", eanItem.get("hit_type"));
                idoc.setField("item", new JSONObject(eanItem).toString());
                idoc.setField("dntstav", eanItem.get("dntstav"));
                if (eanResult.getEanIdent() != null) {
                    idoc.setField("skceanitem", eanResult.getEanIdent().getEanIdentifier());
                }

                solrClient.add(DataCollections.imports_documents.name(), idoc);
                this.importDescription.incrementIndexed();

                return eanResult.getFoundIdentifiers();

            }   else if (titleResult.found()) {

                SolrInputDocument idoc = new SolrInputDocument();
                String item_id = ean + "_" + this.importDescription.getImportOrigin();
                idoc.setField("import_id", this.importDescription.getImportId());
                idoc.setField("import_date", this.importDescription.getImportDate());
                idoc.setField(ImporterUtils.IMPORT_ID_KEY, this.importDescription.getImportId() + "_" + id);
                idoc.setField("item_id", item_id);
                titlesItem.put("URL", "https://www.kosmas.cz/hledej/?Filters.ISBN_EAN=" + titlesItem.get("EAN"));

                idoc.setField("item", new JSONObject(titlesItem).toString());

                idoc.setField("ean", titlesItem.get("EAN"));
                idoc.setField("name", titlesItem.get("NAME"));
                if (eanItem.containsKey("AUTHOR")) {
                    idoc.setField("author", titlesItem.get("AUTHOR"));
                }

                idoc.setField("identifiers", titlesItem.get("identifiers"));
                idoc.setField("na_vyrazeni", titlesItem.get("na_vyrazeni"));
                idoc.setField("hits_na_vyrazeni", titlesItem.get("hits_na_vyrazeni"));
                idoc.setField("num_hits", titlesItem.get("num_hits"));
                idoc.setField("hit_type", titlesItem.get("hit_type"));
                idoc.setField("item", new JSONObject(titlesItem).toString());
                idoc.setField("dntstav", titlesItem.get("dntstav"));
                if (titleResult.getEanIdent() != null) {
                    idoc.setField("skceanitem", titleResult.getEanIdent().getEanIdentifier());
                }


                solrClient.add(DataCollections.imports_documents.name(), idoc);
                this.importDescription.incrementIndexed();

                return titleResult.getFoundIdentifiers();
            }

            return new ArrayList<>();

        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }
    
    
    @Override
    public String getUrl() {
        return this.url;
    }
    


    public ImportResult findInCatalogByTitle(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {

            String q = "ean:\"" + item.get("EAN")+"\"";
            SolrQuery eanQuery = new SolrQuery(q).setRows(100).setParam("q.op", "AND")
                    .setFields("identifier");
            List<JSONObject>eanItems = fetchDocsFromSolr(solrClient, eanQuery, doc->{return true;});

            if (eanItems.size() > 0) {
                String distribName = (String) item.get("NAME");
                String authorName = (String) item.get("AUTHOR");
                String nakladatel = (String) item.get("PUBLISHING");

                String title = "nazev:(" + ClientUtils.escapeQueryChars(((String) item.get("NAME")).trim()) + ")";
                if (item.containsKey("AUTHOR") && !((String) item.get("AUTHOR")).isBlank()) {
                    title += " AND author:(" + ClientUtils.escapeQueryChars((String) item.get("AUTHOR")) + ")";
                }
                SolrQuery query = new SolrQuery(title)
                        .setRows(DEFAULT_NUMBER_HITS_BY_TITLE)
                        .setParam("q.op", "AND")
                        .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a, datum_kurator_stav, marc_245a, marc_245b, author, nakladatel");

                List<String> foundItems = super.findCatalogItem(item, solrClient, query,"noean", itemsToSkip, (doc -> {
                    String id = doc.optString("identifier");

                    boolean matched = match_1(doc, distribName, "", authorName, this.match1);
                    if (matched) {
                        return matched;
                    }
                    matched = match_2(doc, distribName, "", nakladatel, this.match21, this.match22);
                    if (matched) {
                        return matched;
                    }

                    return false;
                }));
                return new ImportResult(foundItems, item, eanItems.get(0).getString("identifier"));
            } else {
                return new ImportResult(new ArrayList<>(), item, null);
            }
        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ImportResult(new ArrayList<>(), item, null);
        }
    }


    public ImportResult findInCatalogByEan(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {

            String q = "ean:\"" + item.get("EAN")+"\"";
            SolrQuery query = new SolrQuery(q)
                    .setRows(100)
                    .setParam("q.op", "AND")
                    .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,datum_kurator_stav");


            List<String> eanItems =  findCatalogItem(item, solrClient, query,  "ean", itemsToSkip, doc-> {
                return true;
            });
            return new ImportResult(eanItems,item, eanItems.size() > 0 ? eanItems.get(0) : null);
        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ImportResult(new ArrayList<>(),item, null);
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
                "days", 1.0f,1.0f,0.5f);
        
        LinkedHashSet<String> doImport = kosmas.doImport(null, false, new LinkedHashSet<>());
    }

}
