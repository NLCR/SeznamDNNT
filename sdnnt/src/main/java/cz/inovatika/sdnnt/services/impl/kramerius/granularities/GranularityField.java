package cz.inovatika.sdnnt.services.impl.kramerius.granularities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.services.impl.kramerius.LinkitemField;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.rules.Marc911Rule;
import cz.inovatika.sdnnt.services.impl.utils.SKCYearsUtils;
import cz.inovatika.sdnnt.services.impl.utils.SolrYearsUtils;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration;
import cz.inovatika.sdnnt.utils.StringUtils;

public class GranularityField extends LinkitemField {
    
    public static final String STAV_KEY = "stav";
    public static final String KURATOR_KEY = "kuratorstav";
    public static final String LICENSE_KEY = "license";
    public static final String ROCNIK_KEY = "rocnik";
    public static final String ROOT_PID_KEY = "rootPid";
    public static final String DETAILS_KEY = "details";
    public static final String CISLO_KEY = "cislo";
    
    public static enum TypeOfRec {
        SDNNT_SOLR,
        KRAM_SOLR,
        SDNNT_MOVED
    }
    
    /** SDNNT stuff **/
    private String stav;
    private String cislo;
    private String kuratorStav;
    private String rocnik;
    private String license;
    private String rootPid;
    private String details;

    /** only runtime information */
    private TypeOfRec typeOfRec; 
    
    
    public GranularityField() {}
    
    public void setStav(String stav) {
        this.stav = stav;
    }
    public String getStav() {
        return stav;
    }
    
    public void setKuratorStav(String kuratorStav) {
        this.kuratorStav = kuratorStav;
    }
    
    public String getKuratorStav() {
        return kuratorStav;
    }
    
    public String getRocnik() {
        return rocnik;
    }
    public void setRocnik(String rocnik) {
        this.rocnik = rocnik;
    }
    
    public String getLicense() {
        return license;
    }
    
    public void setLicense(String license) {
        this.license = license;
    }
    
    
    public String getRootPid() {
        return rootPid;
    }
    
    public void setRootPid(String rootPid) {
        this.rootPid = rootPid;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getCislo() {
        return cislo;
    }
    
    public void setCislo(String cislo) {
        this.cislo = cislo;
    }
    
    
    public void setTypeOfRec(TypeOfRec typeOfRec) {
        this.typeOfRec = typeOfRec;
    }
    
    
    public TypeOfRec getTypeOfRec() {
        return typeOfRec;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(kuratorStav, license, link, rocnik, stav);
    }

    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GranularityField other = (GranularityField) obj;
        return Objects.equals(kuratorStav, other.kuratorStav) && Objects.equals(license, other.license)
                && Objects.equals(link, other.link) && Objects.equals(rocnik, other.rocnik)
                && Objects.equals(stav, other.stav);
    }
    public JSONObject toJSON() {
        JSONObject retval = toSDNNTSolrJson();
        retval.put("type", this.typeOfRec.name());
        return retval;
    }
    public JSONObject toSDNNTSolrJson() {
        JSONObject object = new JSONObject();
        
        if (getStav() != null) {
            JSONArray stavArr = new JSONArray();
            stavArr.put(this.getStav());
            object.put(STAV_KEY, stavArr);
        }

        if (getKuratorStav() != null) {
            JSONArray kuratorStavArr = new JSONArray();
            kuratorStavArr.put(this.getKuratorStav());
            object.put("kuratorstav", kuratorStavArr);
        }
        
        if (this.getLicense() != null && StringUtils.isAnyString(this.getLicense())) {
            object.put(LICENSE_KEY, this.getLicense());
        }
        
        object.put(LINK_KEY,this.getLink());
        
        if (this.getRocnik() != null) {
            object.put(ROCNIK_KEY, this.getRocnik());
        }
        
        if (this.getDostupnost() != null) {
            object.put(DOSTUPNOST_KEY, this.getDostupnost());
        }
        
        if (this.getModel() != null) {
            object.put(LinkitemField.MODEL_KEY, this.getModel());
        }
        
        object.put(LinkitemField.PID_KEY, this.getPid());
        
        if (this.getRootPid() != null) {
            object.put(ROOT_PID_KEY, getRootPid());
        }
        
        if (this.getDetails() != null) {
            object.put(DETAILS_KEY, this.getDetails());
        }
        
        if (this.getBaseUrl() != null) {
            object.put(LinkitemField.BASEURL_KEY, this.getBaseUrl());
        }
        
        if (this.getFetched() != null) {
            object.put(LinkitemField.FETCHED_KEY, this.getFetched());
        }
 
        if (this.getAcronym() != null) {
            object.put(LinkitemField.ACRONYM_KEY, getAcronym());
        }

        if (this.getRocnik() != null) {
            object.put(ROCNIK_KEY, this.getRocnik());
        }

        if (this.getCislo() != null) {
            object.put(CISLO_KEY, getCislo());
        }
        
        if (this.getDate() != null) {
            object.put(LinkitemField.DATE_KEY, this.getDate());
        }
        
        if (this.getPidPaths() != null && !this.getPidPaths().isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            this.getPidPaths().stream().forEach(jsonArray::put);
            object.put(LinkitemField.PID_PATH_KEY, jsonArray);
        }
        
        if (this.getKramLicenses() != null && !this.getKramLicenses().isEmpty()) {
            JSONArray kramLicenses = new JSONArray();
            this.getKramLicenses().forEach(kramLicenses::put);
            object.put(LinkitemField.KRAMERIUS_LICENSES, kramLicenses);
        }
        
        
        return object;
    }
    
    public static GranularityField initFromSDNNTSolrJson(JSONObject object) {
        GranularityField field = new GranularityField();
        
        field.setStav(stav(object, STAV_KEY));
        field.setKuratorStav(stav(object, KURATOR_KEY));
        field.setLicense(object.optString(LICENSE_KEY));
        field.setLink(object.optString(LINK_KEY));
        field.setRocnik(object.optString(ROCNIK_KEY));

        field.setDostupnost(object.optString(DOSTUPNOST_KEY));
        field.setModel(object.optString(LinkitemField.MODEL_KEY));
        String pid = object.optString(LinkitemField.PID_KEY);
        if (!StringUtils.isAnyString(pid)) {
            String link2 = field.getLink();
            int indexof = link2.indexOf("uuid:");
            if (indexof > 0) {
                pid = link2.substring(indexof);
            }
        }
        field.setPid(pid);
        
        field.setRootPid(object.optString(ROOT_PID_KEY));
        field.setDetails(object.optString(DETAILS_KEY));
        
        field.setFetched(object.optString(LinkitemField.FETCHED_KEY));
        field.setAcronym(object.optString(LinkitemField.ACRONYM_KEY));
        field.setBaseUrl(object.optString(LinkitemField.BASEURL_KEY));
        field.setDate(object.optString(LinkitemField.DATE_KEY));

        if (object.has(LinkitemField.KRAMERIUS_LICENSES)) {
            JSONArray kramLicensesJsonArray = object.getJSONArray(LinkitemField.KRAMERIUS_LICENSES);
            List<String> kramLicenses = new ArrayList<>();
            for (int i = 0; i < kramLicensesJsonArray.length(); i++) {
                kramLicenses.add(kramLicensesJsonArray.getString(i));
            }
            field.setKramLicenses(kramLicenses);
        }
        
        if (object.has(LinkitemField.PID_PATH_KEY)) {
            JSONArray pidPathsJsonArray = object.getJSONArray(LinkitemField.PID_PATH_KEY);
            List<String> pidPathsList = new ArrayList<>();
            for (int i = 0; i < pidPathsJsonArray.length(); i++) {
                pidPathsList.add(pidPathsJsonArray.getString(i));
            }
            field.setPidPaths(pidPathsList);
        }
        
        return field;
    }


    public static GranularityField initFromSDNNTSolrJson(JSONObject object, CheckKrameriusConfiguration conf) {
        GranularityField field = new GranularityField();
        
        field.setStav(stav(object, STAV_KEY));
        field.setKuratorStav(stav(object, KURATOR_KEY));
        field.setLicense(object.optString(LICENSE_KEY));
        field.setLink(object.optString(LINK_KEY));
        field.setRocnik(object.optString(ROCNIK_KEY));

        
        String dostupnost = object.optString(DOSTUPNOST_KEY);
        if (StringUtils.isAnyString(dostupnost)) {
            field.setDostupnost(dostupnost);
        }
        
        field.setModel(object.optString(LinkitemField.MODEL_KEY));
        String pid = object.optString(LinkitemField.PID_KEY);
        if (!StringUtils.isAnyString(pid)) {
            String link2 = field.getLink();
            int indexof = link2.indexOf("uuid:");
            if (indexof > 0) {
                pid = link2.substring(indexof);
            }
        }
        field.setPid(pid);
        
        field.setRootPid(object.optString(ROOT_PID_KEY));
        field.setDetails(object.optString(DETAILS_KEY));
        field.setCislo(object.optString(CISLO_KEY));
        
        field.setFetched(object.optString(LinkitemField.FETCHED_KEY));
        String baseUrl = object.optString(LinkitemField.BASEURL_KEY);
        if (!StringUtils.isAnyString(baseUrl)) {
            String link = object.optString(LINK_KEY);
            String baseU = conf.baseUrl(link);
            field.setBaseUrl(baseU);
        } else {
            field.setBaseUrl(baseUrl);
        }
        
        String acronym = object.optString(LinkitemField.ACRONYM_KEY);
        if (!StringUtils.isAnyString(acronym)) {
            InstanceConfiguration instance = conf.match(field.getBaseUrl());
            if (instance != null) {
                acronym = instance.getAcronym();
            }
        }
        field.setAcronym(acronym);

        if (object.has(LinkitemField.KRAMERIUS_LICENSES)) {
            Object kramLicensesObject = object.get(LinkitemField.KRAMERIUS_LICENSES);
            List<String> kramLicenses = new ArrayList<>();
            if (!(kramLicensesObject instanceof JSONArray)) {
                kramLicenses.add(kramLicensesObject.toString());
            } else {
                JSONArray kramLicensesJsonArray = object.getJSONArray(LinkitemField.KRAMERIUS_LICENSES);
                for (int i = 0; i < kramLicensesJsonArray.length(); i++) {
                    kramLicenses.add(kramLicensesJsonArray.getString(i));
                }
                
            }
            field.setKramLicenses(kramLicenses);
        }

        if (object.has(LinkitemField.PID_PATH_KEY)) {
            JSONArray pidPathsJsonArray = object.getJSONArray(LinkitemField.PID_PATH_KEY);
            List<String> pidPathsList = new ArrayList<>();
            for (int i = 0; i < pidPathsJsonArray.length(); i++) {
                pidPathsList.add(pidPathsJsonArray.getString(i));
            }
            field.setPidPaths(pidPathsList);
        }

        return field;
    }

    @Override
    public String toString() {
        return "GranularityField [stav=" + stav + ", kuratorStav=" + kuratorStav + ", rocnik=" + rocnik + ", license="
                + license + ", link=" + link + ", baseUrl=" + baseUrl + ", fetched=" + fetched + ", acronym=" + acronym
                + "]";
    }

    
    
    private static String stav(JSONObject object,  String key) {
        if (object.has(key)) {
            Object stav = object.get(key);
            if (stav instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray)stav;
                if (jsonArray.length() > 0 )  return jsonArray.getString(0);
            } else {
                return stav.toString();
            }
            
        } 
        return null;
    }
}
