package cz.inovatika.sdnnt.services.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.index.utils.GranularityUtils;
import cz.inovatika.sdnnt.index.utils.HistoryObjectUtils;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxy;
import cz.inovatika.sdnnt.services.impl.granularities.MarcRecordDocChange;
import cz.inovatika.sdnnt.utils.JSONUtils;

public class ChangeProcessStatesUtility {

    private ChangeProcessStatesUtility() {}

    
    public static SolrInputDocument changeProcessState(String curState, String license, MarcRecord mr, String user, String message) {
        List<String> previous = mr.dntstav;
        CuratorItemState kstav = CuratorItemState.valueOf(curState);
        PublicItemState pstav = kstav.getPublicItemState(new DocumentProxy(mr, null));
        if (license != null) {
            mr.license = license;
        } else {
            if (pstav != null && (pstav.equals(PublicItemState.A) || pstav.equals(PublicItemState.PA))) {
                if (mr.license == null) {
                    mr.license = License.dnnto.name();
                }
            } else if (pstav != null && pstav.equals(PublicItemState.NL)) {
                if (mr.license == null) {
                    mr.license = License.dnntt.name();
                }
            } else {
                mr.license = null;
            }
        }

        mr.setKuratorStav(kstav.name(), pstav.name(), mr.license, user, message);

        // REcalculate granularity
        calculateGranularity(mr, user, message, null); 
        return mr.toSolrDoc();
    }
    
    public static SolrInputDocument changeProcessState(String state, MarcRecord mr, String user, String message) {
        List<String> previous = mr.dntstav;
        CuratorItemState kstav = CuratorItemState.valueOf(state);
        PublicItemState pstav = kstav.getPublicItemState(new DocumentProxy(mr, null));
        if (pstav != null && (pstav.equals(PublicItemState.A) || pstav.equals(PublicItemState.PA))) {
            if (mr.license == null) {
                mr.license = License.dnnto.name();
            }
        } else if (pstav != null && pstav.equals(PublicItemState.NL)) {
            if (mr.license == null) {
                mr.license = License.dnntt.name();
            }
        } else {
            mr.license = null;
        }
        
        calculateGranularity(mr, user, message, null); 
        mr.setKuratorStav(kstav.name(), pstav.name(), mr.license, user, message);
        return mr.toSolrDoc();
    }

    public static void clearGranularityItem(JSONObject gItem,CuratorItemState expectedPrev,String key, List<String> removeKeys) {
        if (gItem.has(key)) {
            boolean shoudRemove = false;
            Object object = gItem.get(key);
            if (object instanceof JSONArray) {
                JSONArray stavArray = (JSONArray) object;
                if (stavArray.length() > 0 && stavArray.getString(0).equals(expectedPrev.name())) {
                    stavArray.remove(0);
                    shoudRemove = true;
                }
            } else if (object instanceof String) {
                String stv = (String) object;
                if(stv.equals(expectedPrev.name())) {
                    shoudRemove = true;
                }
            }
            if (shoudRemove) {
                gItem.remove(key);
                for (String kk : removeKeys) {  gItem.remove(kk);  }
            }
        }
    }
    
    
    
    
    public static void calculateGranularity(MarcRecord mr,  String user, String poznamka, String originator) {
        MarcRecordDocChange docChange = new MarcRecordDocChange(null, mr);
        if (mr.granularity != null && mr.granularity.length() > 0) {
            List<JSONObject> processingGranularity = new ArrayList<>();
            mr.granularity.forEach(it-> {
                JSONObject obj =  (JSONObject) it;
                JSONObject nObj = new JSONObject();
                obj.keySet().forEach(key-> {
                    nObj.put(key, obj.get(key));
                });
                
                processingGranularity.add(nObj);
            });
            boolean changed = docChange.processOneDoc(processingGranularity, mr.identifier);
            if (changed) {
                docChange.atomicChange(processingGranularity, mr.identifier, originator);
            }
        }
    }


//    public static  SolrInputDocument changeProcessState(MarcRecord mr, String state, String user, String message) throws JsonProcessingException, SolrServerException, IOException {
//        //MarcRecord mr = MarcRecord.fromIndex(solrClient, identifier);
//        return changeProcessState(state, mr,user, message);
//    }


    public static  SolrInputDocument changeProcessState(SolrClient solrClient, String identifier, String state, String user, String message) throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mr = MarcRecord.fromIndex(solrClient, identifier);
        return changeProcessState(state, mr,user, message);
    }
    public static  SolrInputDocument changeProcessState(SolrClient solrClient, String identifier, String state,  String message) throws JsonProcessingException, SolrServerException, IOException {
        return changeProcessState(solrClient, identifier, state, "scheduler", message);
    }
    
    
}
