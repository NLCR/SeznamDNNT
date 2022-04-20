package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfListitem;
import cz.inovatika.sdnnt.openapi.endpoints.model.Listitem;
import cz.inovatika.sdnnt.openapi.endpoints.model.ListitemGranularity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelDocumentOutput  implements  SolrDocumentOutput{

    private ArrayOfListitem arrayOfListitem;

    public ModelDocumentOutput(ArrayOfListitem arrayOfListitem) {
        this.arrayOfListitem = arrayOfListitem;
    }

//    @Override
//    public void output(String selectedInstitution, String label, Collection<Object> nazev, String identifier, String... pids) {
//        for (int i = 0; i < pids.length; i++) {
//            Listitem item = new Listitem()
//                    .pid(pids[i])
//                    .catalogIdentifier(identifier)
//                    .sigla(selectedInstitution)
//                    .title(nazev.toString())
//                    .license(label);
//
//            this.arrayOfListitem.add(item);
//        }
//
//    }

    // "{\"stav\":[\"A\"],\"license\":\"dnnto\",\"cislo\":\"25\",\"link\":\"http://krameriusndk.nkp.cz/search/handle/uuid:305637b0-f3f3-11e9-8d0f-005056825209\",\"kuratorstav\":[\"A\"],\"rocnik\":\"1976\"}",
    //          "{\"stav\":[\"A\"],\"license\":\"dnnto\",\"cislo\":\"26\",\"link\":\"http://krameriusndk.nkp.cz/search/handle/uuid:2f1e5a20-f3d6-11e9-9f6f-5ef3fc9bb22f\",\"kuratorstav\":[\"A\"],\"rocnik\":\"1977\"}",
    //          "{\"stav\":[\"A\"],\"license\":\"dnnto\",\"cislo\":\"27\",\"link\":\"http://krameriusndk.nkp.cz/search/handle/uuid:1e591200-f004-11e9-a329-005056827e51\",\"kuratorstav\":[\"A\"],\"rocnik\":\"1978\"}",
    //

    protected ListitemGranularity granularity(String strJson) {

        JSONObject jsonObject = new JSONObject(strJson);
        JSONArray stav = jsonObject.optJSONArray("stav");
        String license = jsonObject.optString("license");
        String cislo = jsonObject.optString("cislo");
        String link = jsonObject.optString("link");
        String rocnik = jsonObject.optString("rocnik");

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
    }

    @Override
    public void output(Map<String, Object> outputDocument, List<String> ordering, String endpointLicense) {
        Collection pids = (Collection) outputDocument.get(PIDS_KEY);
        pids.forEach(pid-> {
            String identifier = outputDocument.get(IDENTIFIER_KEY) != null ? outputDocument.get(IDENTIFIER_KEY).toString() : null ;
            List<String> siglas = outputDocument.get(SELECTED_INSTITUTION_KEY) != null ? (List<String>) outputDocument.get(SELECTED_INSTITUTION_KEY) : null ;
            String nazev = outputDocument.get(NAZEV_KEY) != null ? outputDocument.get(NAZEV_KEY).toString() : null ;
            String label = outputDocument.get(LABEL_KEY) != null ? outputDocument.get(LABEL_KEY).toString() : null ;
            String idsdnnt = outputDocument.get(SDNNT_ID_KEY) != null ? outputDocument.get(SDNNT_ID_KEY).toString() : null ;

            Listitem item = new Listitem()
                    .pid(pid.toString())
                    .catalogIdentifier(identifier)
                    .sigla(siglas)
                    .title(nazev)
                    .license(label);
            if (label != null) {
                      //territoriality
                item.setTerritoriality("CZ");
            }
            
            if (idsdnnt != null) {
                item.sdnntIdentifier(idsdnnt);
            }
            
            
            List<String> granularity = (List<String>) outputDocument.get(GRANUARITY_KEY);
            if (granularity != null) {
                List<ListitemGranularity> collect = granularity.stream().map(this::granularity).collect(Collectors.toList());
                // filter master pid
                collect = collect.stream().filter(g -> {
                        if (g.getPid()!= null) {
                            return !g.getPid().equals(pid);
                        } else return true;
                }).collect(Collectors.toList());
                if (endpointLicense != null) {
                    collect = collect.stream().filter(g-> g.getLicense() != null && g.getLicense().equals(endpointLicense)).collect(Collectors.toList());
                }
                collect.stream().forEach(item::addGranularityItem);
                this.arrayOfListitem.add(item);
            }
        });
    }
}
