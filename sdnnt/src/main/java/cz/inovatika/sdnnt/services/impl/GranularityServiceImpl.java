package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.ALTERNATIVE_ALEPH_LINK;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.GRANULARITY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_856_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_911_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_956_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SIGLA_FIELD;

import java.io.IOException;
import java.net.URLEncoder;
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
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateSKCUtils;
import cz.inovatika.sdnnt.services.GranularityService;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.impl.utils.SKCYearsUtils;
import cz.inovatika.sdnnt.services.impl.utils.SolrYearsUtils;
import cz.inovatika.sdnnt.services.impl.zahorikutils.ZahorikUtils;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.granularities.Granularity;
import cz.inovatika.sdnnt.services.kraminstances.granularities.GranularityField;
import cz.inovatika.sdnnt.services.kraminstances.granularities.GranularityField.TypeOfRec;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SimpleGET;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class GranularityServiceImpl extends AbstractGranularityService implements GranularityService {

    public static Logger LOGGER = Logger.getLogger(GranularityServiceImpl.class.getName());
    
    private static final int MAX_FETCHED_DOCS = 1000;
    public static final int CHECK_SIZE = 70;
    
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
    
    private Logger logger = Logger.getLogger(GranularityService.class.getName());


    private Map<String, Granularity> granularities = new HashMap<>();

    private Set<String> changedIdentifiers = new LinkedHashSet<>();
    public CheckKrameriusConfiguration checkConf = null;
    
    public GranularityServiceImpl(String logger) {
        if (logger != null) {
            this.logger = Logger.getLogger(logger);
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
//        String baseUrl = checkConf.findValueByPrefix(link);
//        InstanceConfiguration configuration = this.checkConf.get(baseUrl);
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
                    return splitted[splitted.length -2]+"."+splitted[splitted.length-1];
                } else {
                    return domainSubString; 
                }
            } else return null;
        }
     }

    @Override
    public void refershGranularity() throws IOException {
        // processing map; not every 
        Map<String, List<String>> mapping = new HashMap<>();
        Map<String, List<String>> rules911r = new HashMap<>(); 
        
        try (final SolrClient solrClient = buildClient()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "10000");

            CatalogIterationSupport support = new CatalogIterationSupport();
            List<String> plusFilter = Arrays.asList("id_pid:uuid", KURATORSTAV_FIELD + ":*");

            List<String> minusFilter = Arrays.asList( KURATORSTAV_FIELD + ":D",
                    KURATORSTAV_FIELD + ":DX");

            AtomicInteger count = new AtomicInteger();
            support.iterate(solrClient, reqMap, null, plusFilter, minusFilter,
                    Arrays.asList(IDENTIFIER_FIELD, SIGLA_FIELD, MARC_911_U, MARC_956_U, MARC_856_U, GRANULARITY_FIELD, "marc_911r"

                    ), (rsp) -> {
                        
                        
                        Object identifier = rsp.getFieldValue("identifier");
                        
                        Collection<Object> links1 = rsp.getFieldValues(MARC_911_U);
                        Collection<Object> links3 = rsp.getFieldValues(MARC_856_U);
                        List<String> granularity = (List<String>) rsp.getFieldValue(GRANULARITY_FIELD);
                        
                        List<String> granularityLinks = new ArrayList<>();
                        if (granularity != null) {
                            Granularity granObject = new Granularity(identifier.toString());
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
                            getLogger().info("Getting granularity for :"+identifier.toString() +" and size is "+this.granularities.size());
                            this.granularities.put(identifier.toString(), granObject);
                        }

                        if (links1 != null && !links1.isEmpty()) {
                            
                            Collection<Object> yearsColl = rsp.getFieldValues("marc_911r");
                            if (yearsColl != null) {

                                List<String> years = yearsColl.stream().map(Objects::toString).collect(Collectors.toList());
                                rules911r.put(identifier.toString(), years);

                                if (this.granularities.containsKey(identifier.toString())) {
                                    years.stream().forEach(l-> {
                                        this.granularities.get(identifier.toString()).addTitleRule(l);
                                    });
                                }
                                
                            }
                            
                            List<String> ll = links1.stream().map(Object::toString).collect(Collectors.toList());
                            List<String> filtered = ll.stream().filter(it -> !granularityLinks.contains(it))
                                    .collect(Collectors.toList());


                            if (this.granularities.containsKey(identifier.toString())) {
                                ll.stream().forEach(l-> {
                                    this.granularities.get(identifier.toString()).addTitleUrl(l);
                                });
                            }
                            
                            
                            mapping.put(identifier.toString(), filtered);
                        
                        } else if (links3 != null && !links3.isEmpty()) {
                            List<String> ll = links3.stream().map(Object::toString).collect(Collectors.toList());
                            List<String> filtered = ll.stream().filter(it -> !granularityLinks.contains(it))
                                    .collect(Collectors.toList());

                            if (this.granularities.containsKey(identifier.toString())) {
                                ll.stream().forEach(l-> {
                                    this.granularities.get(identifier.toString()).addTitleUrl(l);
                                });
                            }
                            
                            
                            mapping.put(identifier.toString(), filtered);
                        }
                        
                        
                        
                    }, IDENTIFIER_FIELD);
        }
        
        // mapuje id na seznam titulu v knihovnach
        logger.info("Found candidates: " + mapping.size());

        AtomicInteger iteration = new AtomicInteger();
        Map<String, List<Pair<String, String>>> buffer = new HashMap<>();
        for (String key : mapping.keySet()) {

            int counter = iteration.incrementAndGet();
            if ((counter % 10000) == 0) {
                getLogger().fine("Counter: "+counter);
            }
            // identifikator - url 
            Granularity granularity = this.granularities.get(key);
            List<String> links = mapping.get(key);
            for (String link : links) {
                String pid = pid(link);
                if (pid != null) {
                    String baseUrl = checkConf.baseUrl(link);
                    InstanceConfiguration configuration = checkConf.match(baseUrl);
                    if (configuration !=  null && !configuration.isShouldSkip()) {
                        if (!buffer.containsKey(baseUrl)) {
                            buffer.put(baseUrl, new ArrayList<>());
                        }
                        getLogger().info("baseurl " + baseUrl + " pair " + Pair.of(key, pid));
                        buffer.get(baseUrl).add(Pair.of(key, pid));
                        checkBuffer(buffer, rules911r);
                    } else {
                        getLogger().fine("Skipping instance '"+configuration+"'");
                    }
                }
            }
        }

        if (!buffer.isEmpty()) {
            checkBuffer(buffer, rules911r);
        }
        
        
        List<SolrInputDocument> changes = new ArrayList<>();
        this.granularities.keySet().forEach(key-> {
            this.changedIdentifiers.add(key);
            Granularity granularity = this.granularities.get(key);
            granularity.merge(this.checkConf);
            changes.add(granularity.toSolrDocument());
        });
        
        try (final SolrClient solr = buildClient()) {
            int setsize = changes.size();
            int batchsize = 10000;
            int numberofiteration = changes.size() / batchsize;
            if (setsize % batchsize != 0) numberofiteration = numberofiteration + 1;
            for (int i = 0; i < numberofiteration; i++) {
                int from = i*batchsize;
                int to = Math.min((i+1)*batchsize, setsize);
                List<SolrInputDocument> batchDocs = changes.subList(from, to);
                getLogger().info(String.format("Updating records %d - %d and size %d", from, to,batchDocs.size()));
                
                UpdateRequest req = new UpdateRequest();
                for (SolrInputDocument bDoc : batchDocs) {
                    req.add(bDoc);
                }
                
                try {
                    UpdateResponse response = req.process(solr, DataCollections.catalog.name());
                    getLogger().info("qtime:"+response.getQTime());
                } catch (SolrServerException  | IOException e) {
                    e.printStackTrace();
                    getLogger().log(Level.SEVERE,e.getMessage());
                }
            }
            
            SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
        }
        //logger.info("Refreshing finished. Updated identifiers "+this.changedIdentifiers);
        logger.info("Refreshing finished. Updated identifiers "+this.changedIdentifiers.size());
        
    }

    protected void checkBuffer(Map<String, List<Pair<String, String>>> buffer, Map<String, List<String>> filterGranularityRules) {
        Integer sum = buffer.values().stream().map(List::size).reduce(0, Integer::sum);
        if (sum > CHECK_SIZE) {
            clearBuffer(buffer, filterGranularityRules);
        }
    }

    protected void clearBuffer(Map<String, List<Pair<String, String>>> buffer, Map<String, List<String>> filterGranularityRules) {
        try {
            for (String baseUrl : buffer.keySet()) {
                if (baseUrl == null) {
                    continue;
                }

                InstanceConfiguration configuration = this.checkConf.match(baseUrl);

                if (configuration == null || configuration.isShouldSkip()) {
                    getLogger().warning("Skipping url "+baseUrl+"'");
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

                String encodedCondition = URLEncoder.encode(
                        "root_pid:(" + condition + ") AND fedora.model:(monographunit OR periodicalvolume)", "UTF-8");
                String encodedFieldList = URLEncoder.encode("PID fedora.model root_pid details datum_str dostupnost details", "UTF-8");
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

                        Map<String, List<GranularityField>> solrResonseGranularity = new HashMap<>();

                        JSONArray docs = responseObject.optJSONArray("docs");
                        for (int i = 0; i < docs.length(); i++) {
                            JSONObject doc = docs.getJSONObject(i);
                            String rootPid = doc.optString("root_pid");
                            if (!solrResonseGranularity.containsKey(rootPid)) {
                                solrResonseGranularity.put(rootPid, new ArrayList<>());
                            }
                            
                            List<Pair<Integer, Integer>> ranges = new ArrayList<>();
                            List<String> identifiers = pidsMapping.get(rootPid);
                            if (filterGranularityRules != null) {
                                identifiers.stream().forEach(id-> {
                                    if (filterGranularityRules.containsKey(id)) {
                                        List<String> marc911r = filterGranularityRules.get(id);
                                        List<Pair<Integer, Integer>> orange = marc911r.stream().map(SKCYearsUtils::skcRange).flatMap(Collection::stream).collect(Collectors.toList());
                                        ranges.addAll(orange);
                                    }
                                });
                            }
                            
                            GranularityField gf = new GranularityField();
                            gf.setModel(doc.optString("fedora.model"));
                            gf.setDate(doc.optString("datum_str"));
                            gf.setPid(doc.optString("PID"));
                            gf.setRootPid(doc.optString("root_pid"));
                            gf.setDostupnost(doc.optString("dostupnost"));
                            // Zahorikovo reseni, mnohokrat reseno po mailech 
                            // Pokud je public, automaticky dostava priznak X - 
                            /*
                            if (dostupnost != null && dostupnost.equals("public")) {
                                JSONArray stav = new JSONArray();
                                stav.put("X");
                                object.put("kuratorstav", stav);
                                object.put("stav", stav);
                            }*/
                            

                            
                            gf.setBaseUrl(baseUrl);
                            gf.setFetched(SIMPLE_DATE_FORMAT.format(new Date()));
                            
                            gf.setAcronym(configuration.getAcronym());
                            
                            String link = renderLink(baseUrl, gf.getPid());
                            gf.setLink( link);

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
                            

                            
                            solrResonseGranularity.get(rootPid).add(gf);

                            identifiers.stream().forEach(ident-> {
                                if (!granularities.containsKey(ident)) {
                                    granularities.put(ident, new Granularity(ident));
                                }
                                Granularity granularity = this.granularities.get(ident);
                                boolean acceptField = true;
                                if (ranges != null && !ranges.isEmpty()) {
                                    acceptField = gf.accept(ranges);
                                }
                                if (acceptField) {
                                    granularity.addGranularityField(gf);
                                }
                            });
                        }

                        
                        /*
                        try (final SolrClient solrClient = buildClient()) {
                            Set keySet = solrResonseGranularity.keySet();
                            for (Object key : keySet) {
                                // identifiers not identifier
                                List<String> identifiers = pidsMapping.get(key.toString());
                                for (String identifier : identifiers) {
                                    // pokud jsou pravidla, pak filtruj granularitu 
                                    List<Map<String, String>> digitalized = solrResonseGranularity.get(key.toString());
                                    if (filterGranularityRules.containsKey(identifier)) {

                                        List<String> rules = filterGranularityRules.get(identifier);
                                        if (rules != null && rules.size() > 0) {
                                            digitalized = filterGranularity(rules, digitalized);
                                        }
                                    }
                                    SolrQuery q = (new SolrQuery("identifier:\"" + identifier + "\"")
                                            .setFields(MarcRecordFields.GRANULARITY_FIELD)).setRows(1);
                                    QueryResponse response = solrClient.query(DataCollections.catalog.name(), q);
                                    SolrDocumentList solrResults = response.getResults();
                                    if (solrResults.getNumFound() > 0) {
                                        SolrDocument sDoc = solrResults.get(0);
                                        if (sDoc.containsKey(MarcRecordFields.GRANULARITY_FIELD)) {
                                            List<String> solrGran = (List<String>) sDoc
                                                    .getFieldValue(MarcRecordFields.GRANULARITY_FIELD);

                                            compare(solrClient, identifier, baseUrl, solrGran, digitalized);
                                        } else {
                                            add(solrClient, identifier, baseUrl, digitalized);
                                        }
                                    }
                                    
                                }
                            }
                        }
                        */
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, String.format("Error while accessing %s, error: %s ", url, e.getMessage()),
                            e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        buffer.clear();
    }

    private List<Map<String, String>> filterGranularity(List<String> rules, List<Map<String,String>> digitalized) {
        Set<Map<String,String>> set = new LinkedHashSet<>();

        List<Pair<Integer, Integer>> pairs = rules.stream().map(SKCYearsUtils::skcRange).flatMap(Collection::stream).collect(Collectors.toList());
        for (Map<String, String> map : digitalized) {
            if (map.containsKey("datum_str")) {
                String date = map.get("datum_str");
                Integer date2 = SolrYearsUtils.solrDate(date);
                for (Pair<Integer, Integer> p : pairs) {
                    if ( date2 >= p.getLeft() && date2 <= p.getRight()) {
                        set.add(map);
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    private void add(SolrClient solr, String identifier, String baseUrl, List<Map<String, String>> list)
            throws SolrServerException, Exception {

        getLogger().fine("Adding granularity for identifier " + identifier);
        
        if (!list.isEmpty()) {
            String rootPid = list.get(0).get("root_pid");

            JSONObject object = new JSONObject();
            object.put("link", renderLink(baseUrl, rootPid));

            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField(IDENTIFIER_FIELD, identifier);
            atomicAdd(idoc, object.toString(), MarcRecordFields.GRANULARITY_FIELD);
            solr.add(DataCollections.catalog.name(), idoc);
        }
        
        List<String> pidsFromKram = list.stream().map(p -> {
            return p.get("pid");
        }).collect(Collectors.toList());

        for (int i = 0; i < pidsFromKram.size(); i++) {
            JSONObject object = new JSONObject();
            
            String renderingPID = pidsFromKram.get(i);
            String link = renderLink(baseUrl, renderingPID);
            object.put("link", link);

            Map<String, String> oDoc = list.get(i);
            if (oDoc.containsKey("datum_str")) {
                object.put("rocnik", oDoc.get("datum_str"));
            }
            if (oDoc.containsKey("details")) {
                // oDoc.
                String details = oDoc.get("details");
                String[] splitted = details.split("##");
                if (splitted.length > 1) {
                    object.put("cislo", splitted[1]);
                }
            }

            if (oDoc.containsKey("dostupnost")) {
                String dostupnost = oDoc.get("dostupnost");
                object.put("policy", oDoc.get("dostupnost"));
                
                if (dostupnost != null && dostupnost.equals("public")) {
                    JSONArray stav = new JSONArray();
                    stav.put("X");
                    object.put("kuratorstav", stav);
                    object.put("stav", stav);
                }
                
            }

            object.put("fetched", SIMPLE_DATE_FORMAT.format(new Date()));

            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField(IDENTIFIER_FIELD, identifier);
            atomicAdd(idoc, object.toString(), MarcRecordFields.GRANULARITY_FIELD);
            solr.add(DataCollections.catalog.name(), idoc);

            this.changedIdentifiers.add(identifier);
            getLogger().fine(String.format("Adding granularity for %s", identifier));
        }
    }

    private String renderLink(String baseUrl, String pid) {
        InstanceConfiguration configuration = this.checkConf.match(baseUrl);
        //InstanceConfiguration configuration = this.checkConf.get(baseUrl);
        if (configuration != null && StringUtils.isAnyString(configuration.getClientAddress())) {
            return MessageFormat.format(configuration.getClientAddress(), pid);
        } else {
            return baseUrl +(baseUrl.endsWith("/") ? "handle/" :  "/handle/") + pid;
        }
    }

    // pouze jedna baseUrl
    private void compare(SolrClient solr, String identifier, String baseUrl, List<String> solrGran,
            List<Map<String, String>> list) throws SolrServerException, IOException {
        getLogger().fine("Comparing granularity for identifier " + identifier);
        
        Map<Pair,JSONObject> pairsToGranularity = pairsToGranularity(solrGran);
        Set<Pair> pidsFromSolr = pairsToGranularity.keySet();

        String baseUrlDomain = domain(baseUrl);
        List<Pair> pidsFromKram = list.stream().map(p -> {
            return Pair.of(baseUrlDomain, p.get("pid"));
        }).collect(Collectors.toList());
        
        
        for (int i = 0; i < pidsFromKram.size(); i++) {
            // ADD  
            if (!pidsFromSolr.contains(pidsFromKram.get(i))) {

                String pid = pidsFromKram.get(i).getRight().toString();
                String link = renderLink(baseUrl, pid);

                JSONObject object = new JSONObject();
                object.put("link", link);

                Map<String, String> oDoc = list.get(i);
                if (oDoc.containsKey("datum_str")) {
                    object.put("rocnik", oDoc.get("datum_str"));
                }
                if (oDoc.containsKey("details")) {
                    // oDoc.
                    String details = oDoc.get("details");
                    String[] splitted = details.split("##");
                    if (splitted.length > 1) {
                        object.put("cislo", splitted[1]);
                    }
                }
                
                if (oDoc.containsKey("dostupnost")) {
                    String dostupnost = oDoc.get("dostupnost");
                    object.put("policy", dostupnost);
                    if (dostupnost != null && dostupnost.equals("public")) {
                        JSONArray stav = new JSONArray();
                        stav.put("X");
                        object.put("kuratorstav", stav);
                        object.put("stav", stav);
                    }
                }
                
                object.put("fetched", SIMPLE_DATE_FORMAT.format(new Date()));
               
                SolrInputDocument idoc = new SolrInputDocument();
                idoc.setField(IDENTIFIER_FIELD, identifier);
                atomicAdd(idoc, object.toString(), MarcRecordFields.GRANULARITY_FIELD);
                solr.add(DataCollections.catalog.name(), idoc);

                this.changedIdentifiers.add(identifier);
                getLogger().fine(String.format("Updating granularity for %s", identifier));
            }
        }
    }

    private Map<Pair, JSONObject> pairsToGranularity(List<String> solrGran) {
        Map<Pair, JSONObject> docsFromSolr = new HashMap<>();
        solrGran.stream().forEach(s-> {
            try {
                JSONObject jObject = new JSONObject(s);
                String link = jObject.optString("link");
                String domain = domain(link);
                String pid = null;
                if (link != null && link.indexOf("uuid:") > 0) {
                    pid = link.substring(link.indexOf("uuid:"));
                }

                Pair pair = Pair.of(domain, pid);
                docsFromSolr.put(pair, jObject);
            } catch (JSONException e) {
                getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
            
        });
        return docsFromSolr;
    }

    private static String pid(String surl) {
        if (surl.contains("uuid:")) {
            String pid = surl.substring(surl.indexOf("uuid:"));
            char[] charArray = pid.toCharArray();
            boolean checkWS = false;
            for (int i = 0; i < charArray.length; i++) {
                if (Character.isWhitespace(charArray[i])) {
                    checkWS = true;
                }
            }
            return checkWS ? null : pid;
        } else
            return null;
    }

    protected String simpleGET(String url) throws IOException {
        return SimpleGET.get(url);
    }
    

    public static void main(String[] args) throws IOException {
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
