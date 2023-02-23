package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfListitem;
import cz.inovatika.sdnnt.openapi.endpoints.model.Listitem;
import cz.inovatika.sdnnt.openapi.endpoints.model.ListitemGranularity;
import cz.inovatika.sdnnt.openapi.endpoints.model.ListitemSkc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelDocumentOutput  implements  SolrDocumentOutput{

    private ArrayOfListitem arrayOfListitem;
    private Map<String,String> digitalLibrariesConfiguration;
    
    public ModelDocumentOutput(ArrayOfListitem arrayOfListitem, Map<String,String> conf) {
        this.arrayOfListitem = arrayOfListitem;
        this.digitalLibrariesConfiguration = conf;
    }


    protected ListitemGranularity granularity(String strJson, String parentPid) {

        JSONObject jsonObject = new JSONObject(strJson);
        JSONArray stav = jsonObject.optJSONArray("stav");
        String license = jsonObject.optString("license");
        String cislo = jsonObject.optString("cislo");
        String link = jsonObject.optString("link");
        String rocnik = jsonObject.optString("rocnik");
        String fetched  = jsonObject.optString("fetched");

        boolean pidpaths = true;
        
        JSONArray pidPathsJSON = jsonObject.optJSONArray("pidpaths");
        if (pidPathsJSON != null) {
            pidpaths = false;
            for (int i = 0; i < pidPathsJSON.length(); i++) {
                String path = pidPathsJSON.getString(i);
                if (path.contains(parentPid)) { pidpaths = true; }
            }
        }
        

        if (pidpaths && (jsonObject.has("cislo") || jsonObject.has("rocnik") || jsonObject.has("fetched"))) {

            
            ListitemGranularity granularity = new ListitemGranularity();
            if (stav != null) stav.forEach(o-> granularity.addStatesItem(o.toString()));
            if (license != null && !license.trim().equals("")) {
                granularity.setTerritoriality("CZ");
                granularity.setLicense(license);
            }

            if (cislo != null && !cislo.trim().equals("")) granularity.setNumber(cislo);
            if (link != null && link.contains("uuid:")) {
                int indexOf = link.indexOf("uuid:");
                String pid = link.substring(indexOf);
                granularity.pid(pid);
            }
            if (rocnik != null) {
                granularity.number(rocnik);
            }
            
            return granularity;
        } else {
            return null;
        }
        
    }

    @Override
    public void output(Map<String, Object> outputDocument, List<String> ordering, String endpointLicense, boolean doNotEmitParent) {
        Collection pids = (Collection) outputDocument.get(PIDS_KEY);
        Set<String> setPids = new LinkedHashSet(pids);
        if (!setPids.isEmpty()) {
            setPids.forEach(pid-> {
                pidOutput(outputDocument, endpointLicense, pid);
            });
        } else {
            pidOutput(outputDocument, endpointLicense, null);
        }
    }

    private void pidOutput(Map<String, Object> outputDocument, String endpointLicense, String pid ) {

        String identifier = outputDocument.get(IDENTIFIER_KEY) != null ? outputDocument.get(IDENTIFIER_KEY).toString() : null ;
        List<String> siglas = outputDocument.get(SELECTED_INSTITUTION_KEY) != null ? (List<String>) outputDocument.get(SELECTED_INSTITUTION_KEY) : null ;
        String nazev = outputDocument.get(NAZEV_KEY) != null ? outputDocument.get(NAZEV_KEY).toString() : null ;
        String label = outputDocument.get(LABEL_KEY) != null ? outputDocument.get(LABEL_KEY).toString() : null ;
        String idsdnnt = outputDocument.get(SDNNT_ID_KEY) != null ? outputDocument.get(SDNNT_ID_KEY).toString() : null ;
        String fmt = outputDocument.get(FMT_KEY) != null ? outputDocument.get(FMT_KEY).toString() : null ;
        String stav = outputDocument.get(DNTSTAV_KEY) != null  ? outputDocument.get(DNTSTAV_KEY).toString() : null;
        
        // ?? 
        List<String> digitalLibrary = outputDocument.get(SolrDocumentOutput.SELECTED_DL_KEY) != null ? (List<String>)outputDocument.get(SolrDocumentOutput.SELECTED_DL_KEY) : null;
        
        Listitem item = new Listitem()
                //.pid(pid)
                .catalogIdentifier(identifier)
                .title(nazev)
                .state(stav)
                .license(label);
        
        if (pid != null) {
            item.setPid(pid);
        }
        
        if (label != null) {
                  //territoriality
            item.setTerritoriality("CZ");
            item.setLicense(label);
        }
        
        if (siglas != null && !siglas.isEmpty()) {
            item.sigla(siglas);
        }
        
        if (digitalLibrary != null && !digitalLibrary.isEmpty()) {
            for (String dl : digitalLibrary) {  
                if (this.digitalLibrariesConfiguration != null && this.digitalLibrariesConfiguration.containsKey(dl)) {
                    dl = this.digitalLibrariesConfiguration.get(dl);
                }
                List<String> dls = item.getDigitalLibraries();
                if (dls != null && !dls.contains(dl)) {
                    item.addDigitalLibrariesItem(dl);  
                }
            }
        }
        
        
        if (fmt != null) {
            item.type(fmt);
        }
        
        if (idsdnnt != null) {
            item.sdnntIdentifier(idsdnnt);
        }
        
        if (outputDocument.containsKey(SolrDocumentOutput.CONTROL_FIELD_001_KEY)) {
            if (item.getSkc() == null) {
                item.skc(new ListitemSkc());
            }
            item.getSkc().controlfield001((String)outputDocument.get(SolrDocumentOutput.CONTROL_FIELD_001_KEY));
        }

        if (outputDocument.containsKey(SolrDocumentOutput.CONTROL_FIELD_003_KEY)) {
            if (item.getSkc() == null) {
                item.skc(new ListitemSkc());
            }
            item.getSkc().controlfield003((String)outputDocument.get(SolrDocumentOutput.CONTROL_FIELD_003_KEY));
        }

        if (outputDocument.containsKey(SolrDocumentOutput.CONTROL_FIELD_005_KEY)) {
            if (item.getSkc() == null) {
                item.skc(new ListitemSkc());
            }
            item.getSkc().controlfield005((String)outputDocument.get(SolrDocumentOutput.CONTROL_FIELD_005_KEY));
        }

        if (outputDocument.containsKey(SolrDocumentOutput.CONTROL_FIELD_008_KEY)) {
            if (item.getSkc() == null) {
                item.skc(new ListitemSkc());
            }
            item.getSkc().controlfield008((String)outputDocument.get(SolrDocumentOutput.CONTROL_FIELD_008_KEY));
        }

        /*
        if (outputDocument.containsKey(SolrDocumentOutput.RAW_KEY)) {
            if (item.getSkc() == null) {
                item.skc(new ListitemSkc());
            }
            String raw = (String) outputDocument.get(SolrDocumentOutput.RAW_KEY);
            JSONObject rawJSON = new JSONObject(raw);
            
            Map<String, List<String>> tagCodeMapping = new HashMap<>();
            tagsCode(rawJSON, tagCodeMapping, "310", "a");
            tagsCode(rawJSON, tagCodeMapping, "310", "b");
            
            
            if(tagCodeMapping.containsKey("310_a")) {
                ListitemSkcMarc310 m310 = new ListitemSkcMarc310();
                tagCodeMapping.get("310_a").stream().forEach(m310::addAItem);
                item.getSkc().setMarc310(m310);
            }
        }
        */
        
        
        List<String> granularity = (List<String>) outputDocument.get(GRANUARITY_KEY);
        if (granularity != null) {
            List<ListitemGranularity> collect = new ArrayList<>();
            for (String grItemString : granularity) {
                ListitemGranularity gritem = granularity(grItemString, pid);
                if (gritem != null) {
                    collect.add(gritem);
                }
            }
            /* 
            collect = collect.stream().filter(g -> {
                    if (g.getPid()!= null) {
                        return !g.getPid().equals(pid);
                    } else return true;
            }).collect(Collectors.toList());
            */
            if (endpointLicense != null) {
                collect = collect.stream().filter(g-> g.getLicense() != null && g.getLicense().equals(endpointLicense)).collect(Collectors.toList());
            }
            collect.stream().forEach(item::addGranularityItem);
            this.arrayOfListitem.add(item);
        }
    }

    protected void tagsCode(JSONObject rawJSON, Map<String, List<String>> tagCodeMapping, String tag, String code) {
        if (rawJSON.has("dataFields")) {
            JSONObject dataFields = rawJSON.getJSONObject("dataFields");
            if (dataFields.has(tag) ) {
                JSONArray tagArr = dataFields.getJSONArray(tag);
                for (int i = 0; i <  tagArr.length(); i++) {
                    
                    JSONObject tagItem = tagArr.getJSONObject(i);
                    if (tagItem.has("subFields")) {
                        JSONObject subFields = tagItem.getJSONObject("subFields");
                        if (subFields.has(code)) {
                            JSONArray aCode = subFields.getJSONArray(code);
                            for (int j = 0; j < aCode.length(); j++) {
                                String value = aCode.getJSONObject(j).optString("value");
                                if (value != null) {
                                   String format = String.format("%s_%s", tag, code);
                                   if (!tagCodeMapping.containsKey(format)) {
                                       tagCodeMapping.put(format, new ArrayList<>());
                                   }
                                   tagCodeMapping.get(format).add(value);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
