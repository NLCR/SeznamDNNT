package cz.inovatika.sdnnt.services.kraminstances;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration.KramVersion;
import cz.inovatika.sdnnt.utils.StringUtils;

/**
 * Main instance configuration
 * @author happy
 *
 */
public class InstanceConfiguration {
    
    /** Version enum */
    public static enum KramVersion {
        
        V5, V7;
        
        public static KramVersion load(String vinfo) {
            switch(vinfo.toLowerCase()) {
                case "v5":
                case "k5":
                    return V5;
                case "v7":
                case "k7":
                    return V7;
                    
                 default:
                     return V5;
            }
        }
    }
    
    /** API point */
    private String apiPoint;
    /** Link to client */
    private String clientAddress;
    /** Domain where kramerius sits on */
    private String domain;
    /** Acronym for DL */
    private String acronym;
    /** Only description */
    private String description;
    /** Main sigla */
    private String sigla;
    
    private List<String> additionalSiglas = new ArrayList<String>();
    
    private KramVersion version = KramVersion.V5;
    
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
    
    public void setVersion(KramVersion version) {
        this.version = version;
    }
    
    public KramVersion getVersion() {
        return version;
    }
    
    public List<String> getAdditionalSiglas() {
        return additionalSiglas;
    }


    public void setAdditionalSiglas(List<String> additionalSiglas) {
        this.additionalSiglas = additionalSiglas;
    }
    
    public boolean matchSigla(String sigla) {
        if (this.sigla.endsWith(sigla)) return true;
        else {
            return this.additionalSiglas.contains(sigla);
        }
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
        
        JSONArray jsonArray = instOBject.optJSONArray("additional_sigla");
        if (jsonArray != null && jsonArray.length() > 0) {
            List<String> siglas = new ArrayList();
            for (int i = 0; i < jsonArray.length(); i++) {
                String aSigla = jsonArray.getString(i);
                siglas.add(aSigla);
            }
            if (!siglas.isEmpty()) {
                configuration.setAdditionalSiglas(siglas);
            }
        }

        
        
        
        String version = instOBject.optString("version");
        if (version != null && StringUtils.isAnyString(version)) {
            configuration.setVersion(KramVersion.load(version));
        }
        
        
        
        return configuration;
    }
    
    

    
    @Override
    public String toString() {
        return "InstanceConfiguration [apiPoint=" + apiPoint + ", clientAddress=" + clientAddress + ", domain=" + domain
                + ", acronym=" + acronym + ", description=" + description + ", sigla=" + sigla + ", version=" + version
                + ", shouldSkip=" + shouldSkip + "]";
    }
    
    
    
}
