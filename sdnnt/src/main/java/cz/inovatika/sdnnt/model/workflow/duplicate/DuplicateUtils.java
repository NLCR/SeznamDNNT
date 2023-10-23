package cz.inovatika.sdnnt.model.workflow.duplicate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.index.utils.HistoryObjectUtils;
import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.ZadostProcess;
import cz.inovatika.sdnnt.model.workflow.MarcRecordDependencyStore;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.StringUtils;

/**
 * Duplicates utility class
 * @author happy
 */
public class DuplicateUtils {
    
    public static final boolean DEBUG = System.getProperty("duplicate.test") != null;
    
    
    private DuplicateUtils() {}

    
    public static final Logger LOGGER = Logger.getLogger(DuplicateUtils.class.getName());
    
    
    public static void changeRequests(MarcRecord origin, List<MarcRecord> followers, List<Zadost> zadosti) {
        String id = origin.identifier;
        List<String> followersIds = followers.stream().map(m-> m.identifier).collect(Collectors.toList());
        zadosti.stream().forEach(zadost-> {
            if (zadost.getState() != null && !zadost.getState().equals("processed")) {
               Map<String,ZadostProcess> process = zadost.getProcess() != null ? zadost.getProcess() : new HashMap<>();
               Map<String, ZadostProcess> changedProcess = new HashMap<>();
               
               // vymenit vse v zadosti
               if (zadost.getIdentifiers().contains(id)) {
                   // obsahuje 
                   List<String> changed = new ArrayList<>();
                   for (String zid : zadost.getIdentifiers()) {
                       if (zid.equals(id)) { 
                           // prida vsechny followers
                           changed.addAll(followersIds);
                           
                           for (String key : process.keySet()) {
                               if (key.startsWith(id)) {
                                   String diff = StringUtils.minus(key, id);
                                   followersIds.stream().forEach(fid-> {
                                       changedProcess.put(fid+diff, process.get(key));
                                   });
                               } else {
                                   changedProcess.put(key, process.get(key));
                               }
                           }
                       } else {
                           changed.add(zid);
                       }
                   }
                   zadost.setIdentifiers(changed);
                   zadost.setProcess(changedProcess);
                   
                   // uzavrit, pokud neni zadny identifikator
                   if (zadost.getIdentifiers() != null && zadost.getIdentifiers().isEmpty()) {
                       if (!zadost.getState().equals("open")) {
                           String poznamka = zadost.getPoznamka();
                           zadost.setPoznamka("Zruseno systemem - duplicita. ("+poznamka+")");
                           zadost.setState("processed");
                       }
                   }
               }
           }
       });
    }
    
    
    
    
    public static void moveProperties( MarcRecordDependencyStore depStore, MarcRecord origin, List<MarcRecord> followers, Consumer<MarcRecord> consumer) {
        // historie stavu, 
        // historie kuratorskeho stavu
        // stav
        // kurator stav
        // previous kurator stav
        // datum kurator stavu 
        // datum stavu
        // historie granulovaneho stavu 
        // granularity ?? 
        
        // "id_euipo_export":["inital-bk"],
        //"id_oaiident":["SKC01-000392643"],
        //"id_euipo":["euipo:0a715715-30eb-474a-a02b-3b6a0f6bd840"],
        //"export":["euipo"],
        
        followers.stream().forEach(follower-> {

            // verejny stav naslednika A,PA, NL a zaroven stav puvodce A, PA, NL, NPA  => Merge
            if (isOIPAcceptingPublic(follower.dntstav) && (isOIPAcceptingPublic(origin.dntstav) || isOIPAcceptingCuratorState(origin.historie_kurator_stavu))) {
                
                // do identifikatoru dat vsecchny predchazejici 
                List<String> mergedIdentifiers = new ArrayList<>();
                if (depStore != null &&  depStore.containsKey(follower.identifier)) {
                    List<String> idEuIpo = depStore.getMarcRecord(follower.identifier).stream().map(dpr -> {
                        return dpr.idEuipo;
                    }).flatMap(List::stream).collect(Collectors.toList());
                    
                    mergedIdentifiers.addAll(idEuIpo);
                }
                
                
                if (follower.idEuipo != null) {
                    mergedIdentifiers.addAll(follower.idEuipo);
                }
                
                if (origin.idEuipo != null) {
                    mergedIdentifiers.addAll(origin.idEuipo);
                }
                
                if (!mergedIdentifiers.isEmpty()) {
                    follower.idEuipo = new ArrayList<>(new LinkedHashSet<>(mergedIdentifiers));
                }
                
                
                List<String> mergedExports = new ArrayList<>();
                if (depStore != null &&  depStore.containsKey(follower.identifier)) {
                    List<String> idEuIpoExports = depStore.getMarcRecord(follower.identifier).stream().map(dpr -> {
                        return dpr.idEuipoExport;
                    }).flatMap(List::stream).collect(Collectors.toList());
                    
                    mergedExports.addAll(idEuIpoExports);
                }
                
                if (follower.idEuipoExport != null) {
                    mergedExports.addAll(follower.idEuipoExport);
                }
                if (origin.idEuipoExport != null) {
                    mergedExports.addAll(origin.idEuipoExport);
                }

                if (!mergedExports.isEmpty()) {
                    follower.idEuipoExport = mergedExports;
                }
                
                List<String> mergedFacets = new ArrayList<>();
                
                if (depStore != null &&  depStore.containsKey(follower.identifier)) {
                    List<String> mFacets = depStore.getMarcRecord(follower.identifier).stream().map(dpr -> {
                        return dpr.exportsFacets;
                    }).flatMap(List::stream).collect(Collectors.toList());
                    
                    mergedFacets.addAll(mFacets);
                }
                
                if (follower.exportsFacets != null) {
                    mergedFacets.addAll(follower.exportsFacets);
                }
                if (origin.exportsFacets != null) {
                    mergedFacets.addAll(origin.exportsFacets);
                }
                
                if (!mergedFacets.isEmpty()) {
                    follower.exportsFacets = new ArrayList<>(new LinkedHashSet<>( mergedFacets));
                }

            // Verejny stav puvodce A,PA,NL, NPA => Dedi
            } else if (isOIPAcceptingPublic(origin.dntstav) || isOIPAcceptingCuratorState(origin.historie_kurator_stavu)) {

                follower.idEuipo = new ArrayList<>(origin.idEuipo);
                follower.idEuipoExport = new ArrayList<>(origin.idEuipoExport);
                follower.exportsFacets = new ArrayList<>(origin.exportsFacets);
                
                List<String> mergedIdentifiers = new ArrayList<>();
                if (depStore != null &&  depStore.containsKey(follower.identifier)) {
                    List<String> idEuIpo = depStore.getMarcRecord(follower.identifier).stream().map(dpr -> {
                        return dpr.idEuipo;
                    }).flatMap(List::stream).collect(Collectors.toList());
                    
                    mergedIdentifiers.addAll(idEuIpo);

                    follower.idEuipo.addAll (new ArrayList<>(new LinkedHashSet<>(mergedIdentifiers)));
                }
                
                List<String> mergedFacets = new ArrayList<>();
                if (depStore != null &&  depStore.containsKey(follower.identifier)) {
                    List<String> mFacets = depStore.getMarcRecord(follower.identifier).stream().map(dpr -> {
                        return dpr.exportsFacets;
                    }).flatMap(List::stream).collect(Collectors.toList());
                    
                    mergedFacets.addAll(mFacets);
                }

            } /* else {
                
            }*/
            
            
            follower.dntstav = new ArrayList<>(origin.dntstav);
            follower.kuratorstav = new ArrayList<>(origin.kuratorstav);
            
            follower.datum_stavu = origin.datum_stavu != null ?  new Date(origin.datum_stavu.getTime()) : null;
            if (origin.historie_stavu !=  null) {
                follower.historie_stavu =  jsonArray(origin.historie_stavu);
            }
            //mr.historie_stavu = origin.historie_stavu != null ? ;
            if (origin.historie_kurator_stavu != null) {
                follower.historie_kurator_stavu = jsonArray(origin.historie_kurator_stavu);
            }
            
            if (origin.historie_granulovaneho_stavu != null) {
                follower.historie_granulovaneho_stavu = jsonArray(origin.historie_granulovaneho_stavu);
            }
            follower.license = origin.license;

            if (origin.licenseHistory != null) {
                follower.licenseHistory = new ArrayList<String>(origin.licenseHistory);
            }
            
            if (origin.granularity != null) {
                follower.granularity = jsonArray(origin.granularity);
            }
            // TODO:Remove; unused
            follower.previousDntstav = origin.previousDntstav;
            follower.previousKuratorstav = origin.previousKuratorstav;
           
            
            if (consumer != null) {
                consumer.accept(follower);
            }
        });
        
    }
    
    // kontrola, zda posledni v historii byl NPA ?? 
    
    public static boolean isOIPAcceptingCuratorState(JSONArray curatorStateHistory) {
        int length = curatorStateHistory.length();
        if (length > 1) {
            JSONObject historyItem = curatorStateHistory.getJSONObject(length-2);
            boolean has = historyItem.has("stav");
            if (has) {
                String stav = historyItem.getString("stav");
                return stav.equals(CuratorItemState.NPA.name());
            } else return false;
        }
        return false;
    }
    
    
    public static boolean isOIPAcceptingPublic(List<String> publicState) {
        List<String> mergeStates = Arrays.asList(
                PublicItemState.A.name(),
                PublicItemState.PA.name(),
                PublicItemState.NL.name()
        );
        if (publicState != null && publicState.size() > 0) {
            return mergeStates.contains(publicState.get(0));
        }
        return false;
    }
    





    private static JSONArray jsonArray(JSONArray source) {
        JSONArray dest = new JSONArray();
        for (Object object : source) {
            String str = object.toString();
            dest.put(new JSONObject(object.toString()));
        }
        return dest;
    }
    

    public static List<Triple<String, String, String>> findByMarcField(SolrClient solrClient, MarcRecord origin, Pair<String,String> marcFieldPair) throws SolrServerException, IOException {
        List<Triple<String, String, String>> retList = new ArrayList<>();
        List<DataField> marcField = origin.dataFields.get(marcFieldPair.getKey());
        if (marcField != null && !marcField.isEmpty()) {
            marcField.stream().forEach(mField -> {
                List<SubField> subfield = mField.subFields.get(marcFieldPair.getValue());
                if (subfield != null) {
                    // safra.. dat to jinam
                    StringBuilder builder = new StringBuilder("(");
                    for (int i = 0,ll=subfield.size(); i < ll; i++) {
                        if (i> 0) { builder.append(" OR "); }
                        builder.append('"').append(subfield.get(i).getValue()).append('"');
                    }
                    builder.append(")");
                    
                    SolrQuery idQuery = new SolrQuery(String.format("%s:%s", "marc_"+marcFieldPair.getKey()+marcFieldPair.getValue(), builder.toString())).setRows(100);
                    idQuery.addFilterQuery("NOT identifier:\""+origin.identifier+"\"");
                    if (!DEBUG) {
                        idQuery.addFilterQuery("NOT dntstav:D");
                        idQuery.addFilterQuery("NOT kuratorstav:DX");
                    } else {
                        idQuery.addFilterQuery("NOT setSpec:\"DNT-ALL\"");
                    }
                    idQuery.addFilterQuery("fmt:SE OR fmt:BK");

                    try {
                        LOGGER.info("Query: "+idQuery);
                        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
                        for (SolrDocument sDocument : results) {
                            Triple<String, String, String> triple = triple(sDocument);
                            retList.add(triple);
                        }
                    } catch (SolrServerException | IOException e) {
                        LOGGER.log(Level.SEVERE,e.getMessage(),e);
                    }
                }
            });
        }
        return retList;
    }

    public static List<Triple<String, String, String>> findCanceledActiveCCNB(SolrClient solrClient, MarcRecord origin) throws SolrServerException, IOException {
        List<Triple<String, String, String>> retList = new ArrayList<>();
        List<DataField> marcField = origin.dataFields.get("015");
        if (marcField != null && !marcField.isEmpty()) {
            marcField.stream().forEach(mField -> {
                List<SubField> subfield = mField.subFields.get("z");
                if (subfield != null) {
                    // safra.. dat to jinam
                    StringBuilder builder = new StringBuilder("(");
                    for (int i = 0,ll=subfield.size(); i < ll; i++) {
                        if (i> 0) { builder.append(" OR "); }
                        builder.append('"').append(subfield.get(i).getValue()).append('"');
                    }
                    builder.append(")");
                    
                    SolrQuery idQuery = new SolrQuery(String.format("%s:%s", "marc_015a", builder.toString())).setRows(1000);
                    idQuery.addFilterQuery("NOT identifier:\""+origin.identifier+"\"");
                    if (!DEBUG) {
                        idQuery.addFilterQuery("NOT dntstav:D");
                        idQuery.addFilterQuery("NOT kuratorstav:DX");
                    } else {
                        idQuery.addFilterQuery("NOT setSpec:\"DNT-ALL\"");
                    }
                    idQuery.addFilterQuery("fmt:SE OR fmt:BK");
                    try {
                        LOGGER.info("Query: "+idQuery);
                        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
                        for (SolrDocument sDocument : results) {
                            Triple<String, String, String> triple = triple(sDocument);
                            retList.add(triple);
                        }
                    } catch (SolrServerException | IOException e) {
                        LOGGER.log(Level.SEVERE,e.getMessage(),e);
                    }
                }
            });
        }
        return retList;
        
    }
    
    public static List<Triple<String, String, String>> findActiveCanceledCCNB(SolrClient solrClient, MarcRecord origin) throws SolrServerException, IOException {
        List<Triple<String, String, String>> retList = new ArrayList<>();
        List<DataField> marcField = origin.dataFields.get("015");
        if (marcField != null && !marcField.isEmpty()) {
            marcField.stream().forEach(mField -> {
                List<SubField> subfield = mField.subFields.get("a");
                if (subfield != null) {
                    // safra.. dat to jinam
                    StringBuilder builder = new StringBuilder("(");
                    for (int i = 0,ll=subfield.size(); i < ll; i++) {
                        if (i> 0) { builder.append(" OR "); }
                        builder.append('"').append(subfield.get(i).getValue()).append('"');
                    }
                    builder.append(")");
                    
                    SolrQuery idQuery = new SolrQuery(String.format("%s:%s", "marc_015z", builder.toString())).setRows(1000);
                    idQuery.addFilterQuery("NOT identifier:\""+origin.identifier+"\"");
                    if (!DEBUG) {
                        idQuery.addFilterQuery("NOT dntstav:D");
                        idQuery.addFilterQuery("NOT kuratorstav:DX");
                    } else {
                        //setSpec:"DNT-ALL"
                        idQuery.addFilterQuery("NOT setSpec:\"DNT-ALL\"");
                    }
                    idQuery.addFilterQuery("fmt:SE OR fmt:BK");
                    try {
                        LOGGER.info("Query: "+idQuery);
                        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
                        for (SolrDocument sDocument : results) {
                            Triple<String, String, String> triple = triple(sDocument);
                            retList.add(triple);
                        }
                    } catch (SolrServerException | IOException e) {
                        LOGGER.log(Level.SEVERE,e.getMessage(),e);
                    }
                }
            });
        }
        return retList;
    }

    private static Triple<String, String, String> triple(SolrDocument sDocument) {
        String identifier = sDocument.getFieldValue(MarcRecordFields.IDENTIFIER_FIELD).toString();
        String license = null;
        String dntstav = null;
        if (sDocument.containsKey(MarcRecordFields.LICENSE_FIELD)) {
            license = sDocument.getFirstValue(MarcRecordFields.LICENSE_FIELD).toString();
        }
        if (sDocument.containsKey(MarcRecordFields.DNTSTAV_FIELD)) {
            dntstav = sDocument.getFirstValue(MarcRecordFields.DNTSTAV_FIELD).toString();
        }
        Triple<String,String,String> triple = Triple.of(identifier, dntstav, license);
        return triple;
    }

    
    public static List<String> findBy910ax(SolrClient solrClient, String marc910a, String marc910x) throws SolrServerException, IOException {
        
        String combination = marc910a +marc910x;
        
        Set<String> identifiers = new LinkedHashSet<>();
        SolrQuery idQuery = new SolrQuery("*").setRows(100);
        idQuery = idQuery.addFilterQuery(String.format("marc_910a:%s AND marc_910x:%s",marc910a, marc910x));
        idQuery = idQuery.addFilterQuery("fmt:SE OR fmt:BK").setRows(1000);
        LOGGER.info("Query: "+idQuery);
        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
        
        for (SolrDocument sDocument : results) {
            List<String> fCombinations = new ArrayList<>();
                
            String fValue = sDocument.getFieldValue("raw").toString();
            JSONObject rawJSON = new JSONObject(fValue);
            
            JSONObject dataFields = rawJSON.getJSONObject("dataFields");
            if (dataFields.has("910")) {
                JSONArray array910 = dataFields.getJSONArray("910");
                for (int i = 0; i < array910.length(); i++) {
                    JSONObject item910 = array910.getJSONObject(i);
                    if (item910.has("subFields")) {
                        JSONObject subFields = item910.getJSONObject("subFields");
                        if (subFields.has("a") && subFields.has("x")) {
                            String aVal = subFields.getJSONArray("a").getJSONObject(0).getString("value");
                            JSONArray xJSONArray = subFields.getJSONArray("x");
                            for (int j = 0; j < xJSONArray.length(); j++) {
                                JSONObject xObject = xJSONArray.getJSONObject(j);
                                fCombinations.add(aVal+xObject.getString("value"));
                            }
                        }
                    }
                }
            }
            
            if (fCombinations.contains(combination)) {
                Object fieldValue = sDocument.getFieldValue("identifier");
                identifiers.add(fieldValue.toString());
            }
        }
        
        List<String> retvals = new ArrayList<>(identifiers);
        return retvals;
    }    
    
    public static List<Triple<String,String,String>> findFollowersBy910ax(SolrClient solrClient, MarcRecord origin, Pair<String,String> cartesianLeft, Pair<String,String> cartesianRight) throws SolrServerException, IOException {
        Set<String> identifiers = new HashSet<>();
        List<Triple<String,String,String>> retList = new ArrayList<>();
        
        List<String> combinations = new ArrayList<>();
        List<DataField> originDFields = origin.dataFields.get("910");
        if (originDFields == null) return new ArrayList<>();
        for (DataField oDField : originDFields) {
            if (oDField.getSubFields().containsKey("a") && oDField.getSubFields().containsKey("x")) {
                List<SubField> aList = oDField.getSubFields().get("a");
                List<SubField> xList = oDField.getSubFields().get("x");
                for (SubField xSubField : xList) {
                    combinations.add(aList.get(0).getValue()+xSubField.getValue());
                }
            }
        }
        
        List<DataField> cartesianLeftFields = origin.dataFields.get(cartesianLeft.getKey());
        List<DataField> cartesianRightFields = origin.dataFields.get(cartesianRight.getKey());
        
        if (cartesianLeftFields != null && !cartesianLeftFields.isEmpty() && cartesianRightFields != null && !cartesianRightFields.isEmpty()) {
            cartesianLeftFields.stream().forEach(cF-> {
                List<SubField> cFSubfields =cF.subFields.get(cartesianLeft.getValue());
                if (cFSubfields != null) {
                    cFSubfields.stream().forEach(left-> {
                        String code = cartesianRight.getValue();
                        
                        cartesianRightFields.stream().forEach(r-> {
                           List<SubField> list = r.getSubFields().get(code);
                           if (list != null) {
                               StringBuilder builder = new StringBuilder("marc_").append(cartesianLeft.getKey()+cartesianLeft.getValue()).append(":\"").append(left.getValue()).append("\"").append(" AND ");
                               
                               builder.append("marc_").append(cartesianRight.getKey()).append(cartesianRight.getValue()).append(":(");
                               for (int i = 0; i < list.size(); i++) {
                                   SubField right = list.get(i);
                                   if (i>0) builder.append(" OR ");
                                   builder.append('"').append(right.getValue()).append('"');
                               }
                               builder.append(")");
                               try {
                                   
                                   SolrQuery idQuery = new SolrQuery(builder.toString()).setRows(100);
                                   idQuery.addFilterQuery("NOT identifier:\""+origin.identifier+"\"");
                                   if (!DEBUG) {
                                       idQuery.addFilterQuery("NOT dntstav:D");
                                       idQuery.addFilterQuery("NOT kuratorstav:DX");
                                   } else {
                                       idQuery.addFilterQuery("NOT setSpec:\"DNT-ALL\"");
                                   }
                                   idQuery.addFilterQuery("fmt:SE OR fmt:BK").setRows(1000);;
                                   LOGGER.info("Query: "+idQuery);
                                   SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
                                   
                                   for (SolrDocument sDocument : results) {

                                       List<String> fCombinations = new ArrayList<>();
                                       String fValue = sDocument.getFieldValue("raw").toString();
                                       JSONObject rawJSON = new JSONObject(fValue);
                                       
                                       JSONObject dataFields = rawJSON.getJSONObject("dataFields");
                                       if (dataFields.has("910")) {
                                           JSONArray array910 = dataFields.getJSONArray("910");
                                           for (int i = 0; i < array910.length(); i++) {
                                               JSONObject item910 = array910.getJSONObject(i);
                                               if (item910.has("subFields")) {
                                                   JSONObject subFields = item910.getJSONObject("subFields");
                                                   if (subFields.has("a") && subFields.has("x")) {
                                                       String aVal = subFields.getJSONArray("a").getJSONObject(0).getString("value");
                                                       JSONArray xJSONArray = subFields.getJSONArray("x");
                                                       for (int j = 0; j < xJSONArray.length(); j++) {
                                                           JSONObject xObject = xJSONArray.getJSONObject(j);
                                                           fCombinations.add(aVal+xObject.getString("value"));
                                                       }
                                                   }
                                               }
                                           }
                                       }
                                       
                                       for(int i=0,ll=combinations.size();i<ll;i++) {
                                           String combination = combinations.get(i);
                                           if (fCombinations.contains(combination)) {
                                               LOGGER.info("Matched compination "+combination);
                                               Triple<String, String, String> triple = triple(sDocument);
                                               if (!identifiers.contains(triple.getLeft())) {
                                                   retList.add(triple);
                                                   identifiers.add(triple.getLeft());
                                               }
                                               break;
                                           }
                                       }
                                    }
                                   
                               } catch (SolrServerException | IOException e) {
                                   LOGGER.log(Level.SEVERE,e.getMessage(),e);
                               }
                           }
                        });
                    });
                }
            });
        }
        return retList;
    }
}
