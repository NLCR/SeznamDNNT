package cz.inovatika.sdnnt.services.impl.granularities;

import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;

public class MarcRecordDocChange extends GranularityDocChange {
    
    protected MarcRecord rec;

    public MarcRecordDocChange(Logger logger, MarcRecord marcRecord) {
        super(logger);
        this.rec = marcRecord;
    }
    
    protected JSONArray getHistorieGranulovanehoStavu() {
        return this.rec.historie_granulovaneho_stavu;
    }

    
    protected String getLicense() {
        return this.rec.license;
    }

    protected String getFMTField() {
        return this.rec.fmt;
    }

    protected String getControl008() {
        return this.rec.controlFields.get("008");
    }
    
    protected String getDNTStav() {
        if (this.rec != null && this.rec != null && !this.rec.dntstav.isEmpty()) {
            return this.rec.dntstav.get(0);
        } else return null;
    }
    
    protected String getIdentifier() {
        return this.rec.identifier;
    }

    
    protected JSONArray getHistorieStavu() {
        return this.rec.historie_stavu;
    }

    @Override
    protected JSONArray getGranularity() {
        return this.rec.granularity;
    }

    @Override
    protected void setGranularity(List<String> granularity, String historieGranulovanehoStavu) {
        JSONArray jsonArray = new JSONArray();
        granularity.stream().map(JSONObject::new).forEach(jsonArray::put);
        this.rec.granularity = jsonArray;
        
        if (historieGranulovanehoStavu != null) {
            this.rec.historie_granulovaneho_stavu  = new JSONArray(historieGranulovanehoStavu);
        }
        
    }
}
