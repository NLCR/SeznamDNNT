package cz.inovatika.sdnnt.services.impl.kramerius.infos;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.services.impl.kramerius.granularities.GranularityField;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class MasterLinks {
    
    private String identifier;
    private List<MasterLinkItem> masterLinks = new ArrayList<>();

    public MasterLinks(String identifier) {
        super();
        this.identifier = identifier;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public void addMasterLink(MasterLinkItem item) {
        this.masterLinks.add(item);
    }
    
    public void removeMasterLink(MasterLinkItem item) {
        this.masterLinks.remove(item);
    }
    
    public List<MasterLinkItem> getMasterLinks() {
        return new ArrayList<>(masterLinks);
    }
    
    public SolrInputDocument toRomeSolrDocument() {
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.setField(IDENTIFIER_FIELD, identifier);
        SolrJUtilities.atomicSetNull(idoc,  MarcRecordFields.MASTERLINKS_FIELD);
        return idoc;
    }    


    public List<SolrInputDocument> toSolrDocument() {
        if (!this.masterLinks.isEmpty()) {
            
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField(IDENTIFIER_FIELD, identifier);
            List<String> collected = this.masterLinks.stream().map(MasterLinkItem::toSDNNTSolrJson).map(JSONObject::toString).collect(Collectors.toList());
            SolrJUtilities.atomicSet(idoc, collected, MarcRecordFields.MASTERLINKS_FIELD);
            
            SolrInputDocument ddoc = new SolrInputDocument();
            ddoc.setField(IDENTIFIER_FIELD, identifier);
            SolrJUtilities.atomicSet(ddoc, false, MarcRecordFields.MASTERLINKS_DISABLED_FIELD);
            
            return Arrays.asList(idoc, ddoc);
        } else {

            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField(IDENTIFIER_FIELD, identifier);
            SolrJUtilities.atomicSetNull(idoc, MarcRecordFields.MASTERLINKS_FIELD);

            SolrInputDocument ddoc = new SolrInputDocument();
            ddoc.setField(IDENTIFIER_FIELD, identifier);
            SolrJUtilities.atomicSet(ddoc, true, MarcRecordFields.MASTERLINKS_DISABLED_FIELD);

            return Arrays.asList(idoc, ddoc);
            
        }
    }

    
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        this.masterLinks.stream().map(MasterLinkItem::toSDNNTSolrJson).forEach(jsonObject-> {
            array.put(jsonObject);
        });
        obj.put("field", array);
        
        obj.put("identifier", this.identifier);
        return obj;

    }
}