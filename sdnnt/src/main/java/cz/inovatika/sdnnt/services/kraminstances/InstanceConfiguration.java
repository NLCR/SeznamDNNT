package cz.inovatika.sdnnt.services.kraminstances;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class InstanceConfiguration {
    
    public static enum KramVersion {
        V5, V7;
    }
    
    /*
     *                  "description":"Vysoká škola ekonomická v Praze",
                    "api":"https://kramerius.vse.cz/search/",
                    "client":"https://kramerius.vse.cz/search/handle/{0}",
                    "acronym":"vse",
                    "sigla":"ABA006",
                    "skip": true

     */
    
    //private List<String>  matchNames = new ArrayList<>();

    private String apiPoint;
    private String clientAddress;
    private String domain;
    private String acronym;
    private String description;
    private String sigla;
    
    private boolean shouldSkip = false;
    
    
    private InstanceConfiguration() {}
    
    
    public String getApiPoint() {
        return apiPoint;
    }
    
    public void setApiPoint(String apiPoint) {
        this.apiPoint = apiPoint;
    }
    
    public String getClientAddress() {
        return clientAddress;
    }
    
    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public void setShouldSkip(boolean shouldSkip) {
        this.shouldSkip = shouldSkip;
    }
    
    public boolean isShouldSkip() {
        return shouldSkip;
    }
    
//    public void addMatchName(String matchName) {
//        this.matchNames.add(matchName);
//    }
//    
//    public void removeMatchName(String matchName) {
//        this.matchNames.remove(matchName);
//    }
    
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }
    
    public String getAcronym() {
        return acronym;
    }
    
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSigla() {
        return sigla;
    }
    
    public void setSigla(String sigla) {
        this.sigla = sigla;
    }
    
    public static InstanceConfiguration initConfiguration(String name, JSONObject instOBject) {

        InstanceConfiguration configuration = new InstanceConfiguration();
        configuration.setApiPoint(instOBject.optString("api"));
        configuration.setClientAddress(instOBject.optString("client"));
        configuration.setDomain(instOBject.optString("domain"));
        configuration.setShouldSkip(instOBject.optBoolean("skip"));
        configuration.setAcronym(instOBject.optString("acronym"));
        configuration.setDescription(instOBject.optString("description"));
        configuration.setSigla(instOBject.optString("sigla"));
        
        return configuration;
    }
    
}
