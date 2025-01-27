package cz.inovatika.sdnnt.index;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import cz.inovatika.sdnnt.index.AbstractXMLImport.XMLImportDesc;
import cz.inovatika.sdnnt.index.utils.imports.ImporterUtils;
import cz.inovatika.sdnnt.indexer.models.Import;
import cz.inovatika.sdnnt.model.DataCollections;

/**
 *
 * @author alberto
 */
public class XMLImporterDistri extends AbstractXMLImport {

    private static final String IMPORT_IDENTIFIER = "distri.cz";

    public static List<String> ELEMENT_NAMES = Arrays.asList("NAME", "EAN", "AUTHOR", "EDITION", "AVAILABILITY");
    public static final Logger LOGGER = Logger.getLogger(XMLImporterDistri.class.getName());

    public static final String DEFAULT_IMPORT_URL = "https://www.distri.cz/xml-full/3d44df7d05a24ca09134fb6b4979c786/";
    
    private XMLImportDesc importDescription = null;

    public XMLImporterDistri(String strLogger, String groupId, String url, int checkPNStates, String chronoUnit) {
        super(strLogger, url, checkPNStates, chronoUnit);
        if (this.url == null) {
            this.url = DEFAULT_IMPORT_URL;
        }
        this.importDescription =  new XMLImportDesc(IMPORT_IDENTIFIER, groupId);
    }

    @Override
    public String getImportIdentifier() {
        return IMPORT_IDENTIFIER;
    }


    
    public LinkedHashSet<String> processFromStream(String uri, InputStream is, String from_id, SolrClient solrClient, LinkedHashSet<String> itemsToSkip)
            throws XMLStreamException, SolrServerException, IOException {
        getLogger().log(Level.INFO, "Processing {0}", uri);
        List<String> allIdentifiersToPN = new ArrayList<>();
        allIdentifiersToPN = readXML(is, solrClient, itemsToSkip);
        solrClient.commit(DataCollections.imports_documents.name());
        
        ImporterUtils.changePNState(getLogger(), solrClient, allIdentifiersToPN, IMPORT_IDENTIFIER);
        
        indexImportSummary(solrClient);
        return new LinkedHashSet<>(allIdentifiersToPN);
    }

    private List<String> readXML(InputStream is, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = null;
        try {
            Set<String> allIdentifiers = new HashSet<>();
            reader = inputFactory.createXMLStreamReader(is);
            List<String> readDocument = readDocument(reader, solrClient, itemsToSkip);
            allIdentifiers.addAll(readDocument);
            return new ArrayList<>(allIdentifiers);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    private List<String> readDocument(XMLStreamReader reader, SolrClient solrClient, LinkedHashSet<String> itemsToSkip)
            throws XMLStreamException, IOException {
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
            case XMLStreamReader.START_ELEMENT:
                String elementName = reader.getLocalName();
                if (elementName.equals("SHOP")) {
                    return readShop(reader, solrClient, itemsToSkip);
                }
                break;
            case XMLStreamReader.END_ELEMENT:
                break;
            }
        }
        return new ArrayList<>();
    }

    private List<String> readShop(XMLStreamReader reader, SolrClient solrClient, LinkedHashSet<String> itemsToSkip)
            throws XMLStreamException, IOException {
        List<String> allIdents = new ArrayList<>();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
            case XMLStreamReader.START_ELEMENT:
                String elementName = reader.getLocalName();
                if (elementName.equals("ITEM")) {
                    List<String> itemsIdents = readItem(reader, solrClient, itemsToSkip);
                    allIdents.addAll(itemsIdents);
                } else if (elementName.equals("error")) {
                    // ret.put("error", reader.getElementText());
                }
                break;
            case XMLStreamReader.END_ELEMENT:
                break;
            }
        }
        return allIdents;
    }

    private List<String> readItem(XMLStreamReader reader, SolrClient solrClient, LinkedHashSet<String> itemsToSkip)
            throws XMLStreamException, IOException {
        // List<String> allIdents = new ArrayList<>();
        HashMap<String, Object> item = new HashMap<>();
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
            case XMLStreamReader.START_ELEMENT:
                String elementName = reader.getLocalName();
                String val = reader.getElementText();
                if (ELEMENT_NAMES.contains(elementName)) {
                    item.put(elementName, val);
                }
                break;
            case XMLStreamReader.END_ELEMENT:
                elementName = reader.getLocalName();
                if (elementName.equals("ITEM")) {
                    // by ean 
                    // by title
                    return toIndex(item, solrClient, itemsToSkip);
                }
            }
        }
        return new ArrayList<>();
    }

    private List<String> toIndex(Map<String, Object> item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        // closure 
        try {
            this.importDescription.incrementTotal();
            if (!item.containsKey("EAN")) {
                LOGGER.log(Level.INFO, "{0} nema EAN, vynechame", item.get("NAME"));
                this.importDescription.incrementSkipped();
                return new ArrayList<>();
            }
            if (item.containsKey("AVAILABILITY") && "0".equals(item.get("AVAILABILITY"))) {
                LOGGER.log(Level.INFO, "{0} neni dostupny (AVAILABILITY=0), vynechame", item.get("NAME"));
                this.importDescription.incrementSkipped();
                return new ArrayList<>();
            }
            if (this.importDescription.getFirstId() == null) {
                importDescription.setFirstId((String)item.get("EAN"));
            }
            this.importDescription.setLastId((String)item.get("EAN"));

            if (fromId > Long.parseLong((String)item.get("EAN"))) {
                return new ArrayList<>();
            }

            if (item.containsKey("EDITION") && "audioknihy".equals(((String)item.get("EDITION")).toLowerCase())) {
                LOGGER.log(Level.INFO, "{0} ma format audioknihy, vynechame", this.importDescription.getLastId());
                this.importDescription.incrementSkipped();
                return new ArrayList<>();
            }

            SolrInputDocument idoc = new SolrInputDocument();
            String item_id = item.get("EAN") + "_" + this.importDescription.getImportOrigin();
            idoc.setField("import_id", this.importDescription.getImportId());
            idoc.setField("import_date", this.importDescription.getImportDate());
            idoc.setField("item_id", item_id);

            idoc.setField("id", this.importDescription.getImportId() + "_" + item.get("EAN"));
            idoc.setField("ean", item.get("EAN"));
            idoc.setField("name", item.get("NAME"));
            if (item.containsKey("AUTHOR")) {
                idoc.setField("author", item.get("AUTHOR"));
            }
            if (!item.containsKey("URL")) {
                item.put("URL", "https://www.distri.cz/Search/Result/?Text=" + item.get("EAN"));
            }

            addDedup(item);
            addFrbr(item);
            SolrDocument isControlled = Import.isControlled(item_id);
            if (isControlled != null) {
                // LOGGER.log(Level.INFO, "{0} ma format audioknihy, vynechame", isControlled);
                idoc.setField("controlled", true);
                idoc.setField("controlled_note", isControlled.get("controlled_note"));
                idoc.setField("controlled_date", isControlled.get("controlled_date"));
                idoc.setField("controlled_user", isControlled.get("controlled_user"));
            }

            
            List<String> foundIdentifiers = findInCatalogByEan(item, solrClient, itemsToSkip);
//            if (!item.containsKey("found")) {
//                foundIdentifiers = findInCatalogByTitle(item, solrClient, itemsToSkip);
//            }
            
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
            LOGGER.log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }

    private void addDedup(Map<String, Object> item) {
        item.put("dedup_fields", "");
    }

    private void addFrbr(Map<String, Object> item) {
        String frbr = "";
        if (item.containsKey("AUTHOR")) {
            frbr += item.get("AUTHOR");
        }

        frbr += "/" + item.get("NAME");

        item.put("frbr", MD5.normalize(frbr));
    }

    
    
//    public List<String> findInCatalogByEan(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
//        try {
//            String title = "nazev:(" + ClientUtils.escapeQueryChars(((String) item.get("NAME")).trim()) + ")";
//            if (item.containsKey("AUTHOR") && !((String) item.get("AUTHOR")).isBlank()) {
//                title += " AND author:(" + ClientUtils.escapeQueryChars((String) item.get("AUTHOR")) + ")";
//            }
//            String q = "ean:\"" + item.get("EAN") + "\"^10.0 OR (" + title + ")";
//            SolrQuery query = new SolrQuery(q).setRows(100).setParam("q.op", "AND")
//                    .setFields(
//                            "identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,datum_kurator_stav");
//            String ean = item.get("EAN").toString();
//            return super.findCatalogItem(item, solrClient ,query, URLEncoder.encode(title, Charset.forName("UTF-8")), ean, itemsToSkip);
//        } catch (SolrServerException | IOException ex) {
//            getLogger().log(Level.SEVERE, null, ex);
//            return new ArrayList<>();
//        }
//    }
    
//    public List<String> findInCatalogByTitle(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
//        try {
//            String title = "nazev:(" + ClientUtils.escapeQueryChars(((String) item.get("NAME")).trim()) + ")";
//            if (item.containsKey("AUTHOR") && !((String) item.get("AUTHOR")).isBlank()) {
//                title += " AND author:(" + ClientUtils.escapeQueryChars((String) item.get("AUTHOR")) + ")";
//            }
//            SolrQuery query = new SolrQuery(title).setRows(DEFAULT_NUMBER_HITS_BY_TITLE).setParam("q.op", "AND")
//                    .setFields(
//                            "identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,datum_kurator_stav");
//            return super.findCatalogItem(item, solrClient ,query, null, itemsToSkip);
//        } catch (SolrServerException | IOException ex) {
//            getLogger().log(Level.SEVERE, null, ex);
//            return new ArrayList<>();
//        }
//    
//    }    
    
    
    public List<String> findInCatalogByEan(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {
            String q = "ean:\"" + item.get("EAN")+"\"";
            SolrQuery query = new SolrQuery(q).setRows(100).setParam("q.op", "AND")
                    .setFields(
                            "identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,datum_kurator_stav");
            String ean = item.get("EAN").toString();
            return super.findCatalogItem(item, solrClient ,query,  ean, itemsToSkip);
        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
    }

    @Override
    public XMLImportDesc getImportDesc() {
        return this.importDescription;
    }

    
    public static void main(String[] args) {
        
        long groupId = System.currentTimeMillis();
        String hexGroupId = Long.toHexString(groupId);

        XMLImporterDistri distri = new XMLImporterDistri("logger", hexGroupId, "file:///c:/Users/happy/Projects/SeznamDNNT/distri.cz", 
                1, "days");
        distri.doImport(null, false, new LinkedHashSet<>());
        
        XMLImportDesc importDesc = distri.getImportDesc();
        System.out.println(importDesc.toString());
        
    }
    
}
