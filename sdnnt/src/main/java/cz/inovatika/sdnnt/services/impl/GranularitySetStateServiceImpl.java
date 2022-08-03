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
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport;
import cz.inovatika.sdnnt.services.GranularityService;
import cz.inovatika.sdnnt.services.GranularitySetStateService;
import cz.inovatika.sdnnt.services.impl.utils.MarcUtils;
import cz.inovatika.sdnnt.services.impl.zahorikutils.ZahorikUtils;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
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
                MarcRecordFields.GRANULARITY_FIELD + ":*"
            ));
            
            
            if (!testFilters.isEmpty()) {
                testFilters.stream().forEach(plusFilter::add);
            }

            AtomicInteger counter = new AtomicInteger();
            
            List<String> minusFilter = Arrays.asList(KURATORSTAV_FIELD + ":X", KURATORSTAV_FIELD + ":D");
            support.iterate(solrClient, reqMap, null, plusFilter, minusFilter,
                    Arrays.asList(IDENTIFIER_FIELD, 
                            SIGLA_FIELD, 
                            MARC_911_U, 
                            MARC_956_U, 
                            MARC_856_U, 
                            GRANULARITY_FIELD,
                            MarcRecordFields.DNTSTAV_FIELD,
                            MarcRecordFields.LICENSE_FIELD,
                            MarcRecordFields.FMT_FIELD,
                            "controlfield_008",
                            MarcRecordFields.RAW_FIELD,
                            MarcRecordFields.FMT_FIELD

                    ), (rsp) -> {

                        counter.incrementAndGet();
                        
                        AtomicBoolean changedGranularity = new AtomicBoolean();
                        
                        final Object masterIdentifier = rsp.getFirstValue(MarcRecordFields.IDENTIFIER_FIELD);
                                
                        
                        List<String> granularity = (List<String>) rsp.getFieldValue(GRANULARITY_FIELD);
                        List<JSONObject> granularityJSONS = granularity.stream().map(JSONObject::new).collect(Collectors.toList());

                        // associated the same 
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
                                        
                                    }
                                }
                                changedGranularity.set(true);
                                changedIdentifiers.add(masterIdentifier.toString());
                            } else {

                                // kniha; 
                                if (fmt != null && fmt.equals("BK")) {
                                    Set<String> keySet = secondterationNotResolved.keySet();
                                    for(String key: keySet) {
                                        List<JSONObject> items = secondterationNotResolved.get(key);
                                        if (nState.equals(PublicItemState.A.name()) || nState.equals(PublicItemState.PA.name())) {
                                            if (license != null && license.equals(License.dnnto.name())) {
                                                ZahorikUtils.BK_DNNTO(nState, license, items);
                                            } else if (license != null && license.equals(License.dnntt.name())) {
                                                ZahorikUtils.BK_DNNTT(nState, license, items);
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
                                } else if (fmt != null && fmt.equals("SE")) {
                                    Set<String> keySet = secondterationNotResolved.keySet();
                                    for(String key: keySet) {
                                        // pokud je dnntt, netreba zjistovat 
                                        List<JSONObject> items = secondterationNotResolved.get(key);
                                        if (license != null && license.equals(License.dnntt.name())) {
                                            ZahorikUtils.SE_1_DNNTT(nState, license, items);
                                        } else if (license != null && license.equals(License.dnnto.name())){
                                            
                                            boolean regularityFlag = false;
                                            char regularity = controlField.charAt(19);
                                            String regularityPattern = Options.getInstance().getString("granularity.se.00818", "r");
                                            String[] rsplit = regularityPattern.split(",");
                                            for (String confPattern : rsplit) {
                                                if (confPattern.length() > 0 &&  confPattern.charAt(0) == regularity) {
                                                    regularityFlag = true;
                                                    break;
                                                }
                                            }
                                            if (regularityFlag) {
                                                boolean regularSE = false;
                                                String frequencyPattern = Options.getInstance().getString("granularity.se.00819", "b,c,d,e,f,j,q,t,s,m,w,z");
                                                char periodicity = controlField.charAt(18);
                                                String[] psplit = frequencyPattern.split(",");
                                                for (String confPattern : psplit) {
                                                    if (confPattern.length() > 0 &&  confPattern.charAt(0) == periodicity) {
                                                        regularSE = true;
                                                        break;
                                                    }
                                                }
                                                
                                                if (regularSE) {
                                                    ZahorikUtils.SE_2_DNNTO(nState, license, items);
                                                } else {
                                                    ZahorikUtils.SE_1_DNNTO(nState, license, items);
                                                }
                                            } else {
                                                ZahorikUtils.SE_1_DNNTO(nState, license, items);
                                            }
                                        } else {
                                            getLogger().warning("No license");
                                        }
                                    }
                                    changedIdentifiers.add(masterIdentifier.toString());
                                }
                            }
                            changedGranularity.set(true);
                        }
                        
                        
                        if (changedGranularity.get()) {
                            SolrInputDocument idoc = new SolrInputDocument();
                            idoc.setField(IDENTIFIER_FIELD, masterIdentifier);
                            List<String> gStore = granularityJSONS.stream().map(JSONObject::toString).collect(Collectors.toList());
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
        logger.info("Changed states finished. Updated identifiers "+this.changedIdentifiers);

        
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
        String regularityPattern = Options.getInstance().getString("granularity.se.00818", "r");
        String[] rsplit = regularityPattern.split(",");
        System.out.println(rsplit.length);
    }

}
