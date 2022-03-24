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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

public class PXKrameriusServiceImpl extends AbstractPXService implements PXKrameriusService {

    public static final Logger LOGGER = Logger.getLogger(PXKrameriusService.class.getName());


    boolean contextInformation = false;
    private Map<String, String> mappingHosts = new HashMap<>();
    private Map<String, String> mappingApi = new HashMap<>();

    public PXKrameriusServiceImpl(JSONObject iteration, JSONObject results) {
        if (iteration != null) {
            super.iterationConfig(iteration);
        }
        if (results != null) {
            contextInformation = results.optBoolean("ctx");
            super.requestsConfig(results);
        }
    }

    public static Set<String> usedInRequest(SolrClient solr, List<String> identifiers, String typeOfRequest) {
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
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return new HashSet<>();
    }

    private static String pid(String surl) {
        if (surl.contains("uuid:")) {
            return surl.substring(surl.indexOf("uuid:"));
        } else return null;
    }



    protected void initialize() {
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
            }
        }
    }


    @Override
    public List<String> check() {

        LOGGER.info("Initializing px process");
        
        this.initialize();
        List<String> foundCandidates = new ArrayList<>();
        Map<String, List<String>> mapping = new HashMap<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + LIMIT);

        List<String> plusFilter = new ArrayList<>(Arrays.asList("id_pid:uuid", FMT_FIELD + ":BK"));
        if (yearConfiguration != null && !this.yearConfiguration.trim().equals("")) {
            //plusFilter.add(YEAR_OF_PUBLICATION+":" + yearConfiguration);
            plusFilter.add(yearFilter());
        }

        if (!this.states.isEmpty()) {
            String collected = states.stream().collect(Collectors.joining(" OR "));
            plusFilter.add(DNTSTAV_FIELD + ":(" + collected + ")");
        }
        LOGGER.info("Current iteration filter " + plusFilter);
        support.iterate(buildClient(), reqMap, null, plusFilter, Arrays.asList(DNTSTAV_FIELD + ":X", DNTSTAV_FIELD + ":PX"), Arrays.asList(
                IDENTIFIER_FIELD,
                SIGLA_FIELD,
                MARC_911_U,
                MARC_956_U,
                MARC_856_U,
                GRANULARITY_FIELD
        ), (rsp) -> {
            Object identifier = rsp.getFieldValue("identifier");

            Collection<Object> links1 = rsp.getFieldValues(MARC_911_U);
            Collection<Object> links2 = rsp.getFieldValues(MARC_956_U);
            Collection<Object> links3 = rsp.getFieldValues(MARC_856_U);

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

        
        LOGGER.info("Found candidates: "+mapping.size());
        
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
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        LOGGER.info("Found candidates after used check: "+mapping.size());
        
        Map<String, List<Pair<String, String>>> buffer = new HashMap<>();
        for (String key : mapping.keySet()) {
            List<String> links = mapping.get(key);

            // master title; TODO: granularity check
            String master = links.get(0);
            String pid = pid(master);

            String baseUrl = baseUrl(master);
            if (!buffer.containsKey(baseUrl)) {
                buffer.put(baseUrl, new ArrayList<>());
            }
            buffer.get(baseUrl).add(Pair.of(key, pid));
            checkBuffer(buffer, foundCandidates);
        }

        if (!buffer.isEmpty()) {
            clearBuffer(buffer, foundCandidates);
        }
        return foundCandidates;
    }

    protected void checkBuffer(Map<String, List<Pair<String, String>>> buffer, List<String> foundCadidates) {
        Integer sum = buffer.values().stream().map(List::size).reduce(0, Integer::sum);
        if (sum > CHECK_SIZE) {
            clearBuffer(buffer, foundCadidates);
        }
    }

    protected void clearBuffer(Map<String, List<Pair<String, String>>> buffer, List<String> foundCadidates) {
        try {
            for (String baseUrl : buffer.keySet()) {
                if (baseUrl == null) continue;
                List<Pair<String, String>> pairs = buffer.get(baseUrl);
                String condition = pairs.stream().map(Pair::getRight).filter(Objects::nonNull).map(p -> {
                    return p.replace(":", "\\:");
                }).collect(Collectors.joining(" OR "));

                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }


                String encodedCondition = URLEncoder.encode("PID:(" + condition + ")", "UTF-8");
                String url = baseUrl + "api/v5.0/search?q=" +  encodedCondition + "&wt=json&rows="+pairs.size();
                
                LOGGER.info(String.format("Testing url is %s and list of identifiers %s", url, pairs.stream().map(Pair::getLeft).filter(Objects::nonNull).collect(Collectors.toList()).toString()));

                try {
                    String result = simpleGET(url);
                    JSONObject object = new JSONObject(result);
                    JSONObject response = object.getJSONObject("response");
                    JSONArray jsonArray = response.getJSONArray("docs");
                    for (int i = 0, ll = jsonArray.length(); i < ll; i++) {
                        JSONObject oneDoc = jsonArray.getJSONObject(i);
                        if (oneDoc.has("dostupnost")) {
                            String pid = oneDoc.getString("PID");
                            String dostupnost = oneDoc.getString("dostupnost");
                            
                            if (dostupnost != null && dostupnost.equals("public")) {

                                Optional<Pair<String, String>> any = pairs.stream().filter(p -> {
                                    return p.getRight().endsWith(pid);
                                }).findAny();

                                if (any.isPresent()) {
                                    Pair<String, String> pair = any.get();
                                    LOGGER.info(String.format("Found public document. Identifier %s and pid %s", pair.getLeft(), pair.getRight()));
                                    foundCadidates.add(pair.getLeft());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        buffer.clear();
    }

    protected String simpleGET(String url) throws IOException {
        return SimpleGET.get(url);
    }

    String baseUrl(String surl) {
        if (surl.contains("/search/")) {
            String val = surl.substring(0, surl.indexOf("/search") + "/search".length());
            if (findByPrefix(val)) return this.mappingHosts.get(val);
            return val;
        } else {
            List<String> prefixes = Arrays.asList("view", "uuid");
            for (String pref : prefixes) {
                if (surl.contains(pref)) {
                    String remapping = surl.substring(0, surl.indexOf(pref));
                    if (findByPrefix(remapping)) {
                        return this.mappingHosts.get(remapping);
                    } else {
                        return remapping + "/search";
                    }
                }

            }
            return null;
        }
    }

    private boolean findByPrefix(String val) {
        for (String key : this.mappingHosts.keySet()) {
            if (val.startsWith(key)) {
                return true;
            }
        }
        return false;
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

                for (String identifier : identifiers) {
                    
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identifier);
                    
                    
                    
                    LOGGER.fine(String.format("Updating identifier %s", identifier));
                    if (cState != null) {
                        LOGGER.fine(String.format("Setting curator state %s", cState.name()));
                        atomicUpdate(idoc, cState.name(), KURATORSTAV_FIELD);
                    }
                    if (pState != null) {
                        LOGGER.fine(String.format("Setting public state %s", pState.name()));
                        atomicUpdate(idoc, pState.name(), DNTSTAV_FIELD);
                    }
                    
                    // history information
                    if (contextInformation) {
                        LOGGER.fine("Setting context information ");
                        atomicUpdate(idoc, true, FLAG_PUBLIC_IN_DL);
                    }
                    solr.add(DataCollections.catalog.name(), idoc);
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

}
