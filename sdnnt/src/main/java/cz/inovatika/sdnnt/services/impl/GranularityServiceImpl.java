package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.GRANULARITY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_856_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_911_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_956_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SIGLA_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.acl.Owner;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Streams;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateSKCUtils;
import cz.inovatika.sdnnt.services.GranularityService;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.impl.kramerius.LinksOnwer;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.Granularity;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.GranularityField;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.GranularityField.TypeOfRec;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.rules.Marc911Rule;
import cz.inovatika.sdnnt.services.impl.kramerius.infos.MasterLinkItem;
import cz.inovatika.sdnnt.services.impl.kramerius.infos.MasterLinks;
import cz.inovatika.sdnnt.services.impl.utils.SKCYearsUtils;
import cz.inovatika.sdnnt.services.impl.utils.SolrYearsUtils;
import cz.inovatika.sdnnt.services.impl.zahorikutils.ZahorikUtils;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration.KramVersion;
import cz.inovatika.sdnnt.utils.KrameriusFields;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.PIDUtils;
import cz.inovatika.sdnnt.utils.SimpleGET;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class GranularityServiceImpl extends AbstractGranularityService implements GranularityService {


    public static Logger LOGGER = Logger.getLogger(GranularityServiceImpl.class.getName());

    private static final int MAX_FETCHED_DOCS = 1000;
    public static final int CHECK_SIZE = 120;

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private Logger logger = Logger.getLogger(GranularityService.class.getName());

    /**
     * Catalog identifier <=> granularities
     */
    // private Map<String, Granularity> granularities = new HashMap<>(50000);
    private Map<String, LinksOnwer> linksOwner = new HashMap<>(50000);

    /** Changed catalog identifiers */

    private Set<String> changedIdentifiers = new LinkedHashSet<>();
    public CheckKrameriusConfiguration checkConf = null;

    public GranularityServiceImpl(String logger) {
        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        } else {
            this.logger = LOGGER;
        }
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    public void initialize() {
        JSONObject checkKamerius = getOptions().getJSONObject("check_kramerius");
        this.checkConf = CheckKrameriusConfiguration.initConfiguration(checkKamerius);
    }

    public String domain(String link) {

        InstanceConfiguration configuration = checkConf.match(link);
        if (configuration != null) {
            return configuration.getDomain();
        } else {
            String pLink = link;
            if (pLink.startsWith("https")) {
                pLink = pLink.substring("https://".length());
            }
            if (pLink.startsWith("http")) {
                pLink = pLink.substring("http://".length());
            }
            if (pLink.indexOf("/") > 0) {
                String domainSubString = pLink.substring(0, pLink.indexOf("/"));
                String[] splitted = domainSubString.split("\\.");
                if (splitted.length > 2) {
                    // digitalni knihovna
                    return splitted[splitted.length - 2] + "." + splitted[splitted.length - 1];
                } else {
                    return domainSubString;
                }
            } else
                return null;
        }
    }

    @Override
    public void refershGranularity() throws IOException {

        try (final SolrClient solrClient = buildClient()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "10000");

            AtomicInteger counter = new AtomicInteger();

            CatalogIterationSupport support = new CatalogIterationSupport();
            
            List<String> plusFilter = Arrays.asList("(marc_911u:* OR marc_856u:* OR granularity:*)", KURATORSTAV_FIELD + ":*",
                    DNTSTAV_FIELD + ":*"
            );

            
            List<String> minusFilter = Arrays.asList(KURATORSTAV_FIELD + ":D", KURATORSTAV_FIELD + ":DX");

            AtomicInteger count = new AtomicInteger();
            support.iterate(solrClient, reqMap, null, plusFilter, minusFilter,
                    Arrays.asList(IDENTIFIER_FIELD, DNTSTAV_FIELD, SIGLA_FIELD, MARC_911_U, MARC_956_U, MARC_856_U,
                            GRANULARITY_FIELD, "marc_911r", MarcRecordFields.FMT_FIELD, "controlfield_008",
                            MarcRecordFields.LEADER_FIELD, MarcRecordFields.RAW_FIELD

                    ), (rsp) -> {

                        int addAndGet = counter.addAndGet(1);
                        if (addAndGet % 10000 == 0) {
                            getLogger().info(" Counter is " + addAndGet);
                        }

                        Object identifier = rsp.getFieldValue("identifier");
                        
                        Object fmt = rsp.getFieldValue(MarcRecordFields.FMT_FIELD);
                        Object leader = rsp.getFieldValue(MarcRecordFields.LEADER_FIELD);

                        Collection controlFields = (Collection) rsp.getFieldValue("controlfield_008");

                        List dntstav = (List) rsp.getFieldValue(DNTSTAV_FIELD);
                        Collection<Object> links1 = rsp.getFieldValues(MARC_911_U);
                        Collection<Object> links3 = rsp.getFieldValues(MARC_856_U);

                        List<String> granularity = (List<String>) rsp.getFieldValue(GRANULARITY_FIELD);

                        List<String> granularityLinks = new ArrayList<>();
                        if (granularity != null) {
                            Granularity granObject = new Granularity(identifier.toString(),
                                    dntstav != null ? PublicItemState.valueOf(dntstav.get(0).toString()) : null);
                            granularity.stream().forEach(it -> {
                                JSONObject jObj = new JSONObject(it);
                                GranularityField gf = GranularityField.initFromSDNNTSolrJson(jObj, this.checkConf);
                                gf.setTypeOfRec(TypeOfRec.SDNNT_SOLR);
                                if (it.contains("rocnik")) {
                                    String link = jObj.optString("link");
                                    if (link != null) {
                                        granularityLinks.add(link);
                                    }
                                }
                                granObject.addGranularityField(gf);
                            });

                            LinksOnwer onwer = new LinksOnwer(identifier.toString());
                            onwer.setGranularity(granObject);
                            onwer.setFmt(fmt.toString());
                            onwer.setLeader(leader.toString());
                            onwer.setControl008(
                                    controlFields.size() > 0 ? controlFields.iterator().next().toString() : null);

                            this.linksOwner.put(identifier.toString(), onwer);

                        } else {
                            if (links1 != null || links3 != null) {
                                Granularity granObject = new Granularity(identifier.toString(),
                                        dntstav != null ? PublicItemState.valueOf(dntstav.get(0).toString()) : null);
                                LinksOnwer onwer = new LinksOnwer(identifier.toString());
                                onwer.setFmt(fmt.toString());
                                onwer.setLeader(leader.toString());
                                onwer.setControl008(
                                        controlFields.size() > 0 ? controlFields.iterator().next().toString() : null);

                                onwer.setGranularity(granObject);

                                this.linksOwner.put(identifier.toString(), onwer);
                            }
                        }

                        if (links1 != null && !links1.isEmpty()) {

                            Collection<Object> yearsColl = rsp.getFieldValues("marc_911r");
                            if (yearsColl != null) {
                                
                                
                                List<Triple<String,String, String>> siglaYearsMapping = new ArrayList<>();
                                
                                // check against raw field 
                                Object rawField = rsp.getFieldValue(MarcRecordFields.RAW_FIELD);
                                JSONObject rawFieldJSON = new JSONObject(rawField.toString());
                                if (rawFieldJSON.has("dataFields")) {
                                    if (rawFieldJSON.getJSONObject("dataFields").has("911")) {
                                        JSONArray field911 = rawFieldJSON.getJSONObject("dataFields").getJSONArray("911");
                                        for(int i= 0;i<field911.length();i++) {
                                            if (field911.getJSONObject(i).has("subFields")) {
                                                JSONObject subFields = field911.getJSONObject(i).getJSONObject("subFields");
                                                if (subFields.has("a") && subFields.has("r") && subFields.has("u")) {

                                                    JSONArray a = subFields.getJSONArray("a");
                                                    String aVal = a.getJSONObject(0).optString("value");
                                                    
                                                    JSONArray r = subFields.getJSONArray("r");
                                                    String rVal = r.getJSONObject(0).optString("value");

                                                    JSONArray u = subFields.getJSONArray("u");
                                                    String uVal = u.getJSONObject(0).optString("value");

                                                    if (aVal != null && rVal != null && uVal!= null) {
                                                        siglaYearsMapping.add(Triple.of(aVal, rVal, uVal));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (siglaYearsMapping.size() > 0) {
                                    LOGGER.info("Sigla years mapping :"+siglaYearsMapping);
                                    for(int i= 0;i<siglaYearsMapping.size();i++) {
                                        Triple<String, String, String> triple = siglaYearsMapping.get(i);
                                        InstanceConfiguration instanceConfiguration = checkConf.findBySigla(triple.getLeft());
                                        if (instanceConfiguration != null) {

                                            Marc911Rule rule = new Marc911Rule(triple.getMiddle(), triple.getRight(), instanceConfiguration.getAcronym());

                                            if (this.linksOwner.containsKey(identifier.toString())) {
                                                this.linksOwner.get(identifier.toString()).getGranularity().addTitleRule(rule);
                                            }
                                        }
                                    }
                                }

                                
                                /*
                                List<String> years = yearsColl.stream().map(Objects::toString)
                                        .collect(Collectors.toList());
                                List<String> lls = links1.stream().map(Objects::toString).collect(Collectors.toList());
                                for (int i = 0; i < Math.min(years.size(), links1.size()); i++) {
                                    String url = lls.get(i);
                                    InstanceConfiguration instance = this.checkConf.match(url);
                                    if (instance != null) {
                                        Marc911Rule rule = new Marc911Rule(years.get(i), url, instance.getAcronym());
                                        if (this.linksOwner.containsKey(identifier.toString())) {
                                            this.linksOwner.get(identifier.toString()).getGranularity()
                                                    .addTitleRule(rule);
                                        }
                                    }
                                }*/
                            }

                            List<String> ll = links1.stream().map(Object::toString).collect(Collectors.toList());

                            if (this.linksOwner.containsKey(identifier.toString())) {
                                ll.stream().forEach(l -> {
                                    this.linksOwner.get(identifier.toString()).addTitleUrl(l);
                                });

                            }

                        }

                        if (links3 != null && !links3.isEmpty()) {

                            List<String> ll = links3.stream().map(Object::toString).collect(Collectors.toList());
                            if (linksOwner.containsKey(identifier.toString())) {
                                ll.stream().forEach(l -> {
                                    this.linksOwner.get(identifier.toString()).addTitleUrl(l);
                                });
                            }
                        }

                    }, IDENTIFIER_FIELD);
        }

        // mapuje id na seznam titulu v knihovnach
        logger.info("Found candidates: " + this.linksOwner.size());
        List<String> allPids = new ArrayList<>();
        for (String key : this.linksOwner.keySet()) {
            LinksOnwer linksOnwer = this.linksOwner.get(key);
            List<String> links = linksOnwer.getTitleUrls();
            for (String link : links) {
                String pid = PIDUtils.pid(link);
                if (pid != null) {
                    allPids.add(pid);
                }
            }
        }
        logger.info("Found all checking pids : " + allPids.size());

        AtomicInteger iteration = new AtomicInteger();
        Map<String, List<Pair<String, String>>> buffer = new HashMap<>();
        // SOLR changes
        List<SolrInputDocument> changes = new ArrayList<>();
        for (String key : this.linksOwner.keySet()) {

            int counter = iteration.incrementAndGet();
            if ((counter % 10000) == 0) {
                getLogger().info("Counter: " + counter);
            }
            // identifikator - url
            LinksOnwer linksOnwer = this.linksOwner.get(key);
            Granularity granularity = linksOnwer.getGranularity();
            MasterLinks masterLinks = linksOnwer.getMasterLinks();

            List<String> links = linksOnwer.getTitleUrls();
            boolean fullSkip = true;
            for (String link : links) {
                String pid = PIDUtils.pid(link);
                if (pid != null) {
                    String baseUrl = checkConf.baseUrl(link);
                    InstanceConfiguration configuration = checkConf.match(baseUrl);
                    if (configuration != null && !configuration.isShouldSkip()) {
                        fullSkip = false;
                        if (!buffer.containsKey(baseUrl)) {
                            buffer.put(baseUrl, new ArrayList<>());
                        }
                        getLogger().fine("baseurl " + baseUrl + " pair " + Pair.of(key, pid));
                        buffer.get(baseUrl).add(Pair.of(key, pid));

                        checkBuffer(buffer);
                    } else {
                        getLogger().info("Skipping instance '" + configuration + "'");
                    }
                }
            }

            if (fullSkip) {
                if (granularity != null) {
                    SolrInputDocument romeSolrDocument = granularity.toRomeSolrDocument();
                    changes.add(romeSolrDocument);
                    linksOnwer.setGranularity(null);
                }
                if (masterLinks != null) {
                    SolrInputDocument romeSolrDocument = masterLinks.toRomeSolrDocument();
                    changes.add(romeSolrDocument);
                    linksOnwer.setMasterLinks(null);
                }
            }
        }

        if (!buffer.isEmpty()) {
            clearBufferItem(buffer);
            clearBufferChildren(buffer);
            detectGranularityItems(buffer);
            buffer.clear();
        }

        this.linksOwner.keySet().forEach(key -> {
            this.changedIdentifiers.add(key);
            
            
            Granularity granularity = this.linksOwner.get(key).getGranularity();
            if (granularity != null) {

                granularity.validation(this.checkConf);
                granularity.merge(this.checkConf);
                
                SolrInputDocument granularitySolrDoc = granularity.toSolrDocument();
                if (granularitySolrDoc != null) {
                    changes.add(granularitySolrDoc);
                }
                
                MasterLinks mlinks = this.linksOwner.get(key).getMasterLinks();
                if (mlinks != null) {
                    List<SolrInputDocument> sDocs = mlinks.toSolrDocument();
                    if (sDocs != null) {
                        sDocs.forEach(changes::add);
                    }
                }
            } else {
                // pokud nema granularitu, zapiseme alespon masterlinks
                MasterLinks mlinks = this.linksOwner.get(key).getMasterLinks();
                if (mlinks != null) {
                    List<SolrInputDocument> sDocs = mlinks.toSolrDocument();
                    if (sDocs != null) {
                        sDocs.forEach(changes::add);
                    }
                }
                
            }
        });

        try (final SolrClient solr = buildClient()) {
            int setsize = changes.size();
            int batchsize = 10000;
            int numberofiteration = changes.size() / batchsize;
            if (setsize % batchsize != 0)
                numberofiteration = numberofiteration + 1;
            for (int i = 0; i < numberofiteration; i++) {
                int from = i * batchsize;
                int to = Math.min((i + 1) * batchsize, setsize);
                List<SolrInputDocument> batchDocs = changes.subList(from, to);
                getLogger().info(String.format("Updating records %d - %d and size %d", from, to, batchDocs.size()));

                UpdateRequest req = new UpdateRequest();
                for (SolrInputDocument bDoc : batchDocs) {
                    req.add(bDoc);
                }

                try {
                    UpdateResponse response = req.process(solr, DataCollections.catalog.name());
                    getLogger().info("qtime:" + response.getQTime());
                } catch (SolrServerException | IOException e) {
                    e.printStackTrace();
                    getLogger().log(Level.SEVERE, e.getMessage());
                }
            }

            SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
        }
        logger.info("Refreshing finished. Updated identifiers " + this.changedIdentifiers);
    }

    protected void checkBuffer(Map<String, List<Pair<String, String>>> buffer) {
        Integer sum = buffer.values().stream().map(List::size).reduce(0, Integer::sum);
        if (sum > CHECK_SIZE) {
            long start = System.currentTimeMillis();
            clearBufferItem(buffer);
            clearBufferChildren(buffer);
            getLogger().fine("info took (" + (System.currentTimeMillis() - start) + ") ms ");
            detectGranularityItems(buffer);

            buffer.clear();
        }
    }

    private void detectGranularityItems(Map<String, List<Pair<String, String>>> buffer) {
        buffer.keySet().forEach(key -> {
            List<Pair<String, String>> list = buffer.get(key);
            for (int i = 0, ll = list.size(); i < ll; i++) {
                Pair<String, String> pair = list.get(i);
                LinksOnwer owner = this.linksOwner.get(pair.getKey());
                if (owner.getFmt().equals("SE")) {
                    Granularity granularity = owner.getGranularity();
                    MasterLinks masterLinks = owner.getMasterLinks();
                    if (masterLinks != null) {
                        List<MasterLinkItem> links = masterLinks.getMasterLinks();

                        for (MasterLinkItem link : links) {
                            if (link.getModel().equals("monograph") || link.getModel().equals("periodicalvolume")
                                    || link.getModel().equals("periodicalitem")) {
                                GranularityField gf = granularity.findByRootPid(link.getPid());
                                if (gf == null) {
                                    owner.moveMasterLinkToGranularity(link, getLogger());
                                }
                            }
                        }
                    } else {
                        List<String> titleUrls = owner.getTitleUrls();
                        getLogger().info(
                                String.format("non-existent link %s->%s", owner.getCatalogId(), titleUrls.toString()));
                    }

                } else if (owner.getFmt().equals("BK")) {
                    char multimonograph = owner.getLeader().charAt(19);
                    if (multimonograph == 'a') {
                        Granularity granularity = owner.getGranularity();
                        MasterLinks masterLinks = owner.getMasterLinks();
                        if (masterLinks != null) {
                            // pokud rootpid je nekde
                            List<MasterLinkItem> links = masterLinks.getMasterLinks();
                            for (MasterLinkItem link : links) {
                                if (link.getModel().equals("monograph") || link.getModel().equals("monographunit")) {

                                    GranularityField gf = granularity.findByRootPid(link.getPid());
                                    if (gf == null) {
                                        owner.moveMasterLinkToGranularity(link, getLogger());
                                    }
                                }
                            }
                        } else {
                            List<String> titleUrls = owner.getTitleUrls();
                            getLogger().info(String.format("non-existent link %s->%s", owner.getCatalogId(),
                                    titleUrls.toString()));
                        }

                    }
                }
            }

        });
    }
    // master polozky 
    protected void clearBufferItem(Map<String, List<Pair<String, String>>> buffer) {
        try {
            for (String baseUrl : buffer.keySet()) {
                if (baseUrl == null) {
                    continue;
                }
                

                InstanceConfiguration configuration = this.checkConf.match(baseUrl);
                if (configuration == null || configuration.isShouldSkip()) {
                    getLogger().warning("Skipping url " + baseUrl + "'");
                    continue;
                }

                List<Pair<String, String>> pairs = buffer.get(baseUrl);
                Map<String, List<String>> pidsMapping = new HashMap<>();
                pairs.stream().forEach(p -> {
                    if (!pidsMapping.containsKey(p.getRight())) {
                        pidsMapping.put(p.getRight(), new ArrayList<>());
                    }
                    pidsMapping.get(p.getRight()).add(p.getLeft());
                });

                String condition = pairs.stream().map(Pair::getRight).filter(Objects::nonNull).map(p -> {
                    return p.replace(":", "\\:");
                }).collect(Collectors.joining(" OR "));

                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }

                if (configuration.getVersion().equals(KramVersion.V5)) {

                    String encodedCondition = URLEncoder.encode("PID:(" + condition + ")", "UTF-8");

                    String encodedFieldList = URLEncoder.encode(
                            "PID fedora.model root_pid details datum_str dostupnost details dnnt-labels pid_path datum_str datum_begin datum_end",
                            "UTF-8");
                    String url = baseUrl + "api/v5.0/search?q=" + encodedCondition + "&wt=json&rows=" + MAX_FETCHED_DOCS
                            + "&fl=" + encodedFieldList;

                    logger.fine(String.format("Kramerius url is %s and list of identifiers are %s", url, pairs.stream()
                            .map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));

                    try {
                        String result = simpleGET(url);
                        JSONObject resultJSON = new JSONObject(result);
                        JSONObject responseObject = resultJSON.getJSONObject("response");
                        int numFound = responseObject.optInt("numFound", 0);
                        if (numFound > 0) {
                            JSONArray docs = responseObject.optJSONArray("docs");
                            for (int i = 0; i < docs.length(); i++) {
                                // TODO: Move it to instance
                                JSONObject doc = docs.getJSONObject(i);
                                String rootPid = doc.optString(KrameriusFields.ROOT_PID_V5);
                                String pid = doc.optString(KrameriusFields.PID_V5);

                                List<String> identifiers = pidsMapping.get(rootPid);
                                if (identifiers == null) {
                                    identifiers = pidsMapping.get(pid);
                                }

                                MasterLinkItem mf = new MasterLinkItem();
                                mf.setModel(doc.optString(KrameriusFields.FEDORA_MODEL_V5));
                                mf.setDate(doc.optString(KrameriusFields.DATUM_STR_V5));
                                mf.setPid(doc.optString(KrameriusFields.PID_V5));
                                mf.setDostupnost(doc.optString(KrameriusFields.DOSTUPNOST_V5));

                                if (doc.has(KrameriusFields.DNNT_LABELS_V5)) {
                                    if (mf.getDostupnost() != null && !"public".equals(mf.getDostupnost())) {
                                        JSONArray dnntLabels = doc.getJSONArray(KrameriusFields.DNNT_LABELS_V5);
                                        List<String> kramLicenses = new ArrayList<>();
                                        for (int j = 0; j < dnntLabels.length(); j++) {
                                            String klicense = dnntLabels.getString(j);
                                            if (!kramLicenses.contains(klicense)) {
                                                kramLicenses.add(klicense);
                                            }
                                        }
                                        if (!kramLicenses.isEmpty()) {
                                            mf.setKramLicenses(kramLicenses);
                                        }
                                    }
                                }

                                mf.setBaseUrl(baseUrl);
                                mf.setFetched(SIMPLE_DATE_FORMAT.format(new Date()));

                                mf.setAcronym(configuration.getAcronym());

                                String link = renderLink(baseUrl, mf.getPid());
                                mf.setLink(link);


                                if (identifiers != null) {
                                    identifiers.stream().forEach(ident -> {
                                        if (linksOwner.containsKey(ident)) {
                                            LinksOnwer linksOnwer = linksOwner.get(ident);
                                            if (linksOnwer.getMasterLinks() == null) {
                                                linksOnwer.setMasterLinks(new MasterLinks(ident));
                                            }
                                            linksOnwer.getMasterLinks().addMasterLink(mf);

                                        } else {
                                            logger.log(Level.WARNING, "missing granularity for " + ident);
                                        }
                                    });
                                } else {
                                    logger.log(Level.SEVERE,
                                            String.format("Missing catalog identifier for pids %s, %s ", pid, rootPid));
                                }

                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE,
                                String.format("Error while accessing %s, error: %s ", url, e.getMessage()), e);
                    }
                } else {

                    String encodedCondition = URLEncoder.encode("pid:(" + condition + ")", "UTF-8");

                    String encodedFieldList = URLEncoder.encode(
                            "pid model root.pid date.str licenses licenses_of_ancestors own_pid_path date_range_end.year date_range_start.year titles.search",
                            "UTF-8");
                    String url = baseUrl + "api/client/v7.0/search?q=" + encodedCondition + "&wt=json&rows="
                            + MAX_FETCHED_DOCS + "&fl=" + encodedFieldList;

                    logger.fine(String.format("Kramerius url is %s and list of identifiers are %s", url, pairs.stream()
                            .map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));

                    try {
                        String result = simpleGET(url);
                        JSONObject resultJSON = new JSONObject(result);
                        JSONObject responseObject = resultJSON.getJSONObject("response");
                        int numFound = responseObject.optInt("numFound", 0);
                        if (numFound > 0) {
                            JSONArray docs = responseObject.optJSONArray("docs");
                            for (int i = 0; i < docs.length(); i++) {
                                // TODO: Move it to instance
                                JSONObject doc = docs.getJSONObject(i);
                                String rootPid = doc.optString(KrameriusFields.ROOT_PID_V7);
                                String pid = doc.optString(KrameriusFields.PID_V7);

                                List<String> identifiers = pidsMapping.get(rootPid);
                                if (identifiers == null) {
                                    identifiers = pidsMapping.get(pid);
                                }

                                MasterLinkItem mf = new MasterLinkItem();
                                mf.setModel(doc.optString(KrameriusFields.MODEL_V7));
                                mf.setDate(doc.optString(KrameriusFields.DATE_STR_V7));
                                mf.setPid(doc.optString(KrameriusFields.PID_V7));

                                List<String> licensesList = v7licenses(doc);
                                String dostupnost = "private";
                                if (licensesList.contains("public")) { dostupnost = "public"; }
                                mf.setDostupnost(dostupnost);
                                mf.setKramLicenses(licensesList);
                                mf.setBaseUrl(baseUrl);
                                mf.setFetched(SIMPLE_DATE_FORMAT.format(new Date()));
                                mf.setAcronym(configuration.getAcronym());
                                String link = renderLink(baseUrl, mf.getPid());
                                mf.setLink(link);


                                if (identifiers != null) {
                                    identifiers.stream().forEach(ident -> {
                                        if (linksOwner.containsKey(ident)) {
                                            LinksOnwer linksOnwer = linksOwner.get(ident);
                                            if (linksOnwer.getMasterLinks() == null) {
                                                linksOnwer.setMasterLinks(new MasterLinks(ident));
                                            }
                                            linksOnwer.getMasterLinks().addMasterLink(mf);

                                        } else {
                                            logger.log(Level.WARNING, "missing granularity for " + ident);
                                        }
                                    });
                                } else {
                                    logger.log(Level.SEVERE,
                                            String.format("Missing catalog identifier for pids %s, %s ", pid, rootPid));
                                }

                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE,
                                String.format("Error while accessing %s, error: %s ", url, e.getMessage()), e);
                    }
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

//    private List<String> v7licenses(JSONObject doc) {
//        JSONArray licensesJSONArray = new JSONArray();
//        if (doc.has(KrameriusFields.LICENSES_V7)) {
//            licensesJSONArray = doc.getJSONArray(KrameriusFields.LICENSES_V7);
//        } else if (doc.has(KrameriusFields.LICENSES_OF_ANCESTORS_V7)) {
//            licensesJSONArray = doc.getJSONArray(KrameriusFields.LICENSES_OF_ANCESTORS_V7);
//        }
//        List<String> licensesList = new ArrayList<>();
//        for (Object licObj : licensesJSONArray) {
//            licensesList.add(licObj.toString());
//        }
//        return licensesList;
//    }
    
    private List<String> v7licenses(JSONObject doc) {
        JSONArray licensesJSONArray = new JSONArray();
        if (doc.has(KrameriusFields.LICENSES_V7)) {
            doc.getJSONArray(KrameriusFields.LICENSES_V7).forEach(licensesJSONArray::put);
        } 
        if (doc.has(KrameriusFields.LICENSES_OF_ANCESTORS_V7)) {
            doc.getJSONArray(KrameriusFields.LICENSES_OF_ANCESTORS_V7).forEach(licensesJSONArray::put);
        }
        List<String> licensesList = new ArrayList<>();
        for (Object licObj : licensesJSONArray) {
            licensesList.add(licObj.toString());
        }
        return licensesList;
    }


    // polozky granularity
    protected void clearBufferChildren(Map<String, List<Pair<String, String>>> buffer) {
        try {
            for (String baseUrl : buffer.keySet()) {
                if (baseUrl == null) {
                    continue;
                }

                InstanceConfiguration configuration = this.checkConf.match(baseUrl);
                if (configuration == null || configuration.isShouldSkip()) {
                    getLogger().warning("Skipping url " + baseUrl + "'");
                    continue;
                }

                List<Pair<String, String>> pairs = buffer.get(baseUrl);

                Map<String, List<String>> pidsMapping = new HashMap<>();
                pairs.stream().forEach(p -> {
                    if (!pidsMapping.containsKey(p.getRight())) {
                        pidsMapping.put(p.getRight(), new ArrayList<>());
                    }
                    pidsMapping.get(p.getRight()).add(p.getLeft());
                });

                String condition = pairs.stream().map(Pair::getRight).filter(Objects::nonNull).map(p -> {
                    return p.replace(":", "\\:");
                }).collect(Collectors.joining(" OR "));

                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }

                if (configuration.getVersion().equals(KramVersion.V5)) {
                    // Vsechny deti // K5
                    String encodedCondition = URLEncoder.encode(
                            "root_pid:(" + condition + ") AND fedora.model:(monographunit OR periodicalvolume)",
                            "UTF-8");
                    // K5
                    String encodedFieldList = URLEncoder.encode(
                            "PID fedora.model root_pid details datum_str dostupnost details dnnt-labels pid_path",
                            "UTF-8");
                    String url = baseUrl + "api/v5.0/search?q=" + encodedCondition + "&wt=json&rows=" + MAX_FETCHED_DOCS
                            + "&fl=" + encodedFieldList;

                    logger.fine(String.format("Kramerius url is %s and list of identifiers are %s", url, pairs.stream()
                            .map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));
                    try {
                        String result = simpleGET(url);
                        JSONObject resultJSON = new JSONObject(result);
                        JSONObject responseObject = resultJSON.getJSONObject("response");
                        int numFound = responseObject.optInt("numFound", 0);
                        if (numFound > 0) {
                            JSONArray docs = responseObject.optJSONArray("docs");
                            for (int i = 0; i < docs.length(); i++) {
                                JSONObject doc = docs.getJSONObject(i);
                                String rootPid = doc.optString(KrameriusFields.ROOT_PID_V5);

                                List<String> identifiers = pidsMapping.get(rootPid);

                                GranularityField gf = new GranularityField();
                                gf.setModel(doc.optString(KrameriusFields.FEDORA_MODEL_V5));
                                gf.setDate(doc.optString(KrameriusFields.DATUM_STR_V5));
                                gf.setPid(doc.optString(KrameriusFields.PID_V5));
                                gf.setRootPid(doc.optString(KrameriusFields.ROOT_PID_V5));
                                gf.setDostupnost(doc.optString(KrameriusFields.DOSTUPNOST_V5));

                                if (doc.has(KrameriusFields.DNNT_LABELS_V5)) {
                                    if (gf.getDostupnost() != null && !"public".equals(gf.getDostupnost())) {
                                        JSONArray dnntLabels = doc.getJSONArray(KrameriusFields.DNNT_LABELS_V5);
                                        List<String> kramLicenses = new ArrayList<>();
                                        for (int j = 0; j < dnntLabels.length(); j++) {
                                            String klicense = dnntLabels.getString(j);
                                            if (!kramLicenses.contains(klicense)) {
                                                kramLicenses.add(klicense);
                                            }
                                        }
                                        if (!kramLicenses.isEmpty()) {
                                            gf.setKramLicenses(kramLicenses);
                                        }
                                    }
                                }

                                if (doc.has(KrameriusFields.PID_PATH_V5)) {
                                    JSONArray pidPathJsonArray = doc.getJSONArray(KrameriusFields.PID_PATH_V5);
                                    List<String> pidPathList = new ArrayList<>();
                                    for (int j = 0; j < pidPathJsonArray.length(); j++) {
                                        pidPathList.add(pidPathJsonArray.getString(j));
                                    }
                                    gf.setPidPaths(pidPathList);
                                }

                                gf.setBaseUrl(baseUrl);
                                gf.setFetched(SIMPLE_DATE_FORMAT.format(new Date()));
                                gf.setAcronym(configuration.getAcronym());
                                String link = renderLink(baseUrl, gf.getPid());
                                gf.setLink(link);
                                gf.setTypeOfRec(TypeOfRec.KRAM_SOLR);

                                if (gf.getModel() != null && gf.getModel().equals("periodicalvolume")) {
                                    JSONArray detailsJSONArray = doc.getJSONArray("details");
                                    if (detailsJSONArray.length() > 0) {
                                        gf.setDetails(detailsJSONArray.getString(0));
                                        String details = gf.getDetails();
                                        if (details != null) {
                                            String[] splitted = details.split("##");
                                            if (splitted.length > 1) {
                                                gf.setRocnik(splitted[0]);
                                                gf.setCislo(splitted[1]);
                                            } else if (splitted.length > 0) {
                                                gf.setRocnik(splitted[0]);
                                            }
                                        }
                                    }
                                } else {
                                    JSONArray detailsJSONArray = doc.getJSONArray("details");
                                    if (detailsJSONArray.length() > 0) {
                                        gf.setDetails(detailsJSONArray.getString(0));
                                        String details = gf.getDetails();
                                        String[] splitted = details.split("##");
                                        if (splitted.length > 0) {
                                            gf.setCislo(splitted[0]);
                                        }
                                        gf.setRocnik(gf.getDate());

                                    }
                                }

                                identifiers.stream().forEach(ident -> {

                                    if (linksOwner.containsKey(ident)) {
                                        Granularity granularity = linksOwner.get(ident).getGranularity();
                                        // 911r
                                        boolean acceptField = granularity.acceptByRule(gf, getLogger());
                                        if (acceptField) {
                                            granularity.addGranularityField(gf);
                                        }
                                    } else {
                                        logger.log(Level.WARNING, "missing granularity for " + ident);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE,
                                String.format("Error while accessing %s, error: %s ", url, e.getMessage()), e);
                    }
                } else {
                    
                    // Vsechny deti // K7
                    String encodedCondition = URLEncoder.encode(
                            "root.pid:(" + condition + ") AND model:(monographunit OR periodicalvolume)",
                            "UTF-8");
                    
                    String encodedFieldList = URLEncoder.encode(
                            "pid model root.pid date.str licenses licenses_of_ancestors own_pid_path part.number.str",
                            "UTF-8");

                    String url = baseUrl + "api/client/v7.0/search?q=" + encodedCondition + "&wt=json&rows="
                            + MAX_FETCHED_DOCS + "&fl=" + encodedFieldList;

                    logger.fine(String.format("Kramerius url is %s and list of identifiers are %s", url, pairs.stream()
                            .map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));

                    String result = simpleGET(url);
                    JSONObject resultJSON = new JSONObject(result);
                    JSONObject responseObject = resultJSON.getJSONObject("response");
                    int numFound = responseObject.optInt("numFound", 0);
                    if (numFound > 0) {

                        JSONArray docs = responseObject.optJSONArray("docs");
                        for (int i = 0; i < docs.length(); i++) {
                            JSONObject doc = docs.getJSONObject(i);
                            String rootPid = doc.optString(KrameriusFields.ROOT_PID_V7);

                            List<String> identifiers = pidsMapping.get(rootPid);

                            GranularityField gf = new GranularityField();
                            gf.setModel(doc.optString(KrameriusFields.MODEL_V7));
                            gf.setDate(doc.optString(KrameriusFields.DATE_STR_V7));
                            gf.setPid(doc.optString(KrameriusFields.PID_V7));
                            gf.setRootPid(doc.optString(KrameriusFields.ROOT_PID_V7));

                            gf.setRocnik(doc.optString(KrameriusFields.DATE_STR_V7));
                            gf.setCislo(doc.optString(KrameriusFields.PART_NUMBER_V7));
                            
                            List<String> licensesList = v7licenses(doc);
                            if (!licensesList.isEmpty()) {
                                gf.setKramLicenses(licensesList);
                            }

                            String dostupnost = "private";
                            if (licensesList.contains("public")) { dostupnost = "public"; }
                            gf.setDostupnost(dostupnost);
                                
//

                            if (doc.has(KrameriusFields.OWN_PID_PATH_K7)) {
                                List<String> ownPidPaths = Arrays.asList(doc.getString(KrameriusFields.OWN_PID_PATH_K7));
                                gf.setPidPaths(ownPidPaths);
                            }

                            gf.setBaseUrl(baseUrl);
                            gf.setFetched(SIMPLE_DATE_FORMAT.format(new Date()));

                            gf.setAcronym(configuration.getAcronym());

                            String link = renderLink(baseUrl, gf.getPid());
                            gf.setLink(link);

                            gf.setTypeOfRec(TypeOfRec.KRAM_SOLR);

                            identifiers.stream().forEach(ident -> {
                                if (linksOwner.containsKey(ident)) {
                                    Granularity granularity = linksOwner.get(ident).getGranularity();
                                    // 911r
                                    boolean acceptField = granularity.acceptByRule(gf, getLogger());
                                    if (acceptField) {
                                        granularity.addGranularityField(gf);
                                    }
                                } else {
                                    logger.log(Level.WARNING, "missing granularity for " + ident);
                                }
                            });
                        }
                    
                    }
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        //
        // buffer.clear();
    }


    private String renderLink(String baseUrl, String pid) {
        InstanceConfiguration configuration = this.checkConf.match(baseUrl);
        // InstanceConfiguration configuration = this.checkConf.get(baseUrl);
        if (configuration != null && StringUtils.isAnyString(configuration.getClientAddress())) {
            return MessageFormat.format(configuration.getClientAddress(), pid);
        } else {
            return baseUrl + (baseUrl.endsWith("/") ? "handle/" : "/handle/") + pid;
        }
    }


    protected String simpleGET(String url) throws IOException {
        return SimpleGET.get(url);
    }

    public static void main(String[] args) throws IOException {

//        String url ="https://api.kramerius.mzk.cz/search";
//        JSONObject checkKamerius = Options.getInstance().getJSONObject("check_kramerius");
//        CheckKrameriusConfiguration initConfiguration = CheckKrameriusConfiguration.initConfiguration(checkKamerius);
//        InstanceConfiguration match = initConfiguration.match(url);
//        System.out.println(match);
//        List<InstanceConfiguration> instances = initConfiguration.getInstances();
//        System.out.println(initConfiguration);

        GranularityServiceImpl service = new GranularityServiceImpl("test");
        try {
            service.initialize();
            service.refershGranularity();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try (SolrClient solrClient = service.buildClient()) {
                SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
            }
        }
    }

}
