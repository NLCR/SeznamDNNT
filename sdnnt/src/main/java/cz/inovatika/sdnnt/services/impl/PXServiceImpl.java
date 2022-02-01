package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.InitServlet;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.ZadostTyp;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.PXService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.hackcerts.HttpsTrustManager;
import cz.inovatika.sdnnt.utils.SimpleGET;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

public class PXServiceImpl implements PXService {

    public static final Logger LOGGER = Logger.getLogger(PXService.class.getName());

    public static final int CHECK_SIZE = 10;
    public static final int LIMIT = 1000;

    private Map<String,String> mappingHosts = new HashMap<>();
    private Map<String,String> mappingApi = new HashMap<>();

    private String yearConfiguration = null;

    public PXServiceImpl(String yearConfiguration) {
        this.yearConfiguration = yearConfiguration;
    }

    protected void initialize() {
        JSONObject checkKamerius = Options.getInstance().getJSONObject("check_kramerius");
        if (checkKamerius != null && checkKamerius.has("urls")) {
            JSONObject urlobject = checkKamerius.getJSONObject("urls");
            Set<String> urls = urlobject.keySet();
            for (String key : urls) {
                JSONObject jObject = urlobject.getJSONObject(key);
                String api = jObject.optString("api");
                if (api != null)   {
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
        this.initialize();
        List<String> foundCandidates = new ArrayList<>();
        Map<String,List<String>> mapping = new HashMap<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String,String> reqMap = new HashMap<>();
        reqMap.put("rows", ""+LIMIT);

        List<String> plusFilter = new ArrayList<>(Arrays.asList("id_pid:uuid","fmt:BK"));

        if (yearConfiguration != null) {
            plusFilter.add("rokvydani:"+ yearConfiguration);
        }
        LOGGER.info("Current iteration filter "+plusFilter);
        support.iterate(reqMap, null, plusFilter, Arrays.asList("dntstav:X", "dntstav:PX"), Arrays.asList(
                IDENTIFIER_FIELD,
                SIGLA_FIELD,
                MARC_911_U,
                MARC_956_U,
                GRANULARITY_FIELD
        ), (rsp) -> {
            Object identifier = rsp.getFieldValue("identifier");

            Collection<Object> links1 = rsp.getFieldValues(MARC_911_U);
            Collection<Object> links2 = rsp.getFieldValues(MARC_956_U);

            if (links1 != null && !links1.isEmpty()) {
                List<String> ll = links1.stream().map(Object::toString).collect(Collectors.toList());
                mapping.put(identifier.toString(), ll);
            } else if (links2 != null && !links2.isEmpty()) {
                List<String> ll = links2.stream().map(Object::toString).collect(Collectors.toList());
                mapping.put(identifier.toString(), ll);
            }
        }, IDENTIFIER_FIELD);


        try (final SolrClient solr = buildClient()) {

            List<String> keys = new ArrayList<>(mapping.keySet());
            int batchSize = 40;
            int numberOfBatches = keys.size() / batchSize;
            if (keys.size() % batchSize > 0) {
                numberOfBatches += 1;
            }
            for (int i = 0; i < numberOfBatches; i++) {
                int startIndex = i*batchSize;
                int endIndex = (i+1)*batchSize;
                List<String> batch = keys.subList(startIndex, Math.min(endIndex, keys.size()));
                Set<String> used = usedInRequest(solr, batch);
                used.stream().forEach(mapping::remove);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }

        Map<String, List<Pair<String,String>>> buffer = new HashMap<>();
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
            checkBuffer(buffer, foundCandidates);
        }
        return foundCandidates;
    }

    public static Set<String>  usedInRequest(SolrClient solr, List<String> identifiers) {
        try {
            Set<String> retval = new HashSet<>();
            SolrQuery query = new SolrQuery("*")
                    .setFields("id","identifiers")
                    .addFilterQuery("navrh:PXN")
                    .setRows(3000);

            String collected = identifiers.stream().map(id -> '"' + id + '"').collect(Collectors.joining(" OR "));
            query.addFilterQuery("identifiers:("+collected+")");

            SolrDocumentList zadost = solr.query("zadost", query).getResults();
            zadost.stream().forEach(solrDoc-> {
                Collection<Object> identsFromZadost = solrDoc.getFieldValues("identifiers");
                identsFromZadost.stream().map(Object::toString).forEach(foundInZadost-> {
                    if (identifiers.contains(foundInZadost)) {
                        retval.add(foundInZadost);
                    }
                });

            });
            return retval;
        } catch (SolrServerException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }
        return new HashSet<>();
    }

    private void checkBuffer( Map<String, List<Pair<String,String>>> buffer , List<String> foundCadidates)  {
        Integer sum = buffer.values().stream().map(List::size).reduce(0, Integer::sum);
        if (sum > CHECK_SIZE ) {
            try {
                for (String baseUrl :  buffer.keySet()) {
                    if (baseUrl == null) continue;
                    List<Pair<String, String>> pairs = buffer.get(baseUrl);
                    String condition = pairs.stream().map(Pair::getRight).filter(Objects::nonNull).map(p -> {
                        return p.replace(":", "\\:");
                    }).collect(Collectors.joining(" OR "));

                    if(!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl+"/";
                    }


                    String encodedCondition = URLEncoder.encode("PID:(" + condition + ")", "UTF-8");
                    String url = baseUrl + "api/v5.0/search?q="+"PID:(" + encodedCondition + ")"+"&wt=json";

                    try {
                        String result = SimpleGET.get(url);
                        JSONObject object = new JSONObject(result);
                        JSONObject response = object.getJSONObject("response");
                        JSONArray jsonArray = response.getJSONArray("docs");
                        for (int i=0,ll=jsonArray.length();i<ll;i++) {
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
                                        LOGGER.info(String.format("Found public document. Identifier %s and pid %s",  pair.getLeft(), pair.getRight()));
                                        foundCadidates.add(pair.getLeft());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(),e);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }

            buffer.clear();
        }
    }

    private static String pid(String surl) {
        if (surl.contains("uuid:")){
            return surl.substring(surl.indexOf("uuid:"));
        } else return null;
    }

    String baseUrl(String surl) {
        if (surl.contains("/search/")){
            String val = surl.substring(0, surl.indexOf("/search")+"/search".length());
            if (findByPrefix(val)) return this.mappingHosts.get(val);
            return val;
        } else {
            List<String> prefixes = Arrays.asList("view", "uuid");
            for (String pref : prefixes) {
                if (surl.contains(pref)){
                    String remapping = surl.substring(0, surl.indexOf(pref));
                    if (findByPrefix(remapping)){
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
                return  true;
            }
        }
        return false;
    }




    @Override
    public void request(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException {
        if (!identifiers.isEmpty()) {
            ApplicationUserLoginSupport appSupport = new StaticApplicationLoginSupport(getSchedulerUser());
            AccountService accountService = new AccountServiceImpl(appSupport, null);
            JSONObject px = accountService.prepare("PXN");
            Zadost zadost = Zadost.fromJSON(px.toString());
            zadost.setTypeOfRequest(ZadostTyp.scheduler.name());
            identifiers.stream().forEach(zadost::addIdentifier);
            zadost.setState("waiting");
            accountService.schedulerDefinedCloseRequest(zadost.toJSON().toString());
            LOGGER.info("Requests sent");
        }
    }


    private static User getSchedulerUser() {
        User user = new User();
        user.setJmeno("scheduler");
        user.setPrijmeni("scheduler");
        user.setUsername("scheduler");
        return user;
    }

    static SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
