package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.GRANULARITY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_856_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_911_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_956_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SIGLA_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;import org.apache.commons.math3.ode.SecondOrderIntegrator;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient.Rsp;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.function.EqualFunction;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.utils.HistoryObjectUtils;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport;
import cz.inovatika.sdnnt.services.GranularityService;
import cz.inovatika.sdnnt.services.GranularitySetStateService;
import cz.inovatika.sdnnt.services.impl.utils.MarcUtils;
import cz.inovatika.sdnnt.services.impl.zahorikutils.ZahorikUtils;
import cz.inovatika.sdnnt.utils.JSONUtils;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class GranularitySetStateServiceImpl extends AbstractGranularityService implements GranularitySetStateService{

    private static final int MAX_FETCHED_DOCS = 1000;

    public static final int CHECK_SIZE = 70;

    private Logger logger = Logger.getLogger(GranularityService.class.getName());

    
    private Set<String> changedIdentifiers = new LinkedHashSet<>();
    private Set<String> duplicatesdentifiers = new LinkedHashSet<>();
    
    public GranularitySetStateServiceImpl(String logger) {
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


    /**
     *
     */
    @Override
    public void setStates(List<String> testFilters) throws IOException {
        try (final SolrClient solrClient = buildClient()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "10000");

            CatalogIterationSupport support = new CatalogIterationSupport();
            List<String> plusFilter = new ArrayList<>(Arrays.asList(
                MarcRecordFields.GRANULARITY_FIELD + ":*",
                "setSpec:SKC"
            ));
            
            
            if (!testFilters.isEmpty()) {
                testFilters.stream().forEach(plusFilter::add);
            }

            AtomicInteger counter = new AtomicInteger();
            
            List<String> minusFilter = Arrays.asList( KURATORSTAV_FIELD + ":D");
            support.iterate(solrClient, reqMap, null, plusFilter, minusFilter,
                    Arrays.asList(IDENTIFIER_FIELD, 
                            SIGLA_FIELD, 
                            MARC_911_U, 
                            MARC_956_U, 
                            MARC_856_U, 
                            GRANULARITY_FIELD,
                            MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD,
                            MarcRecordFields.HISTORIE_STAVU_FIELD,
                            
                            MarcRecordFields.DNTSTAV_FIELD,
                            MarcRecordFields.LICENSE_FIELD,
                            MarcRecordFields.FMT_FIELD,
                            "controlfield_008",
                            MarcRecordFields.RAW_FIELD,
                            MarcRecordFields.FMT_FIELD

                    ), (rsp) -> {

                        counter.incrementAndGet();
                        
                        AtomicBoolean changedGranularity = new AtomicBoolean();
                        
                        List<String> granularity = (List<String>) rsp.getFieldValue(GRANULARITY_FIELD);
                        List<JSONObject> granularityJSONS = granularity.stream().map(JSONObject::new).collect(Collectors.toList());
                        final Object masterIdentifier = rsp.getFirstValue(MarcRecordFields.IDENTIFIER_FIELD);
                        
                        // vycisti granularitu
                        Map<String, List<JSONObject>> xstates = new HashMap<>();
                        for (int i = 0; i < granularity.size(); i++) {
                            JSONObject iIterationObj = granularityJSONS.get(i);
                            if (iIterationObj.has("pid")) {
                                String pid = iIterationObj.optString("pid");
                                if (!xstates.containsKey(pid)) {
                                    xstates.put(pid, new ArrayList<>());
                                }
                                xstates.get(iIterationObj.optString("pid")).add(iIterationObj);
                            }
                            
                            if (iIterationObj.has("stav")) {
                                Object jStav = iIterationObj.get("stav");
                                String stav =  null;
                                if (jStav instanceof JSONArray) {
                                    JSONArray jStavArray  = (JSONArray) jStav;
                                    if (jStavArray.length() > 0) {
                                        stav = jStavArray.getString(0);
                                    } else {
                                        stav = null;
                                    }
                                } else  if(jStav instanceof String) {
                                    stav = (String) jStav;
                                }
                                
                                if (stav != null && !stav.toUpperCase().equals(PublicItemState.X.name())) {
                                    iIterationObj.remove("stav");
                                    iIterationObj.remove("kuratorstav");
                                    iIterationObj.remove("license");
                                } 
                            }
                        } 

                        
                        // XStates 
                        for (int i = 0; i < granularity.size(); i++) {
                            
                            JSONObject iIterationObj = granularityJSONS.get(i);
                            String pid = iIterationObj.optString("pid");
                            List<JSONObject> list = xstates.get(pid);
                            if (list != null) {

                                Set<String> set = list.stream().filter(obj->{
                                    //"acronym":"nkp"
                                    if (obj.has("acronym")) {
                                        boolean flag = Options.getInstance().boolKey("granularity.x_state."+obj.optString("acronym"), false);
                                        return flag;
                                    } else return false;
                                }).map(obj-> {
                                    if (obj.has("dostupnost")) {
                                        String dostupnost = obj.optString("dostupnost");
                                        return dostupnost;
                                    } else {
                                        return "private";
                                    }
                                }).collect(Collectors.toSet());
                                
                                if (set.size() == 1 && set.toArray()[0].equals("public")) {
                                    JSONArray jsonArray = new JSONArray();
                                    jsonArray.put("X");
                                    iIterationObj.put("stav", jsonArray);
                                    iIterationObj.put("kuratorstav", jsonArray);
                                    changedGranularity.set(true);
                                }
                            }
                        } 
                        
                        // associate the same 
                        Map<String, List<Pair<String, String>>> resolved = new HashMap<>();
                        for (int i = 0; i < granularity.size(); i++) {
                            JSONObject iIterationObj = granularityJSONS.get(i);

                            String pid = null;
                            String stav = null;
                            String license = null;

                            if (iIterationObj.has("link")) {
                                pid= PIDSupport.pidFromLink(iIterationObj.getString("link"));
                                pid = PIDSupport.pidNormalization(pid);
                                
                            }
                            
                            if (iIterationObj.has("stav")) {
                                Object jStav = iIterationObj.get("stav");
                                if (jStav instanceof JSONArray) {
                                    JSONArray jStavArray  = (JSONArray) jStav;
                                    if (jStavArray.length() > 0) {
                                        stav = jStavArray.getString(0);
                                    } else {
                                        stav = null;
                                    }
                                } else  if(jStav instanceof String) {
                                    stav = (String) jStav;
                                }
                            }
                            
                            if (iIterationObj.has("license")) {
                                license = iIterationObj.getString("license");
                            }

                            if (!resolved.containsKey(pid)) {
                                resolved.put(pid, new ArrayList<>());
                            }
                            if (stav != null) {
                                resolved.get(pid).add(Pair.of(stav,license));
                            }
                        }
                        
                        Map<String, List<JSONObject>> firstIterationNotResolved = new HashMap<>();
                        for (int i = 0; i < granularity.size(); i++) {
                            JSONObject iIterationObj = granularityJSONS.get(i);
                            if (iIterationObj.has("fetched")) {
                                String pid = null;
                                if (iIterationObj.has("link")) {
                                    pid= PIDSupport.pidFromLink(iIterationObj.getString("link"));
                                    pid = PIDSupport.pidNormalization(pid);
                                }
                                if (!iIterationObj.has("stav") || (iIterationObj.getJSONArray("stav").length() == 0)) {
                                    if (pid != null && !firstIterationNotResolved.containsKey(pid)) {
                                      firstIterationNotResolved.put(pid, new ArrayList<>());
                                    }
                                    firstIterationNotResolved.get(pid).add(iIterationObj);
                                }
                            }
                        }
                        
                        Map<String, List<JSONObject>> secondterationNotResolved = new HashMap<>();
                        for(String key: firstIterationNotResolved.keySet()) {
                            if (resolved.containsKey(key)) {
                                List<Pair<String, String>> resolvedList = resolved.get(key);
                                Pair<String,String> pair = onlyOne(resolvedList);
                                if (pair == null) {
                                    pair = same(resolvedList);
                                } 
                                if (pair == null) {
                                    secondterationNotResolved.put(key, firstIterationNotResolved.get(key));
                                } else {
                                    List<JSONObject> list = firstIterationNotResolved.get(key);
                                    for (JSONObject gItemJSON : list) {
                                        
                                        JSONArray stavArr = new JSONArray();
                                        stavArr.put(pair.getLeft());
                                        gItemJSON.put("stav", stavArr);
                                        gItemJSON.put("kuratorstav", stavArr);
                                      
                                        if(pair.getRight() != null) {
                                            gItemJSON.put("license",pair.getRight());
                                        }
                                    }
                                    
                                    changedGranularity.set(true);
                                    changedIdentifiers.add(masterIdentifier.toString());
                                }
                            }
                        }

                        
                        if (!secondterationNotResolved.isEmpty()) {

                            String nState = (String) rsp.getFirstValue(MarcRecordFields.DNTSTAV_FIELD);
                            String controlField = (String) rsp.getFirstValue("controlfield_008");
                            String fmt = (String) rsp.getFirstValue(MarcRecordFields.FMT_FIELD);
                            String license = (String) rsp.getFirstValue(MarcRecordFields.LICENSE_FIELD);
                            

                            if (nState.equals(PublicItemState.X.name())) {
                                Set<String> keySet = secondterationNotResolved.keySet();
                                for(String key: keySet) {
                                    List<JSONObject> list = secondterationNotResolved.get(key);
                                    for (int i = 0; i < list.size(); i++) {
                                        JSONObject gItemJSON = list.get(i);
                                        JSONArray stavArr = new JSONArray();
                                        stavArr.put(PublicItemState.X.name());

                                        gItemJSON.put("stav", stavArr);
                                        gItemJSON.put("kuratorstav", stavArr);
                                        gItemJSON.remove("license");
                                        
                                    }
                                }
                                changedGranularity.set(true);
                                changedIdentifiers.add(masterIdentifier.toString());
                            } 

                            if (nState.equals(PublicItemState.N.name())) {
                                Set<String> keySet = secondterationNotResolved.keySet();
                                for(String key: keySet) {
                                    List<JSONObject> list = secondterationNotResolved.get(key);
                                    for (int i = 0; i < list.size(); i++) {
                                        JSONObject gItemJSON = list.get(i);
                                        JSONArray stavArr = new JSONArray();
                                        stavArr.put(PublicItemState.N.name());

                                        gItemJSON.put("stav", stavArr);
                                        gItemJSON.put("kuratorstav", stavArr);
                                        gItemJSON.remove("license");
                                        
                                    }
                                }
                                changedGranularity.set(true);
                                changedIdentifiers.add(masterIdentifier.toString());
                            } 
                            
                            if (nState.equals(PublicItemState.N.name())  || nState.equals(PublicItemState.X.name()) ) {
                                /*
                                Set<String> keySet = secondterationNotResolved.keySet();
                                for(String key: keySet) {
                                    List<JSONObject> list = secondterationNotResolved.get(key);
                                    for (int i = 0; i < list.size(); i++) {
                                        JSONObject gItemJSON = list.get(i);
                                        JSONArray stavArr = new JSONArray();
                                        stavArr.put(PublicItemState.N.name());

                                        gItemJSON.put("stav", stavArr);
                                        gItemJSON.put("kuratorstav", stavArr);
                                        
                                    }
                                }
                                changedGranularity.set(true);
                                changedIdentifiers.add(masterIdentifier.toString());
                                */
                            } else {
                                
                                // kniha; 
                                if (fmt != null && fmt.equals("BK")) {
                                    Set<String> keySet = secondterationNotResolved.keySet();
                                    for(String key: keySet) {
                                        List<JSONObject> items = secondterationNotResolved.get(key);
                                        if (nState.equals(PublicItemState.A.name()) || nState.equals(PublicItemState.PA.name())) {
                                            if (license != null && license.equals(License.dnnto.name())) {
                                                ZahorikUtils.BK_DNNTO(nState, license, items, getLogger());
                                            } else if (license != null && license.equals(License.dnntt.name())) {
                                                ZahorikUtils.BK_DNNTT(nState, license, items, getLogger());
                                            }
                                        } else {
                                            for (JSONObject gItem : items) {
                                                JSONArray stavArr = new JSONArray();
                                                stavArr.put(nState);
                                                gItem.put("stav", stavArr);
                                                gItem.put("kuratorstav", stavArr);
                                            }
                                        }
                                    }
                                    changedIdentifiers.add(masterIdentifier.toString());
                                
                                // serialy 
                                } else if (fmt != null && fmt.equals("SE")) {
                                    Set<String> keySet = secondterationNotResolved.keySet();
                                    for(String key: keySet) {
                                        // pokud je dnntt, netreba zjistovat 
                                        List<JSONObject> items = secondterationNotResolved.get(key);
                                        if (license != null && license.equals(License.dnntt.name())) {
                                            ZahorikUtils.SE_1_DNNTT(nState, license, items, getLogger());
                                        } else if (license != null && license.equals(License.dnnto.name())){
                                            // rozhodnuti zda to jsou neprava periodika 
                                            boolean regularityFlag = false;
                                            char regularity = controlField.charAt(19);
                                            
                                            String regularityPattern = Options.getInstance().stringKey("granularity.se.00819", "r");
                                            String[] rsplit = regularityPattern.split(",");
                                            for (String confPattern : rsplit) {
                                                if (confPattern.length() > 0 &&  confPattern.charAt(0) == regularity) {
                                                    regularityFlag = true;
                                                    break;
                                                }
                                            }
                                            if (regularityFlag) {
                                                boolean regularSE = false;
                                                String frequencyPattern = Options.getInstance().stringKey("granularity.se.00818", "b,c,d,e,f,j,q,t,s,m,w,z");
                                                char periodicity = controlField.charAt(18);
                                                String[] psplit = frequencyPattern.split(",");
                                                for (String confPattern : psplit) {
                                                    if (confPattern.length() > 0 &&  confPattern.charAt(0) == periodicity) {
                                                        regularSE = true;
                                                        break;
                                                    }
                                                }
                                                if (regularSE) {
                                                    ZahorikUtils.SE_2_DNNTO(nState, license, items, getLogger());
                                                } else {
                                                    ZahorikUtils.SE_1_DNNTO(nState, license, items, getLogger());
                                                }
                                            } else {
                                                ZahorikUtils.SE_1_DNNTO(nState, license, items, getLogger());
                                            }
                                        } else {
                                            getLogger().warning("No license for "+masterIdentifier);
                                        }
                                    }
                                    changedIdentifiers.add(masterIdentifier.toString());
                                }
                            }
                            changedGranularity.set(true);
                        }
                        
                        
                        if (changedGranularity.get()) {
                            String history = (String) rsp.getFieldValue(MarcRecordFields.HISTORIE_STAVU_FIELD);
                            JSONObject lastHistoryObject = null;
                            if (history !=  null) {
                                JSONArray jsonArray = new JSONArray(history);
                                if (jsonArray.length() > 0) {
                                    lastHistoryObject = jsonArray.getJSONObject(jsonArray.length()-1);
                                }
                            }
                            
                            List<String> origGranularity = (List<String>) rsp.getFieldValue(GRANULARITY_FIELD);
                            Map<String,Pair<String,JSONObject>> origMap = new HashMap<>();
                            origGranularity.stream().map(JSONObject::new).forEach(origJSON-> {
                                String pid = origJSON.optString("pid");
                                String acronym = origJSON.optString("acronym","null");
                                
                                String key = String.format("%s|%s", pid, acronym);

                                String stav = JSONUtils.first(origJSON, "stav");
                                String kuratorStav = JSONUtils.first(origJSON, "kuratorstav");
                                String license = origJSON.optString("license","null");
                                    
                                String value = String.format("%s|%s|%s", stav, kuratorStav, license);
                                origMap.put(key, Pair.of(value, origJSON));
                            });
                            
                            SolrInputDocument idoc = new SolrInputDocument();
                            idoc.setField(IDENTIFIER_FIELD, masterIdentifier);
                            // json diff  
                            List<String> gStore = granularityJSONS.stream().map(JSONObject::toString).collect(Collectors.toList());
                            Map<String,Pair<String,JSONObject>> changedMap = new HashMap<>();
                            granularityJSONS.stream().forEach(changedJSON-> {

                                String pid = changedJSON.optString("pid");
                                String acronym = changedJSON.optString("acronym","null");
                                
                                String key = String.format("%s|%s", pid, acronym);

                                String stav = JSONUtils.first(changedJSON, "stav");
                                String kuratorStav = JSONUtils.first(changedJSON, "kuratorstav");
                                String license = changedJSON.optString("license","null");
                                    
                                String value = String.format("%s|%s|%s", stav, kuratorStav, license);
                                changedMap.put(key, Pair.of(value, changedJSON));
                            });
                            
                            
                            List<JSONObject> historyAdd = compare(origMap, changedMap, lastHistoryObject != null ? lastHistoryObject.optString("comment"): null,  lastHistoryObject != null ? lastHistoryObject.optString("user"): "granularity");
                            if (!historyAdd.isEmpty()) {
                                String historie = (String) rsp.getFirstValue(MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD);
                                JSONArray historieGranulovanehoStavu = historie != null ? new JSONArray(historie) : new JSONArray();
                                for (JSONObject historyItem : historyAdd) { historieGranulovanehoStavu.put(historyItem); }
                                atomicSet(idoc, historieGranulovanehoStavu.toString(), MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD);
                            }
                            
                            // porovnat rocniky, pokud zmena pak zapis 
                            // historie ?? historie rucniku
                            atomicSet(idoc, gStore, MarcRecordFields.GRANULARITY_FIELD);
                            
                            
                            try {
                                solrClient.add(DataCollections.catalog.name(), idoc);
                            } catch (SolrServerException | IOException e) {
                                getLogger().log(Level.SEVERE,e.getMessage(),e);
                            }
                        }

                }, IDENTIFIER_FIELD);
        }
        
        
        try (final SolrClient solrClient = buildClient()) {
            SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
        }
        logger.info("Changed states finished. Updated identifiers ("+this.changedIdentifiers.size()+") "+this.changedIdentifiers);
    }

    private List<JSONObject> compare(Map<String, Pair<String, JSONObject>> origMap, Map<String, Pair<String, JSONObject>> changedMap, String masterComment, String user) {
        List<JSONObject> history = new ArrayList<>();
        Set<String> origKeys = origMap.keySet();
        Set<String> changedKeys = changedMap.keySet();

        List<Pair<Pair<String,JSONObject>, Pair<String,JSONObject>>> toCompare = new ArrayList<>();

        for (String origkey : origKeys) {
            if (changedKeys.contains(origkey)) {
                toCompare.add(Pair.of(origMap.get(origkey), changedMap.get(origkey)));
            } else {
                Pair<String, JSONObject> pair = origMap.get(origkey);
                if (pair != null) {
                    JSONObject granitem = pair.getRight().put(HistoryObjectUtils.STAV_FIELD, PublicItemState.D.name());
                    
                    String stav = JSONUtils.first(granitem, "stav");
                    String kuratorStav = JSONUtils.first(granitem, "kuratorstav");
                    String license = granitem.optString("license");
                    String rocnik = granitem.optString("rocnik");
                    String date = granitem.optString("date");
                    String cislo = granitem.optString("cislo");

                    history.add(HistoryObjectUtils.historyObjectGranularityField(cislo, rocnik, stav, license, null, user, masterComment, MarcRecord.FORMAT.format(new Date())));
                }
            }
        }
        for (String changedKey : changedKeys) {
            if (!origKeys.contains(changedKey)) {

                JSONObject granitem = changedMap.get(changedKey).getValue();

                String stav = JSONUtils.first(granitem, "stav");
                String kuratorStav = JSONUtils.first(granitem, "kuratorstav");
                String license = granitem.optString("license");
                String rocnik = granitem.optString("rocnik");
                String date = granitem.optString("date");
                String cislo = granitem.optString("cislo");
                
                
                history.add(HistoryObjectUtils.historyObjectGranularityField(cislo, rocnik, stav, license, null, user, masterComment, MarcRecord.FORMAT.format(new Date())));
            }
        }
         
        for (Pair<Pair<String,JSONObject>, Pair<String,JSONObject>> cmp : toCompare) {
            Pair<String,JSONObject> left = cmp.getLeft();
            Pair<String,JSONObject> right = cmp.getRight();
            
            if (!left.getKey().equals(right.getKey())) {
                
                JSONObject granitem = right.getValue();

                String stav = JSONUtils.first(granitem, "stav");
                String kuratorStav = JSONUtils.first(granitem, "kuratorstav");
                String license = granitem.optString("license");
                String rocnik = granitem.optString("rocnik");
                String date = granitem.optString("date");
                String cislo = granitem.optString("cislo");
                
                
                history.add(HistoryObjectUtils.historyObjectGranularityField( cislo, rocnik, stav, license, null, user, masterComment, MarcRecord.FORMAT.format(new Date())));
            }
        }
        return history;
    }

    private Pair<String, String> same(List<Pair<String, String>> resolvedList) {
        if (resolvedList.size() >= 1) {
            
            List<Pair<String, String>> filtered = resolvedList.stream().filter(p-> {
                String stav = p.getLeft();
                if(stav != null && stav.equals("X")) return false;
                else return true;
            }).collect(Collectors.toList());

            Set<Integer> collected = filtered.stream().map(Object::hashCode).collect(Collectors.toSet());
            if(collected.size() == 1) {
                return resolvedList.get(0);
            } else return null;
        } else return null;
    }

    private Pair<String, String> onlyOne(List<Pair<String, String>> resolvedList) {
        if (resolvedList.size() == 1) {
            return resolvedList.get(0);
        } else {
            return null;
        }
    }

    
    public static void main(String[] args) throws IOException {
        GranularitySetStateServiceImpl service = new GranularitySetStateServiceImpl(null);
        service.setStates(new ArrayList<>());
    }


}
