package cz.inovatika.sdnnt.services.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxy;

public class ChangeProcessStatesUtility {

    private ChangeProcessStatesUtility() {}
    
    public static SolrInputDocument changeProcessState(String state, MarcRecord mr, String message) {
        List<String> previous = mr.dntstav;

        CuratorItemState kstav = CuratorItemState.valueOf(state);
        PublicItemState pstav = kstav.getPublicItemState(new DocumentProxy(mr));
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
        granularityChange(mr, previous, kstav); 
        mr.setKuratorStav(kstav.name(), pstav.name(), mr.license, "scheduler", message, new JSONArray());
        return mr.toSolrDoc();
    }

    
    private static void granularityChange(MarcRecord mr, List<String> previous, CuratorItemState current) {

        if (previous != null && previous.size() > 0 && previous.get(0).equals(CuratorItemState.PA.name()) && current.equals(CuratorItemState.A)) {
            JSONArray granularity = mr.granularity;
            if (granularity != null) {
                for (int i = 0; i < granularity.length(); i++) {

                    JSONObject gItem = granularity.getJSONObject(i);
                    granularityItem(gItem, current, CuratorItemState.PA, "stav", new ArrayList<>());
                    granularityItem(gItem, current, CuratorItemState.PA, "kuratorstav", new ArrayList<>());
                }
            }
        }
        
        if (current != null && current.equals(CuratorItemState.N)) {
            JSONArray granularity = mr.granularity;
            if (granularity != null) {
                for (int i = 0; i < granularity.length(); i++) {
                    JSONObject gItem = granularity.getJSONObject(i);
                    granularityItem(gItem, current, CuratorItemState.A, "stav", Arrays.asList("license"));
                    granularityItem(gItem, current, CuratorItemState.PA, "stav",Arrays.asList("license"));
                    granularityItem(gItem, current, CuratorItemState.NL, "stav",Arrays.asList("license"));

                    granularityItem(gItem, current, CuratorItemState.A, "kuratorstav",Arrays.asList("license"));
                    granularityItem(gItem, current, CuratorItemState.PA, "kuratorstav",Arrays.asList("license"));
                    granularityItem(gItem, current, CuratorItemState.NL, "kuratorstav",Arrays.asList("license"));
                }
            }
        }
    }

    private static void granularityItem(JSONObject gItem, CuratorItemState current, CuratorItemState expectedPrev,String key, List<String> removeKeys) {
        if (gItem.has(key)) {
            boolean updated = false;
            Object object = gItem.get(key);
            if (object instanceof JSONArray) {
                JSONArray stavArray = (JSONArray) object;
                if (stavArray.length() > 0 && stavArray.getString(0).equals(expectedPrev.name())) {
                    stavArray.remove(0);
                    stavArray.put(current.name());
                    updated = true;
                }
            } else if (object instanceof String) {
                String stv = (String) object;
                if(stv.equals(expectedPrev.name())) {
                    gItem.put(key, current.name());
                    updated = true;
                }
            }
            
            if (updated) {
                for (String kk : removeKeys) {  gItem.remove(kk);  }
            }
        }
    }

    public static  SolrInputDocument changeProcessState(SolrClient solrClient, String identifier, String state, String message) throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mr = MarcRecord.fromIndex(solrClient, identifier);
        return changeProcessState(state, mr, message);
    }

}
