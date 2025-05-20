
package cz.inovatika.sdnnt.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.index.utils.imports.ImporterUtils;
import cz.inovatika.sdnnt.model.DataCollections;

/**
 *
 * @author alberto
 */
public class XMLImporterHeureka extends AbstractXMLImport {

    public static final Map<String, String> FIELD_MAPPING = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("NAME", "PRODUCTNAME"), new AbstractMap.SimpleEntry<>("ISBN", "ISBN"));
    public static List<String> ELEMENT_NAMES = Arrays.asList("NAME", "EAN");

    public static final Logger LOGGER = Logger.getLogger(XMLImporterHeureka.class.getName());
    private static final String IMPORT_IDENTIFIER = "heureka";
    public static final String DEFAULT_IMPORT_URL = "https://feeds.mergado.com/palmknihy-cz-heureka-cz-cz-6-ed3e5a88767ca249029fda83b9326415.xml";

    private XMLImportDesc importDescription = null;//new XMLImportDesc(IMPORT_IDENTIFIER);

    public XMLImporterHeureka(String strLogger,  String groupId, String url,int checkPNStates, String chronoUnit, float match1, float match21, float match22) {
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

    //public LinkedHashSet<String> processFromStream(String uri, InputStream is, String from_id, SolrClient solrClient, LinkedHashSet<String> itemsToSkip)

    public LinkedHashSet<String>  processFromStream(String uri, InputStream is, String fromId, SolrClient solrClient,LinkedHashSet<String> itemsToSkip)
            throws XMLStreamException, SolrServerException, IOException {
        getLogger().log(Level.INFO, "Processing {0}", uri);
        
        List<String> allIdentifiersToPN = new ArrayList<>();
        allIdentifiersToPN = readXML(is, "SHOPITEM", solrClient,itemsToSkip);
        solrClient.commit(DataCollections.imports_documents.name());
        ImporterUtils.changePNState(getLogger(), solrClient, allIdentifiersToPN, IMPORT_IDENTIFIER);
        indexImportSummary(solrClient);
        return new LinkedHashSet<>(allIdentifiersToPN);

   }

    private List<String> readXML(InputStream is, String itemName, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) throws XMLStreamException {
        try {
            Set<String> allIdentifiers = new HashSet<>();
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
                allIdentifiers.addAll(toIndex(item, solrClient,itemsToSkip));
            }
            return new ArrayList<>(allIdentifiers);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }

    private List<String> toIndex(Map<String, String> item, SolrClient solrClient,LinkedHashSet<String> itemsToSkip) {
        try {
            this.importDescription.incrementTotal();
            if (this.importDescription.getFirstId() == null) {
                importDescription.setFirstId(item.get("ITEM_ID"));
            }
            this.importDescription.setLastId(item.get("ITEM_ID"));
            if (fromId > Integer.parseInt(item.get("ITEM_ID"))) {
                return new ArrayList<>();
            }

            if (item.containsKey(FIELD_MAPPING.get("NAME"))
                    && item.get(FIELD_MAPPING.get("NAME")).toLowerCase().contains("[audiokniha]")) {
                LOGGER.log(Level.INFO, "{0} ma format audioknihy, vynechame", this.importDescription.getLastId());
                this.importDescription.incrementSkipped();
                return new ArrayList<>();
            }

            String ean;
            if (item.containsKey("ISBN")) {
                ISBNValidator isbn = ISBNValidator.getInstance();
                ean = item.get("ISBN");
                ean = isbn.validate(ean);
                if (ean != null) {
                    item.put("EAN", ean);
                } else {
                    item.put("EAN", item.get("ISBN"));
                }
            } else {
                LOGGER.log(Level.INFO, "{0} nema ISBN, vynechame", item.get("PRODUCTNAME"));
                return new ArrayList<>();
            }

            Map<String, Object> eanItem = new HashMap<>(item);
            // tady je pouze dle eanu
            ImportResult eanResult = findInCatalogByEan(eanItem, solrClient, itemsToSkip);

            Map<String, Object> titlesItem = new HashMap<>(item);
            ImportResult titleResult = findInCatalogByTitle(titlesItem, solrClient, itemsToSkip);

            if (eanResult.found()) {
                SolrInputDocument idoc = new SolrInputDocument();
                String item_id = ean + "_" + importDescription.getImportOrigin(); // import_origin;
                idoc.setField("import_id", importDescription.getImportId());
                idoc.setField("import_date", importDescription.getImportDate());
                idoc.setField("id", importDescription.getImportId() + "_" + item_id);
                idoc.setField("item_id", item_id);

                idoc.setField("item", new JSONObject(eanItem).toString());

                addDedup(eanItem);
                addFrbr(eanItem);

                idoc.setField("ean", eanItem.get("EAN"));
                idoc.setField("name", eanItem.get(FIELD_MAPPING.get("NAME")));
                if (eanItem.containsKey(FIELD_MAPPING.get("AUTHOR"))) {
                    idoc.setField("author", eanItem.get(FIELD_MAPPING.get("AUTHOR")));
                }

                idoc.setField("identifiers", eanItem.get("identifiers"));
                idoc.setField("na_vyrazeni", eanItem.get("na_vyrazeni"));
                idoc.setField("hits_na_vyrazeni", eanItem.get("hits_na_vyrazeni"));
                idoc.setField("catalog", eanItem.get("catalog"));
                idoc.setField("num_hits", eanItem.get("num_hits"));
                idoc.setField("hit_type", eanItem.get("hit_type"));
                idoc.setField("dntstav", eanItem.get("dntstav"));

                solrClient.add(DataCollections.imports_documents.name(), idoc);
                this.importDescription.incrementIndexed();
                return  eanResult.getFoundIdentifiers();

            }   else if (titleResult.found()) {

                SolrInputDocument idoc = new SolrInputDocument();
                String item_id = ean + "_" + importDescription.getImportOrigin(); // import_origin;
                idoc.setField("import_id", importDescription.getImportId());
                idoc.setField("import_date", importDescription.getImportDate());
                idoc.setField("id", importDescription.getImportId() + "_" + item_id);
                idoc.setField("item_id", item_id);

                idoc.setField("item", new JSONObject(titlesItem).toString());

                addDedup(titlesItem);
                addFrbr(titlesItem);

                idoc.setField("ean", titlesItem.get("EAN"));
                idoc.setField("name", titlesItem.get(FIELD_MAPPING.get("NAME")));
                if (titlesItem.containsKey(FIELD_MAPPING.get("AUTHOR"))) {
                    idoc.setField("author", titlesItem.get(FIELD_MAPPING.get("AUTHOR")));
                }

                idoc.setField("identifiers", titlesItem.get("identifiers"));
                idoc.setField("na_vyrazeni", titlesItem.get("na_vyrazeni"));
                idoc.setField("hits_na_vyrazeni", titlesItem.get("hits_na_vyrazeni"));
                idoc.setField("catalog", titlesItem.get("catalog"));
                idoc.setField("num_hits", titlesItem.get("num_hits"));
                idoc.setField("hit_type", titlesItem.get("hit_type"));
                idoc.setField("dntstav", titlesItem.get("dntstav"));

                solrClient.add(DataCollections.imports_documents.name(), idoc);
                this.importDescription.incrementIndexed();
                return  titleResult.getFoundIdentifiers();
            }
            return new ArrayList<>();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }

    private void addDedup(Map item) {
        item.put("dedup_fields", "");
    }

    private void addFrbr(Map item) {
        String frbr = "";
        if (item.containsKey(FIELD_MAPPING.get("AUTHOR"))) {
            frbr += item.get(FIELD_MAPPING.get("AUTHOR"));
        }

        frbr += "/" + item.get(FIELD_MAPPING.get("NAME"));

        item.put("frbr", MD5.normalize(frbr));
    }

    public ImportResult findInCatalogByTitle(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {
            String q = "ean:\"" + item.get("EAN")+"\"";
            SolrQuery eanQuery = new SolrQuery(q).setRows(100).setParam("q.op", "AND")
                    .setFields("identifier");
            List<JSONObject>eanItems = fetchDocsFromSolr(solrClient, eanQuery, doc->{return true;});

            if (!eanItems.isEmpty()) {
                String name = ((String) item.get(FIELD_MAPPING.get("NAME"))).replaceAll("\\s*\\[[^\\]]*\\]\\s*", "");
                String nakladatel = (String) item.get("MANUFACTURER");
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

                String fq = "ean:\"" + item.get("EAN")+"\"";

                SolrQuery query = new SolrQuery(title)
                        .setRows(DEFAULT_NUMBER_HITS_BY_TITLE)
                        .setParam("q.op", "AND")
                        .addFilterQuery(fq)
                        .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a, datum_kurator_stav, marc_245a, marc_245b, author, nakladatel");

                List<String> foundItems = super.findCatalogItem(item, solrClient, query,"noean", itemsToSkip, (doc -> {
                    String id = doc.optString("identifier");

                    boolean matched = match_1(doc, nazev, "", parts.length > 1 ? parts[1] : "", match1);

                    String catalogNormalizedTitle = normalizeObjects(doc.optJSONArray("marc_245a"), doc.optJSONArray("marc_245b"));
                    String distriNormalizedTitle = normalizeObjects(nazev, "");
                    if (matched) {
                        return matched;
                    }

                    matched = match_2(doc, nazev, "", nakladatel, match21, match22);
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
    
    public ImportResult  findInCatalogByEan(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {
            String q = "ean:\"" + item.get("EAN")+"\"";
            SolrQuery query = new SolrQuery(q)
                    .setRows(100)
                    .setParam("q.op", "AND")
                    .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a, datum_kurator_stav");
            
            List<String> eanItems =  super.findCatalogItem(item, solrClient,  query,"ean", itemsToSkip, doc-> {
                return true;
            });
            return new ImportResult(eanItems,item, eanItems.size() > 0 ? eanItems.get(0) : null);

        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ImportResult(new ArrayList<>(),item, null);
        }
    }
        

    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public XMLImportDesc getImportDesc() {
        return this.importDescription;
    }
    
    public static void main(String[] args) {
        XMLImporterHeureka distri = new XMLImporterHeureka("logger", "grpab",
                "https://feeds.mergado.com/palmknihy-cz-heureka-cz-cz-6-ed3e5a88767ca249029fda83b9326415.xml",
                1, "days", 1.0f, 1.0f, 0.5f);
        distri.doImport(null, false, new LinkedHashSet<>());

        XMLImportDesc importDesc = distri.getImportDesc();
        System.out.println(importDesc.toString());
    }
}
