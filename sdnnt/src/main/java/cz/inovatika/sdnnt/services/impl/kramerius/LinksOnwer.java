package cz.inovatika.sdnnt.services.impl.kramerius;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.services.impl.kramerius.granularities.Granularity;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.GranularityField;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.GranularityField.TypeOfRec;
import cz.inovatika.sdnnt.services.impl.kramerius.infos.MasterLinkItem;
import cz.inovatika.sdnnt.services.impl.kramerius.infos.MasterLinks;


public class LinksOnwer {

    private String control008;
    private String leader;
    
    private String catalogId;
    private String fmt;
    
    private List<String> urls = new ArrayList<>();

    private Granularity granularity;
    private MasterLinks masterLinks;
    
    public LinksOnwer(String catalogId) {
        super();
        this.catalogId = catalogId;
    }

    public void addTitleUrl(String url) {
        this.urls.add(url);
    }
    
    public void removeTitleUrl(String url) {
        this.urls.remove(url);
    }
    
    public List<String> getTitleUrls() {
        return new ArrayList<String>(this.urls);
    }

    
    public String getCatalogId() {
        return catalogId;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public void setGranularity(Granularity granularity) {
        this.granularity = granularity;
    }

    public MasterLinks getMasterLinks() {
        return masterLinks;
    }
    
    public void setMasterLinks(MasterLinks masterLinks) {
        this.masterLinks = masterLinks;
    }
    
    
    public void moveMasterLinkToGranularity(MasterLinkItem item, Logger logger) {
        
        GranularityField field = new GranularityField();
        field.setAcronym(item.getAcronym());
        field.setBaseUrl(item.getBaseUrl());
        field.setDate(item.getDate());
        field.setCislo(item.getDate());
        field.setRocnik(item.getDate());
        
        field.setDostupnost(item.getDostupnost());
        field.setKramLicenses(item.getKramLicenses());
        field.setLink(item.getLink());
        field.setModel(item.getModel());
        field.setFetched(item.getFetched());
        field.setPid(item.getPid());
        field.setPidPaths(item.getPidPaths());
        field.setTypeOfRec(TypeOfRec.SDNNT_MOVED);
        
        if (this.granularity.acceptByRule(field, logger)) {
            masterLinks.removeMasterLink(item);
            this.granularity.addGranularityField(field);
        } else {
            // nechame na 
        }
        
    }
    
    public String getFmt() {
        return fmt;
    }

    public void setFmt(String fmt) {
        this.fmt = fmt;
    }

    
    
    public String getControl008() {
        return control008;
    }

    public void setControl008(String control008) {
        this.control008 = control008;
    }
    
    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }


    public JSONObject toJSON() {
        JSONObject retval = new JSONObject();
        retval.put("identifier", this.catalogId);
        JSONArray titlesUrls = new JSONArray();
        this.urls.stream().forEach(url-> {
            titlesUrls.put(url);
        });
        retval.put("urls", titlesUrls);
        retval.put("fmt", this.fmt);
        retval.put("controlfield_008", this.control008);
        retval.put("ldr", this.leader);
        retval.put("granularity", this.granularity.toJSON());
        if (this.masterLinks != null) {
            retval.put("masterlinks", this.masterLinks.toJSON());
        }
        return retval;
    }
}
