package cz.inovatika.sdnnt.services.impl.granularities;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

import static cz.inovatika.sdnnt.utils.SolrJUtilities.*;

public class SolrDocChange extends GranularityDocChange {
    
    private SolrDocument doc;
    private SolrClient solrClient;
    
    public SolrDocChange(SolrDocument doc, SolrClient solrClient, Logger logger) {
        super(logger);
        this.doc = doc;
        this.solrClient = solrClient;
    }

    
    public JSONArray getHistorieGranulovanehoStavu() {
        String historie =  (String) this.doc.getFirstValue(MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD);
        JSONArray historieGranulovanehoStavu = historie != null ? new JSONArray(historie) : new JSONArray();
        return historieGranulovanehoStavu;
    }
    
    
    public JSONArray getHistorieStavu() {
        String historie = (String) this.doc.getFieldValue(MarcRecordFields.HISTORIE_STAVU_FIELD);
        JSONArray historietavu = historie != null ? new JSONArray(historie) : new JSONArray();
        return historietavu;
    }

    

    public String getLicense() {
        return (String) this.doc.getFirstValue(MarcRecordFields.LICENSE_FIELD);
    }
    
    
    public String getFMTField() {
        return (String) this.doc.getFirstValue(MarcRecordFields.FMT_FIELD);
    }

    
    public String getControl008() {
        return (String) this.doc.getFirstValue("controlfield_008");
    }

    public String getDNTStav() {
        return (String) this.doc.getFirstValue(MarcRecordFields.DNTSTAV_FIELD);
    }
    
    public String getIdentifier() {
        return (String) this.doc.getFirstValue(MarcRecordFields.IDENTIFIER_FIELD);
    }
    
    
    public JSONArray getGranularity() {
        JSONArray retval = new JSONArray();
        List<String> granularities =  (List<String>) this.doc.getFieldValue(MarcRecordFields.GRANULARITY_FIELD);
        if (granularities != null) {
            granularities.stream().map(JSONObject::new).forEach(retval::put);
        }
        return retval;
    }


    @Override
    protected void setGranularity(List<String> granularity, String historieGranulovanehoStavu) {
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.setField(IDENTIFIER_FIELD, this.getIdentifier());
        if (historieGranulovanehoStavu != null) {
            atomicSet(idoc, historieGranulovanehoStavu.toString(), MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD);
        }
        atomicSet(idoc, granularity, MarcRecordFields.GRANULARITY_FIELD);

        try {
            solrClient.add(DataCollections.catalog.name(), idoc);
        } catch (SolrServerException | IOException e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
        }
    }

}
