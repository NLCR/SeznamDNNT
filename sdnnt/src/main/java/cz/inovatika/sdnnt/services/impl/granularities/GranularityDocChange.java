package cz.inovatika.sdnnt.services.impl.granularities;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.utils.HistoryObjectUtils;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport;
import cz.inovatika.sdnnt.services.impl.zahorikutils.ZahorikUtils;
import cz.inovatika.sdnnt.utils.JSONUtils;

public abstract class GranularityDocChange {
    
    protected Logger logger = Logger.getLogger(GranularityDocChange.class.getName());
    
    protected abstract JSONArray getHistorieGranulovanehoStavu();
    protected abstract JSONArray getHistorieStavu();
    protected abstract JSONArray getGranularity();

    protected abstract String getLicense();
    protected abstract String getFMTField();
    protected abstract String getControl008();
    protected abstract String getDNTStav();
    protected abstract String getIdentifier();

    
    
    public GranularityDocChange(Logger logger) {
        super();
        if (logger != null)  this.logger = logger;
        else this.logger = Logger.getLogger(GranularityDocChange.class.getName());
    }
    
    
    
    public boolean processOneDoc( List<JSONObject> granularityJSONS, final Object masterIdentifier) {
        AtomicBoolean changedGranularity  = new AtomicBoolean();

        // vycisti granularitu - no x
        Map<String, List<JSONObject>> xstates = new HashMap<>();
        for (int i = 0; i < granularityJSONS.size(); i++) {
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
                // odebiram granularitu ve vsech pripadech 
                if (stav != null) {
                    iIterationObj.remove("stav");
                    iIterationObj.remove("kuratorstav");
                    iIterationObj.remove("license");
                } 
            }
        } 

        
        // XStates 
        for (int i = 0; i < granularityJSONS.size(); i++) {
            
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
        for (int i = 0; i < granularityJSONS.size(); i++) {
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
        for (int i = 0; i < granularityJSONS.size(); i++) {
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
                    //changedIdentifiers.add(masterIdentifier.toString());
                }
            }
        }

        
        if (!secondterationNotResolved.isEmpty()) {

            String nState = getDNTStav();
            String controlField = getControl008();
            String fmt = getFMTField();
            String license = getLicense();
            

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
                //changedIdentifiers.add(masterIdentifier.toString());
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
                //changedIdentifiers.add(masterIdentifier.toString());
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
                        if (nState.equals(PublicItemState.A.name()) || nState.equals(PublicItemState.PA.name()) || nState.equals(PublicItemState.NL.name())) {
                            if (license != null && license.equals(License.dnnto.name())) {
                                ZahorikUtils.BK_DNNTO(nState, license, items, logger);
                            } else if (license != null && license.equals(License.dnntt.name())) {
                                ZahorikUtils.BK_DNNTT(nState, license, items, logger);
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
                    changedGranularity.set(true);
                    //changedIdentifiers.add(masterIdentifier.toString());
                
                // serialy 
                } else if (fmt != null && fmt.equals("SE")) {
                    Set<String> keySet = secondterationNotResolved.keySet();
                    for(String key: keySet) {
                        // pokud je dnntt, netreba zjistovat 
                        List<JSONObject> items = secondterationNotResolved.get(key);
                        if (license != null && license.equals(License.dnntt.name())) {
                            ZahorikUtils.SE_1_DNNTT(nState, license, items, logger);
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
                                    ZahorikUtils.SE_2_DNNTO(nState, license, items, logger);
                                } else {
                                    ZahorikUtils.SE_1_DNNTO(nState, license, items, logger);
                                }
                            } else {
                                ZahorikUtils.SE_1_DNNTO(nState, license, items, logger);
                            }
                        } else {
                            logger.warning("No license for "+masterIdentifier);
                        }
                    }
                }
            }
            changedGranularity.set(true);
        }
        
        return changedGranularity.get();
    }

    
    public void atomicChange(List<JSONObject> granularityJSONS,
            final Object masterIdentifier, String originator) {
        JSONArray history = getHistorieStavu();
        JSONObject lastHistoryObject = null;
        if (history !=  null) {
            if (history.length() > 0) {
                lastHistoryObject = history.getJSONObject(history.length()-1);
            }
        }
        
        JSONArray origGranularity = this.getGranularity();
        
        Map<String,Pair<String,JSONObject>> origMap = new HashMap<>();
        origGranularity.forEach(orig -> {
            JSONObject origJSON = (JSONObject) orig;
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
        
        
        List<JSONObject> historyAdd = compare(origMap, changedMap, lastHistoryObject != null ? lastHistoryObject.optString("comment"): null,  lastHistoryObject != null ? lastHistoryObject.optString("user"): "granularity", originator);
        String changedHistory = null;
        if (!historyAdd.isEmpty()) {
            JSONArray historieGranulovanehoStavu = getHistorieGranulovanehoStavu();
            // TODO: groupovat
            for (JSONObject historyItem : historyAdd) { historieGranulovanehoStavu.put(historyItem); }
            changedHistory =  historieGranulovanehoStavu.toString();
        }
        
        setGranularity(gStore, changedHistory);
    }


    //protected abstract void setHistorieGranulovanehoStavu(String historieGranulovanehoStavu);
    protected abstract void setGranularity(List<String> granularity, String historieGranulovanehoStavu);

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
    
    protected List<JSONObject> compare(Map<String, Pair<String, JSONObject>> origMap, Map<String, Pair<String, JSONObject>> changedMap, String masterComment, String user, String originator) {
        String groupId = System.currentTimeMillis()+"";
        
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
                    
                    String link = JSONUtils.first(granitem, "link");
                    String acronym = JSONUtils.first(granitem, "acronym");
                    String pid = JSONUtils.first(granitem, "pid");

                    history.add(HistoryObjectUtils.historyObjectGranularityField(
                            cislo, 
                            rocnik, 
                            stav, 
                            license, 
                            originator, 
                            user, 
                            masterComment, 
                            MarcRecord.FORMAT.format(new Date()), 
                            groupId,

                            acronym,
                            pid,
                            link

                            ));
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
                
                
                String link = JSONUtils.first(granitem, "link");
                String acronym = JSONUtils.first(granitem, "acronym");
                String pid = JSONUtils.first(granitem, "pid");
                
//                String acronym, 
//                String pid, 
//                String link
                
                history.add(HistoryObjectUtils.historyObjectGranularityField(
                        cislo, 
                        rocnik, 
                        stav, 
                        license, 
                        originator, 
                        user, 
                        masterComment, 
                        MarcRecord.FORMAT.format(new Date()), 
                        
                        groupId,
                        acronym,
                        pid,
                        link
                        
                        
                        ));
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
                
                String link = JSONUtils.first(granitem, "link");
                String acronym = JSONUtils.first(granitem, "acronym");
                String pid = JSONUtils.first(granitem, "pid");

                history.add(HistoryObjectUtils.historyObjectGranularityField( 
                        cislo, 
                        rocnik, 
                        stav, 
                        license, 
                        originator, 
                        user, 
                        masterComment, 
                        MarcRecord.FORMAT.format(new Date()),

                        groupId,
                        acronym,
                        pid,
                        link

                        
                        ));
            }
        }
        return history;
    }


}
