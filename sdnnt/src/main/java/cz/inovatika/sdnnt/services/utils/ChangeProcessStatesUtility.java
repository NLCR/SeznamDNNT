package cz.inovatika.sdnnt.services.utils;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxy;

public class ChangeProcessStatesUtility {

    private ChangeProcessStatesUtility() {}
    
    public static SolrInputDocument changeProcessState(String state, MarcRecord mr) {
        CuratorItemState kstav = CuratorItemState.valueOf(state);
        PublicItemState pstav = kstav.getPublicItemState(new DocumentProxy(mr));
        if (pstav != null && pstav.equals(PublicItemState.A) || pstav.equals(PublicItemState.PA)) {
          mr.license = License.dnnto.name();
        } else if (pstav != null && pstav.equals(PublicItemState.NL)) {
          mr.license = License.dnntt.name();
        } else {
            mr.license = null;
        }
        mr.setKuratorStav(kstav.name(), pstav.name(), mr.license, "scheduler", "scheduler", new JSONArray());
        return mr.toSolrDoc();
    }

    public static  SolrInputDocument changeProcessState(SolrClient solrClient, String identifier, String state) throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mr = MarcRecord.fromIndex(solrClient, identifier);
        return changeProcessState(state, mr);
    }

}
