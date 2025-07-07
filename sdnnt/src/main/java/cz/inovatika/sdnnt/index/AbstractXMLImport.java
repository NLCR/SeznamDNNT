package cz.inovatika.sdnnt.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import cz.inovatika.sdnnt.index.utils.imports.AuthorUtils;
import cz.inovatika.sdnnt.index.utils.imports.PublisherInfoCleaner;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.utils.imports.ImporterUtils;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.LoggerAware;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public abstract class AbstractXMLImport implements LoggerAware {

    public static Logger LOGGER = Logger.getLogger(AbstractXMLImport.class.getName());


    public static final String DEFAULT_CHRONO_UNIT = "month";
    public static final int DEFAULT_VALUE = 6;
    protected static final int DEFAULT_NUMBER_HITS_BY_TITLE = 100;
    protected Logger logger = Logger.getLogger(XMLImporterDistri.class.getName());
    protected String url;
    protected int fromId = -1;
 
    protected int checkPNStates = AbstractXMLImport.DEFAULT_VALUE;
    protected String chronoUnit = AbstractXMLImport.DEFAULT_CHRONO_UNIT;

    protected float match1 = 1.0f;
    protected float match21 = 1.0f;
    protected float match22 = 0.5f;

    
    public AbstractXMLImport(String strLogger,  String url, int checkPNStates, String chronoUnit, float match1, float match21, float match22) {
        if (strLogger != null) {
            this.logger = Logger.getLogger(strLogger);
        }
        if (url != null) {
            this.url = url;
        }
        if (checkPNStates > -1) {
            this.checkPNStates = checkPNStates;
            this.chronoUnit = chronoUnit;
        }
        this.match1 = match1;
        this.match21 = match21;
        this.match22 = match22;
    }


    protected static String normalizeObjects(Object ... objs) {
        StringBuilder valueBuilder = new StringBuilder();
        Arrays.stream(objs).forEach(obj-> {
            if (obj != null) {
                if (obj instanceof  JSONArray) {
                    JSONArray arr = (JSONArray) obj;
                    for (int i = 0; i < arr.length(); i++) {
                        String str = arr.getString(i);
                        valueBuilder.append(str);
                    }
                } else {
                    valueBuilder.append(obj.toString());
                }
            }
        });
        return ImporterUtils.normalize(valueBuilder.toString());
    }


    protected SolrDocument isControlled(String id, SolrClient solr) throws SolrServerException, IOException {
        int days = Options.getInstance().getInt("importControlledExpireationDays", 30);
        SolrQuery q = new SolrQuery("*").addFilterQuery("item_id:" + id)
                .addFilterQuery("controlled_date:[NOW/DAY-" + days + "DAYS TO NOW]").addFilterQuery("controlled:true");
        SolrDocumentList docs = solr.query("imports_documents", q).getResults();
        if (docs.getNumFound() > 0) {
            return docs.get(0);
        }
        return null;
    }


    protected List<JSONObject> fetchDocsFromSolr(SolrClient solrClient, SolrQuery query, Predicate<JSONObject> filter) throws SolrServerException, IOException {
        QueryRequest qreq = new QueryRequest(query);
        NoOpResponseParser rParser = new NoOpResponseParser();
        rParser.setWriterType("json");
        qreq.setResponseParser(rParser);

        NamedList<Object> qresp = solrClient.request(qreq, "catalog");
        JSONObject jresp = new JSONObject((String) qresp.get("response")).getJSONObject("response");
        JSONArray docs = jresp.getJSONArray("docs");

        List<JSONObject> filteredDocs = new ArrayList<>();
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            if (filter == null ||  (filter != null && filter.test(doc))) {
                filteredDocs.add(doc);
            }
        }

        return filteredDocs;
    }

    protected List<String> findCatalogItem(Map<String, Object> item, SolrClient solrClient, SolrQuery query, String hitType, LinkedHashSet<String> itemsToSkip, Predicate<JSONObject> filter) throws SolrServerException, IOException {
        List<JSONObject>docs = fetchDocsFromSolr(solrClient, query, filter);

        /** all identifiers */
        List<String> identifiers = new ArrayList<>();
        /** pn identifiers */
        List<String> pnIdetifiers = new ArrayList<>();
        /** na vyrazeni */
        List<String> naVyrazeni = new ArrayList<>();

        if (docs.size() == 0) {
            return new ArrayList<>();
        }
        
        List<String> states = new ArrayList<>();
        for (Object o : docs) {
            // tady  bereme vsechny ale muze to byt tak, ze bereme jenom nejake
            JSONObject doc = (JSONObject) o;
            String identifier = doc.getString("identifier");

            if (doc.has("dntstav")) {
                states.addAll(doc.getJSONArray("dntstav").toList().stream().map(Object::toString).collect(Collectors.toList()));

                List<Object> publicstate = doc.getJSONArray(MarcRecordFields.DNTSTAV_FIELD).toList();
                List<Object> kuratorstate = doc.getJSONArray(MarcRecordFields.KURATORSTAV_FIELD).toList();
                String datumKuratorStav = doc.optString(MarcRecordFields.DATUM_KURATOR_STAV_FIELD);

                // Nebude fungovat NL -> Musim vyradit
                if (publicstate.contains("A") || publicstate.contains("PA") /*|| publicstate.contains("NL")*/) {
                    if (!kuratorstate.contains(CuratorItemState.PN.name())) {
                        if (!itemsToSkip.contains(identifier)) {
                            getImportDesc().incrementInSdnnt();
                            naVyrazeni.add(identifier);
                        } else {
                            getImportDesc().incrementSkipped();
                        }
                    } else {
                        if (datumKuratorStav != null) {
                            Instant parsedInstant = Instant.parse(datumKuratorStav);
                            ChronoUnit selected = ChronoUnit.DAYS;
                            ChronoUnit[] values = ChronoUnit.values();
                            for (ChronoUnit chUnit : values) {
                                if (chUnit.name().toLowerCase().equals(this.chronoUnit)) {
                                    selected = chUnit;
                                    break;
                                }
                            }

                            if (ImporterUtils.calculateInterval(parsedInstant, Instant.now(), selected) > this.checkPNStates) {
                                if (!itemsToSkip.contains(identifier)) {
                                    getImportDesc().incrementInSdnnt();
                                    naVyrazeni.add(identifier);
                                } else {
                                    getImportDesc().incrementSkipped();
                                    logger.info(String.format("Skipping PN state %s", identifier));
                                    pnIdetifiers.add(identifier);
                                }
                            } else {
                                getImportDesc().incrementSkipped();
                                logger.info(String.format("Skipping PN state %s", identifier));
                                pnIdetifiers.add(identifier);
                                getLogger().info(String.format("New PN state %s, skipping",  doc.getString("identifier")));
                            }
                        } else {
                            pnIdetifiers.add(identifier);
                            getLogger().info("No " + MarcRecordFields.DATUM_KURATOR_STAV_FIELD + " field");
                        }
                    }

                }
            }
            
            if (!pnIdetifiers.contains(identifier) && !itemsToSkip.contains(identifier)) {
                doc.remove("granularity");
                doc.remove("raw");
                identifiers.add(doc.toString());
            }
       }

        
        JSONArray dntStavArray = new JSONArray();
        states.stream().filter(state -> Arrays.asList("A", "PA", "NL").contains(state)).forEach(dntStavArray::put);
        if (naVyrazeni.size() > 0)  item.put("dntstav",dntStavArray);
        getImportDesc().addToDiscardingHits(naVyrazeni.size());

        item.put("found", true);
        //item.put("hit_type", isEAN ? "ean" : "noean");
        item.put("hit_type", hitType);
        item.put("num_hits",identifiers.size());
        item.put("identifiers", identifiers);
        item.put("na_vyrazeni", naVyrazeni);
        item.put("hits_na_vyrazeni", naVyrazeni.size());

        return naVyrazeni;
    }


    protected int getLastId(SolrClient solrClient) {
        try {
            int last = -1;
            SolrQuery q = new SolrQuery("*").setRows(1).addFilterQuery("origin:" + getImportDesc().getImportOrigin())
                    .setFields(ImporterUtils.IMPORT_LASTID_KEY).setSort("indextime", SolrQuery.ORDER.desc);
            SolrDocumentList docs = solrClient.query("imports", q).getResults();
            if (docs.getNumFound() > 0) {
                last = Integer.parseInt((String) docs.get(0).getFirstValue("last_id")) + 1;
            }
            return last;
        } catch (NumberFormatException | SolrServerException | IOException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            return -1;
        }
    }

    
    protected void indexImportSummary(SolrClient solrClient) throws SolrServerException, IOException {
        XMLImportDesc importDesc = getImportDesc();
        solrClient.add(DataCollections.imports.name(), getImportDesc().toSolrInputDocument());
        solrClient.commit(DataCollections.imports.name());
    }

    public String getUrl() {
        return this.url;
    }

    /** title + subtitle + author = 90% match */
    protected static boolean match_1(JSONObject doc, String distriTitle, String distriSubtitle, String distriAuthor, float match1Prec) {
        StringBuilder authorStringBuilder = new StringBuilder();
        JSONArray authorArray = doc.optJSONArray("author");
        if (authorArray != null) {
            for (int i = 0; i < authorArray.length(); i++) {
                String optString = authorArray.optString(i);
                authorStringBuilder.append(AuthorUtils.normalizeAndSortAuthorName(optString).stream().collect(Collectors.joining(" ")));
            }
        }

        String catalogNormalizedTitle = normalizeObjects(doc.optJSONArray("marc_245a"), doc.optJSONArray("marc_245b"), authorStringBuilder.toString());
        String distriNormalizedTitle = normalizeObjects(distriTitle, distriSubtitle, AuthorUtils.normalizeAndSortAuthorName(distriAuthor));
        //Options.getInstance().getFloat()
        boolean matched = ImporterUtils.similartyMatch(catalogNormalizedTitle, distriNormalizedTitle, match1Prec);
        return matched;
    }

    /** title + subtitle  = 100% match, publisher = 50 % match */
    protected static boolean match_2(JSONObject doc, String distriTitle, String distriSubtitle, String publisher, float match21Prec, float match22Prec) {
        String catalogNormalizedTitle = normalizeObjects(doc.optJSONArray("marc_245a"), doc.optJSONArray("marc_245b"));
        String distriNormalizedTitle = normalizeObjects(distriTitle, distriSubtitle);
        boolean matchedTitle = ImporterUtils.similartyMatch(catalogNormalizedTitle, distriNormalizedTitle, match21Prec);
        if (matchedTitle) {

            StringBuilder catalogRAWNakladatel = new StringBuilder();
            JSONArray catalogNakladatelJSONArray = doc.optJSONArray("nakladatel");
            if (catalogNakladatelJSONArray != null) {
                for (int i = 0; i < catalogNakladatelJSONArray.length(); i++) {  catalogRAWNakladatel.append(catalogNakladatelJSONArray.getString(i)); }
            }

            String catalogNakladatel = normalizeObjects(PublisherInfoCleaner.normalizePublisher( catalogRAWNakladatel.toString() ));
            String distriNakladatel = normalizeObjects(PublisherInfoCleaner.normalizePublisher( publisher));
            boolean matchedNakladatel =  ImporterUtils.similartyMatch(catalogNakladatel, distriNakladatel, match22Prec);
            return matchedNakladatel;
        }
        return false;
    }

    public abstract XMLImportDesc getImportDesc();
    public abstract String getImportIdentifier();

    // Process stream
    // change PN states
    public abstract LinkedHashSet<String> processFromStream(
            String uri, 
            InputStream is, 
            String fromId, 
            SolrClient solrClient, 
            LinkedHashSet<String> identifiers) throws XMLStreamException, SolrServerException, IOException;
    
    
    
    
    
    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    
    public LinkedHashSet<String> doImport(String fromId, boolean resume, LinkedHashSet<String> skipIdentifiers) {
        try (final SolrClient solrClient = buildClient()) {
            JSONObject ret = new JSONObject();
            LinkedHashSet<String> skipped = new LinkedHashSet<>();
            long start = new Date().getTime();
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            getImportDesc().setImportDate(now.format(DateTimeFormatter.ISO_INSTANT));
            getImportDesc().setImportUrl(getUrl());
            getImportDesc().setImportId(now.toEpochSecond() + "");
            if (resume) {
                this.fromId = getLastId(solrClient);
            } else if (fromId != null) {
                this.fromId = Integer.parseInt(fromId);
            }
            if (getUrl().startsWith("http")) {
                Path p = ImporterUtils.download(getUrl());
                getLogger().info(String.format("Path is %s", p.toFile().getAbsolutePath()));
                try (InputStream is = new FileInputStream(p.toFile())) {
                    skipped = processFromStream(getUrl(), is, fromId, solrClient, skipIdentifiers);
                } catch (Exception ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                }
            } else {
                URL url = new URL(getUrl());
                try (InputStream is = url.openStream()) {
                    skipped = processFromStream(getUrl(), is, fromId, solrClient,skipIdentifiers);
                } catch (Exception ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                    ret.put("error", ex);
                }
            }
    
            LinkedHashSet<String> linked = new LinkedHashSet<>();
            linked.addAll(skipIdentifiers);
            linked.addAll(skipped);
            
            return linked;
            
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            return new LinkedHashSet<>();
        }
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    public static class XMLImportDesc {

        private String importOrigin;
        private String importId;
        private String importDate;
        private String importUrl;
        private int inSdnnt;
        private String firstId;
        private String lastId;

        private int discardingHits = 0;
        private int total = 0;
        private int skipped = 0;
        private int indexed = 0;

        private String group;
        
        
        public XMLImportDesc(String importOrigin, String group) {
            super();
            this.importOrigin = importOrigin;
            this.group = group;
        }

        public String getImportOrigin() {
            return importOrigin;
        }

        public String getImportId() {
            return importId;
        }

        public void setImportId(String importId) {
            this.importId = importId;
        }

        public String getImportDate() {
            return importDate;
        }

        public void setImportDate(String importDate) {
            this.importDate = importDate;
        }

        public String getImportUrl() {
            return importUrl;
        }

        public void setImportUrl(String importUrl) {
            this.importUrl = importUrl;
        }

        public int getInSdnnt() {
            return inSdnnt;
        }

        public void setInSdnnt(int inSdnnt) {
            this.inSdnnt = inSdnnt;
        }

        public int incrementInSdnnt() {
            return ++this.inSdnnt;
        }

        public String getFirstId() {
            return firstId;
        }

        public void setFirstId(String firstId) {
            this.firstId = firstId;
        }

        public String getLastId() {
            return lastId;
        }

        public void setLastId(String lastId) {
            this.lastId = lastId;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int incrementTotal() {
            return ++this.total;
        }

        public int getSkipped() {
            return skipped;
        }

        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }

        public int incrementSkipped() {
            return ++this.skipped;
        }

        public int getIndexed() {
            return indexed;
        }

        public void setIndexed(int indexed) {
            this.indexed = indexed;
        }

        public int incrementIndexed() {
            return ++this.indexed;
        }

        public void setImportOrigin(String importOrigin) {
            this.importOrigin = importOrigin;
        }


        public void addToDiscardingHits(int discardingHits) {
            this.discardingHits = this.discardingHits + discardingHits;
        }

        public void removeFromDiscardingHits(int discardingHits) {
            this.discardingHits = this.discardingHits - discardingHits;
        }

        public String getGroup() {
            return group;
        }

        public boolean indexable() {
            return this.inSdnnt > 0 || this.indexed > 0;
        }


        @Override
        public String toString() {
            return "XMLImportDesc [importOrigin=" + importOrigin + ", importId=" + importId + ", importDate="
                    + importDate + ", importUrl=" + importUrl + ", inSdnnt=" + inSdnnt + ", firstId=" + firstId
                    + ", lastId=" + lastId + ", total=" + total + ", skipped=" + skipped + ", indexed=" + indexed
                    + ", group=" + group + "]";
        }

        public SolrInputDocument toSolrInputDocument() {
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField(ImporterUtils.IMPORT_ID_KEY, importId);
            idoc.setField(ImporterUtils.IMPORT_DATE_KEY, importDate);
            idoc.setField(ImporterUtils.IMPORT_URL_KEY, importUrl);
            idoc.setField(ImporterUtils.IMPORT_ORIGIN_KEY, this.importOrigin);
            idoc.setField(ImporterUtils.IMPORT_FIRSTID_KEY, firstId);
            idoc.setField(ImporterUtils.IMPORT_LASTID_KEY, lastId);
            idoc.setField(ImporterUtils.IMPORT_PROCESSED_KEY, false);
            idoc.setField(ImporterUtils.IMPORT_NUMITEMS_KEY, total);
            idoc.setField(ImporterUtils.IMPORT_NUMDOCS_KEY, indexed);
            idoc.setField(ImporterUtils.IMPORT_SKIPPED_KEY, skipped);
            idoc.setField(ImporterUtils.IMPORT_NUMINSDNNT_KEY, inSdnnt);
            //idoc.setField(ImporterUtils.IMPORT_NUM_CANCELING_HITS, this.discardingHits);
            idoc.setField(ImporterUtils.IMPORT_GROUP_KEY, this.group);
            return idoc;
        }
    }

}
