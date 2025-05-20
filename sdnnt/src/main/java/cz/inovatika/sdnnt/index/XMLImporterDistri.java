package cz.inovatika.sdnnt.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import cz.inovatika.sdnnt.utils.MarcRecordFields;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import cz.inovatika.sdnnt.index.utils.imports.ImporterUtils;
import cz.inovatika.sdnnt.model.DataCollections;

/**
 *
 * @author alberto
 */
public class XMLImporterDistri extends AbstractXMLImport {

    public static final String DEFAULT_IMPORT_URL = "https://www.distri.cz/xml-full/3d44df7d05a24ca09134fb6b4979c786/";

    private static final String IMPORT_IDENTIFIER = "distri.cz";

    public static List<String> ELEMENT_NAMES = Arrays.asList("NAME", "EAN", "AUTHOR", "EDITION", "AVAILABILITY","SUBTITLE");
    public static final Logger LOGGER = Logger.getLogger(XMLImporterDistri.class.getName());

    private XMLImportDesc importDescription = null;


    public XMLImporterDistri(String strLogger, String groupId, String url, int checkPNStates, String chronoUnit, float match1, float match21, float match22) {
        super(strLogger, url, checkPNStates, chronoUnit, match1, match21, match22);
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
                    return toIndex(item, solrClient, itemsToSkip);
                }
            }
        }
        return new ArrayList<>();
    }

    private List<String> toIndex(Map<String, Object> item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        String ean = (String) item.get("EAN");
        try {
            this.importDescription.incrementTotal();
            LOGGER.log(Level.INFO, String.format("Total number is %d", this.importDescription.getTotal()));
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

            // ean verze
            Map<String, Object> eanItem = new HashMap<>(item);
            ImportResult eanResult = findInCatalogByEan(eanItem, solrClient, itemsToSkip);

            Map<String, Object> titlesItem = new HashMap<>(item);
            ImportResult titleResult = findInCatalogByTitle(titlesItem, solrClient, itemsToSkip, this.match1, this.match21, this.match22);

            if (eanResult.found()) {
                SolrInputDocument idoc = new SolrInputDocument();
                String item_id = eanItem.get("EAN") + "_" + this.importDescription.getImportOrigin();
                idoc.setField("import_id", this.importDescription.getImportId());
                idoc.setField("import_date", this.importDescription.getImportDate());
                idoc.setField("item_id", item_id);

                idoc.setField("id", this.importDescription.getImportId() + "_" + eanItem.get("EAN"));
                idoc.setField("ean", eanItem.get("EAN"));
                idoc.setField("name", eanItem.get("NAME"));
                if (eanItem.containsKey("AUTHOR")) {
                    idoc.setField("author", eanItem.get("AUTHOR"));
                }
                if (!eanItem.containsKey("URL")) {
                    eanItem.put("URL", "https://www.distri.cz/Search/Result/?Text=" + eanItem.get("EAN"));
                }

                addDedup(eanItem);
                addFrbr(eanItem);

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

            } else if (titleResult.found()) {

                SolrInputDocument idoc = new SolrInputDocument();
                String item_id = titlesItem.get("EAN") + "_" + this.importDescription.getImportOrigin();
                idoc.setField("import_id", this.importDescription.getImportId());
                idoc.setField("import_date", this.importDescription.getImportDate());
                idoc.setField("item_id", item_id);

                idoc.setField("id", this.importDescription.getImportId() + "_" + titlesItem.get("EAN"));
                idoc.setField("ean", titlesItem.get("EAN"));
                idoc.setField("name", titlesItem.get("NAME"));
                if (titlesItem.containsKey("AUTHOR")) {
                    idoc.setField("author", titlesItem.get("AUTHOR"));
                }
                if (!titlesItem.containsKey("URL")) {
                    titlesItem.put("URL", "https://www.distri.cz/Search/Result/?Text=" + titlesItem.get("EAN"));
                }

                addDedup(titlesItem);
                addFrbr(titlesItem);

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


    //float match1Prec
    public ImportResult findInCatalogByTitle(Map item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip, float match1Prec, float match21Prec, float match22Prec) {
        try {
            // EAN - Musi mit ean
            String q = "ean:\"" + item.get("EAN")+"\"";
            SolrQuery eanQuery = new SolrQuery(q).setRows(100).setParam("q.op", "AND")
                    .setFields("identifier");

            List<JSONObject>eanItems = fetchDocsFromSolr(solrClient, eanQuery, doc->{return true;});
            if (eanItems.size() > 0) {
                final String distriTitle = (String) item.get("NAME");
                final String distriSubtitle = (String) item.get("SUBTITLE");
                final String distriAuthor = item.containsKey("AUTHOR")  ? (String) item.get("AUTHOR") : "";
                final String distriNakladatel = item.containsKey("PUBLISHING") ? (String)item.get("PUBLISHING") : "";

                String title = "nazev:(" + ClientUtils.escapeQueryChars(((String) item.get("NAME")).trim()) + ")";
                if (item.containsKey("AUTHOR") && !((String) item.get("AUTHOR")).isBlank()) {
                    title += " AND author:(" + ClientUtils.escapeQueryChars((String) item.get("AUTHOR")) + ")";
                }
                SolrQuery query = new SolrQuery(title).setRows(DEFAULT_NUMBER_HITS_BY_TITLE).setParam("q.op", "AND")
                        .setFields(
                                "identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,marc_245a,marc_245b,datum_kurator_stav, author");
                List<String> catalog =  super.findCatalogItem(item, solrClient ,query, "noean", itemsToSkip, (doc-> {

                    // match1 - title, subtitle, author
                    boolean matched = match_1(doc, distriTitle, distriSubtitle, distriAuthor, match1Prec);
                    if (matched)  {
                        return matched;
                    }

                    matched = match_2(doc, distriTitle, distriSubtitle, distriNakladatel, match21Prec, match22Prec);
                    if (matched)  {
                        return matched;
                    }

                    return false;
                }));
                return new ImportResult(catalog, item, eanItems.get(0).getString(MarcRecordFields.IDENTIFIER_FIELD));
            }  else {
                return new ImportResult(new ArrayList<>(), item, null);
            }
        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ImportResult(new ArrayList<>(), item, null);
        }
    }


    public ImportResult findInCatalogByEan(Map<String,Object> item, SolrClient solrClient, LinkedHashSet<String> itemsToSkip) {
        try {
            String q = "ean:\"" + item.get("EAN")+"\"";
            SolrQuery query = new SolrQuery(q).setRows(100).setParam("q.op", "AND")
                    .setFields(
                            "identifier,nazev,score,ean,dntstav,rokvydani,license,kuratorstav,granularity:[json],marc_998a,datum_kurator_stav");
            List<String> catalogItem = super.findCatalogItem(item, solrClient, query, "ean", itemsToSkip, doc -> {
                return true;
            });

            return new ImportResult(catalogItem,item, catalogItem.size() > 0 ? catalogItem.get(0) : null);

        } catch (SolrServerException | IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
            return new ImportResult(new ArrayList<>(),item, null);
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
                1, "days", 1.0f, 1.0f, 0.5f);
        distri.doImport(null, false, new LinkedHashSet<>());
        XMLImportDesc importDesc = distri.getImportDesc();
        System.out.println(importDesc.toString());
        
    }
    
}
