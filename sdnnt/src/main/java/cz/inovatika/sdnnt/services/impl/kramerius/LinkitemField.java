package cz.inovatika.sdnnt.services.impl.kramerius;

import java.util.ArrayList;
import java.util.List;

public class LinkitemField {

    public static final String BASEURL_KEY = "baseUrl";
    public static final String MODEL_KEY = "model";
    public static final String ACRONYM_KEY = "acronym";
    public static final String PID_KEY = "pid";
    public static final String FETCHED_KEY = "fetched";
    public static final String KRAMERIUS_LICENSES = "kram_licenses";
    public static final String LINK_KEY = "link";
    public static final String DOSTUPNOST_KEY = "dostupnost";
    public static final String PID_PATH_KEY ="pidpaths";
    public static final String DATE_KEY = "date";

    protected String link;
    /** Kramerius stuff */
    protected String pid;
    protected String model;

    /** Additional information */
    protected String baseUrl;
    protected String fetched;
    protected String acronym;
    protected List<String> kramLicenses = new ArrayList<>();
    protected String dostupnost;
    protected String date;
    protected List<String> pidPaths = new ArrayList<>();

    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public String getFetched() {
        return fetched;
    }
    public void setFetched(String fetched) {
        this.fetched = fetched;
    }
    public String getBaseUrl() {
        return baseUrl;
    }
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    public void setPid(String pid) {
        this.pid = pid;
    }
    public String getPid() {
        return pid;
    }
    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }
    public String getAcronym() {
        return acronym;
    }
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }
    public void setKramLicenses(List<String> kramLicenses) {
        this.kramLicenses = kramLicenses;
    }
    public List<String> getKramLicenses() {
        return kramLicenses;
    }
    public String getDostupnost() {
        return dostupnost;
    }
    public void setDostupnost(String dostupnost) {
        this.dostupnost = dostupnost;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public List<String> getPidPaths() {
        return pidPaths;
    }
    public void setPidPaths(List<String> pidPaths) {
        this.pidPaths = pidPaths;
    }

}
