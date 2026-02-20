package cz.inovatika.sdnnt.services.impl.kramerius.infos;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.services.impl.kramerius.LinkitemField;

public class MasterLinkItem extends LinkitemField {


    public JSONObject toSDNNTSolrJson() {
        JSONObject object = new JSONObject();
        
        
        object.put(LINK_KEY,this.getLink());
        
        /*
        if (this.getRocnik() != null) {
            object.put(ROCNIK_KEY, this.getRocnik());
        }*/
        
        if (this.getDostupnost() != null) {
            object.put(DOSTUPNOST_KEY, this.getDostupnost());
        }
        
        if (this.getModel() != null) {
            object.put(LinkitemField.MODEL_KEY, this.getModel());
        }
        
        object.put(LinkitemField.PID_KEY, this.getPid());
        

        if (this.getBaseUrl() != null) {
            object.put(LinkitemField.BASEURL_KEY, this.getBaseUrl());
        }
        
        if (this.getFetched() != null) {
            object.put(LinkitemField.FETCHED_KEY, this.getFetched());
        }
 
        if (this.getAcronym() != null) {
            object.put(LinkitemField.ACRONYM_KEY, getAcronym());
        }

        /*
        if (this.getRocnik() != null) {
            object.put(ROCNIK_KEY, this.getRocnik());
        }

        if (this.getCislo() != null) {
            object.put(CISLO_KEY, getCislo());
        }
        */
        
        if (this.getPidPaths() != null && !this.getPidPaths().isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            this.getPidPaths().stream().forEach(jsonArray::put);
            object.put(PID_PATH_KEY, jsonArray);
        }
        
        if (this.getDate() != null) {
            object.put(DATE_KEY, this.getDate());
        }
        
        if (this.getKramLicenses() != null && !this.getKramLicenses().isEmpty()) {
            JSONArray kramLicenses = new JSONArray();
            this.getKramLicenses().forEach(kramLicenses::put);
            object.put(LinkitemField.KRAMERIUS_LICENSES, kramLicenses);
        }
        
        
        return object;
    }

    
}
