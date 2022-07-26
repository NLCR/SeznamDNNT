package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.*;
import cz.inovatika.sdnnt.model.workflow.ZadostTyp;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.SimpleGET;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

/**
 * Sluzba kontroluje zda je v krameriovi dilo volne ci nikoliv 
 * Vysledkem je kontextova informace a zadost PXN
 * @author happy
 *
 */
public class PXKrameriusServiceImpl extends AbstractPXService implements PXKrameriusService {

    boolean contextInformation = false;
    private Map<String, String> mappingHosts = new HashMap<>();
    private Map<String, String> mappingApi = new HashMap<>();
    private Map<String, Boolean> skipHosts = new HashMap<>();

    private AtomicInteger fetchedInfoCounter = new AtomicInteger();
    private AtomicInteger requestedInfoCounter = new AtomicInteger();

    private Logger logger = Logger.getLogger(PXKrameriusService.class.getName());
    
    
    public PXKrameriusServiceImpl(String logger, JSONObject iteration, JSONObject results) {
        if (iteration != null) {
            super.iterationConfig(iteration);
        }
        if (results != null) {
            contextInformation = results.optBoolean("ctx");
            super.requestsConfig(results);
        }
        
        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }
    }

    public  Set<String> usedInRequest(SolrClient solr, List<String> identifiers, String typeOfRequest) {
        try {
            Set<String> retval = new HashSet<>();
            SolrQuery query = new SolrQuery("*")
                    .setFields("id", "identifiers")
                    .addFilterQuery("navrh:" + typeOfRequest)
                    .setRows(3000);

            String collected = identifiers.stream().map(id -> '"' + id + '"').collect(Collectors.joining(" OR "));
            query.addFilterQuery("identifiers:(" + collected + ")");

            SolrDocumentList zadost = solr.query("zadost", query).getResults();
            zadost.stream().forEach(solrDoc -> {
                Collection<Object> identsFromZadost = solrDoc.getFieldValues("identifiers");
                identsFromZadost.stream().map(Object::toString).forEach(foundInZadost -> {
                    if (identifiers.contains(foundInZadost)) {
                        retval.add(foundInZadost);
                    }
                });
            });
            return retval;
        } catch (SolrServerException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return new HashSet<>();
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
        } else return null;
    }

    

    
    public void initialize() {
        JSONObject checkKamerius = getOptions().getJSONObject("check_kramerius");
        if (checkKamerius != null && checkKamerius.has("urls")) {
            JSONObject urlobject = checkKamerius.getJSONObject("urls");
            Set<String> urls = urlobject.keySet();
            for (String key : urls) {
                JSONObject jObject = urlobject.getJSONObject(key);
                String api = jObject.optString("api");
                if (api != null) {
                    this.mappingHosts.put(key, api);
                    String version = jObject.optString("version");
                    if (version != null) {
                        this.mappingApi.put(key, version);
                    }
                }
                
                if (jObject.has("skip")) {
                    this.skipHosts.put(key, true);
                    this.skipHosts.put(api, true);
                }

            }
        }
    }


    @Override
    public List<String> check() {
        logger.info("Initializing px process");

        this.initialize();
        List<String> foundCandidates = new ArrayList<>();
        Map<String, List<String>> mapping = new HashMap<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + LIMIT);

        List<String> plusFilter = new ArrayList<>(Arrays.asList("id_pid:uuid", FMT_FIELD + ":BK"));
        if (yearConfiguration != null && !this.yearConfiguration.trim().equals("")) {
            plusFilter.add(yearFilter());
        }

        if (!this.states.isEmpty()) {
            String collected = states.stream().collect(Collectors.joining(" OR "));
            plusFilter.add(DNTSTAV_FIELD + ":(" + collected + ")");
        }
        logger.info("Current iteration filter " + plusFilter);
        try (final SolrClient solrClient = buildClient()) {
            support.iterate(solrClient, reqMap, null, plusFilter, Arrays.asList(KURATORSTAV_FIELD + ":X", KURATORSTAV_FIELD + ":PX", DNTSTAV_FIELD+":D"), Arrays.asList(
                    IDENTIFIER_FIELD,
                    SIGLA_FIELD,
                    MARC_911_U,
                    MARC_956_U,
                    MARC_856_U,
                    GRANULARITY_FIELD
            ), (rsp) -> {
                Object identifier = rsp.getFieldValue("identifier");

                Collection<Object> links1 = rsp.getFieldValues(MARC_911_U);
                Collection<Object> links2 = rsp.getFieldValues(MARC_856_U);
                Collection<Object> links3 = rsp.getFieldValues(MARC_956_U);

                if (links1 != null && !links1.isEmpty()) {
                    List<String> ll = links1.stream().map(Object::toString).collect(Collectors.toList());
                    mapping.put(identifier.toString(), ll);
                } else if (links2 != null && !links2.isEmpty()) {
                    List<String> ll = links2.stream().map(Object::toString).collect(Collectors.toList());
                    mapping.put(identifier.toString(), ll);
                } else if (links3 != null && !links3.isEmpty()) {
                    List<String> ll = links3.stream().map(Object::toString).collect(Collectors.toList());
                    mapping.put(identifier.toString(), ll);
                }
            }, IDENTIFIER_FIELD);
        } catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        
        logger.info("Found candidates: "+mapping.size());
        
        try (final SolrClient solr = buildClient()) {

            List<String> keys = new ArrayList<>(mapping.keySet());
            int batchSize = 40;
            int numberOfBatches = keys.size() / batchSize;
            if (keys.size() % batchSize > 0) {
                numberOfBatches += 1;
            }
            for (int i = 0; i < numberOfBatches; i++) {
                int startIndex = i * batchSize;
                int endIndex = (i + 1) * batchSize;
                List<String> batch = keys.subList(startIndex, Math.min(endIndex, keys.size()));
                if (this.typeOfRequest != null) {
                	// used in reuqest; must not be in case of context information
                    Set<String> used = usedInRequest(solr, batch, this.typeOfRequest);
                    used.stream().forEach(mapping::remove);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        logger.info("Found candidates after used check: "+mapping.keySet().size());
        
        Map<String, List<Pair<String, String>>> buffer = new HashMap<>();
        for (String key : mapping.keySet()) {
            List<String> links = mapping.get(key);

            // master title; TODO: granularity check
            String master = links.get(0);
            String pid = pid(master);
            if (pid != null) {
                String baseUrl = baseUrl(master);
                if (!buffer.containsKey(baseUrl)) {
                    buffer.put(baseUrl, new ArrayList<>());
                }
                buffer.get(baseUrl).add(Pair.of(key, pid));
                checkBuffer(buffer, foundCandidates);
            }
        }

        if (!buffer.isEmpty()) {
            clearBuffer(buffer, foundCandidates);
        }
        logger.info(String.format("Requested info count %d, Fetched info count %d", this.requestedInfoCounter.get(), this.fetchedInfoCounter.get()));
        return foundCandidates;
    }

    
    protected void checkBuffer(Map<String, List<Pair<String, String>>> buffer, List<String> foundCadidates) {
        Integer sum = buffer.values().stream().map(List::size).reduce(0, Integer::sum);
        if (sum > bufferSize()) {
            clearBuffer(buffer, foundCadidates);
        }
    }
    
    protected int bufferSize() {
        JSONObject jsonObject = getOptions().getJSONObject("check_kramerius");
        if (jsonObject != null) {
            return jsonObject.optInt("buffersize", CHECK_SIZE);
        } else return CHECK_SIZE;
    }

    protected void clearBuffer(Map<String, List<Pair<String, String>>> buffer, List<String> foundCadidates) {
        try {
            for (String baseUrl : buffer.keySet()) {
                if (baseUrl == null) continue;
                
                if (this.skipHosts.containsKey(baseUrl)) {
                    getLogger().warning("Skipping url "+baseUrl+"'");
                    continue;
                }

                List<Pair<String, String>> pairs = buffer.get(baseUrl);
                String condition = pairs.stream().map(Pair::getRight).filter(Objects::nonNull).map(p -> {
                    return p.replace(":", "\\:");
                }).collect(Collectors.joining(" OR "));

                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
                this.requestedInfoCounter.addAndGet(pairs.size());

                String encodedCondition = URLEncoder.encode("PID:(" + condition + ")", "UTF-8");
                String encodedFieldList = URLEncoder.encode("PID dostupnost","UTF-8");
                String url = baseUrl + "api/v5.0/search?q=" +  encodedCondition + "&wt=json&rows="+pairs.size()+"&fl="+encodedFieldList;
                
                logger.fine(String.format("Kramerius url is %s and list of identifiers are %s", url, pairs.stream().map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));

                try {
                    String result = simpleGET(url);
                        
                    JSONObject object = new JSONObject(result);
                    JSONObject response = object.getJSONObject("response");
                    JSONArray jsonArray = response.getJSONArray("docs");
                    for (int i = 0, ll = jsonArray.length(); i < ll; i++) {
                        JSONObject oneDoc = jsonArray.getJSONObject(i);
                        this.fetchedInfoCounter.addAndGet(1);
                        
                        if (oneDoc.has("dostupnost")) {
                            String pid = oneDoc.getString("PID");
                            String dostupnost = oneDoc.getString("dostupnost");
                            
                            if (dostupnost != null && dostupnost.equals("public")) {

                                Optional<Pair<String, String>> any = pairs.stream().filter(p -> {
                                    return p.getRight().endsWith(pid);
                                }).findAny();

                                if (any.isPresent()) {
                                    Pair<String, String> pair = any.get();
                                    logger.info(String.format("Found public document. Identifier %s and pid %s", pair.getLeft(), pair.getRight()));
                                    foundCadidates.add(pair.getLeft());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, String.format("Error while accessing %s, error: %s ", url, e.getMessage()) , e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        buffer.clear();
    }

    protected String simpleGET(String url) throws IOException {
        return SimpleGET.get(url);
    }

    public String baseUrl(String surl) {
        if (surl.contains("/search/")) {
            String val = surl.substring(0, surl.indexOf("/search") + "/search".length());
            String foundByPrefix = findValueByPrefix(val);
            return foundByPrefix != null ? foundByPrefix : val;
        } else {
            List<String> prefixes = Arrays.asList("view", "uuid");
            for (String pref : prefixes) {
                if (surl.contains(pref)) {
                    String remapping = surl.substring(0, surl.indexOf(pref));
                    String foundByPrefix = findValueByPrefix(remapping);
                    return foundByPrefix != null ? foundByPrefix : (remapping + (remapping.endsWith("/") ? "": "/")+"search");
                }

            }
            return null;
        }
    }


    private String findValueByPrefix(String val) {
        for (String key : this.mappingHosts.keySet()) {
            if (val.startsWith(key)) {
                return this.mappingHosts.get(key);
            }
        }
        return null;
    }
    
    @Override
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException {
        if (!identifiers.isEmpty()) {

            CuratorItemState cState = null;
            PublicItemState pState = null;
            if (this.destinationState != null && StringUtils.isAnyString(this.destinationState)) {
                cState = CuratorItemState.valueOf(this.destinationState);
                pState = cState.getPublicItemState(null);
            }
            try (final SolrClient solr = buildClient()) {
                // diff documents - use to have field
                for (String identifier : identifiers) {
                    SolrInputDocument idoc = null;
                    logger.fine(String.format("Updating identifier %s", identifier));
                    if (cState != null) {
                        idoc = ChangeProcessStatesUtility.changeProcessState(solr, identifier, cState.name(), "scheduler/kramerius check");
                    }
                    // history information
                    if (contextInformation) {
                        logger.fine("Setting context information ");
                        if (idoc != null) {
                            super.enahanceContextInformation(idoc);
                        } else {
                            idoc = super.changeContextInformation(solr, identifier);
                        }
                    }
                    if (idoc != null) {
                        solr.add(DataCollections.catalog.name(), idoc);
                    }
                }
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
            }
        }
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }
    
    

}
