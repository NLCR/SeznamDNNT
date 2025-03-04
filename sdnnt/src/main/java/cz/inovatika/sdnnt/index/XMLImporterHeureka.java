
package cz.inovatika.sdnnt.index;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.index.AbstractXMLImport.XMLImportDesc;
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

    public XMLImporterHeureka(String strLogger,  String groupId, String url,int checkPNStates, String chronoUnit) {
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

            SolrInputDocument idoc = new SolrInputDocument();
            String item_id = ean + "_" + importDescription.getImportOrigin(); // import_origin;
            idoc.setField("import_id", importDescription.getImportId());
            idoc.setField("import_date", importDescription.getImportDate());
            idoc.setField("id", importDescription.getImportId() + "_" + item_id);
            idoc.setField("item_id", item_id);

            idoc.setField("item", new JSONObject(item).toString());

            addDedup(item);
            addFrbr(item);
            List<String> foundIdentifiers = findInCatalogByEan(item, solrClient, itemsToSkip);
            SolrDocument isControlled = isControlled(item_id, solrClient);

            /*
            if (isControlled != null) {
                idoc.setField("controlled", true);
                idoc.setField("controlled_note", isControlled.get("controlled_note"));
                idoc.setField("controlled_date", isControlled.get("controlled_date"));
                idoc.setField("controlled_user", isControlled.get("controlled_user"));
            }*/


            if (item.containsKey("found")) {
                idoc.setField("ean", item.get("EAN"));
                idoc.setField("name", item.get(FIELD_MAPPING.get("NAME")));
                if (item.containsKey(FIELD_MAPPING.get("AUTHOR"))) {
                    idoc.setField("author", item.get(FIELD_MAPPING.get("AUTHOR")));
                }
                
                idoc.setField("identifiers", item.get("identifiers"));
                idoc.setField("na_vyrazeni", item.get("na_vyrazeni"));
                idoc.setField("hits_na_vyrazeni", item.get("hits_na_vyrazeni"));
                idoc.setField("catalog", item.get("catalog"));
                idoc.setField("num_hits", item.get("num_hits"));
                idoc.setField("hit_type", item.get("hit_type"));
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

//    public List<String> findInCatalogByTitle(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
//        try {
//            String name = ((String) item.get(FIELD_MAPPING.get("NAME"))).replaceAll("\\s*\\[[^\\]]*\\]\\s*", "");
//    
//            String[] parts = name.split(" - ");
//            String nazev = parts[0].trim();
//            String title = "";
//            if (nazev.isEmpty()) {
//                if (parts.length > 1) {
//                    title = " nazev:(" + ClientUtils.escapeQueryChars(parts[1].trim()) + ")";
//                }
//            } else {
//                title = "nazev:(" + ClientUtils.escapeQueryChars(nazev) + ")";
//                if (parts.length > 1) {
//                    title += " AND author:(" + ClientUtils.escapeQueryChars(parts[1].trim()) + ")";
//                }
//            }
//    
//            SolrQuery query = new SolrQuery(title)
//                    .setRows(DEFAULT_NUMBER_HITS_BY_TITLE)
//                    .setParam("q.op", "AND")
//                    // .addFilterQuery("dntstav:A OR dntstav:PA OR dntstav:NL")
//                    .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a, datum_kurator_stav");
//            
//            return super.findCatalogItem(item, solrClient,  query, item.get("EAN").toString(), itemsToSkip);
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
                    // .addFilterQuery("dntstav:A OR dntstav:PA OR dntstav:NL")
                    .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a, datum_kurator_stav");
            
            return super.findCatalogItem(item, solrClient,  query,item.get("EAN").toString(), itemsToSkip);
        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
}
        
//    public List<String> findInCatalog(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip ) {
//        try {
//            String name = ((String) item.get(FIELD_MAPPING.get("NAME"))).replaceAll("\\s*\\[[^\\]]*\\]\\s*", "");
//
//            String[] parts = name.split(" - ");
//            String nazev = parts[0].trim();
//            String title = "";
//            if (nazev.isEmpty()) {
//                if (parts.length > 1) {
//                    title = " nazev:(" + ClientUtils.escapeQueryChars(parts[1].trim()) + ")";
//                }
//            } else {
//                title = "nazev:(" + ClientUtils.escapeQueryChars(nazev) + ")";
//                if (parts.length > 1) {
//                    title += " AND author:(" + ClientUtils.escapeQueryChars(parts[1].trim()) + ")";
//                }
//            }
//
//
//            String q = "ean:\"" + item.get("EAN") + "\"^10.0 OR (" + title + ")";
//
//            SolrQuery query = new SolrQuery(q)
//                    .setRows(100)
//                    .setParam("q.op", "AND")
//                    // .addFilterQuery("dntstav:A OR dntstav:PA OR dntstav:NL")
//                    .setFields("identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a, datum_kurator_stav");
//            
//            return super.findCatalogItem(item, solrClient,  query,  item.get("EAN").toString(), itemsToSkip);
//        } catch (SolrServerException | IOException ex) {
//            getLogger().log(Level.SEVERE, null, ex);
//            return new ArrayList<>();
//        }
//    }

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
                1, "days");
        distri.doImport(null, false, new LinkedHashSet<>());
        
        XMLImportDesc importDesc = distri.getImportDesc();
        System.out.println(importDesc.toString());
        
    }


}
