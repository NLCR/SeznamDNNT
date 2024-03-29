package cz.inovatika.sdnnt.model.workflow.duplicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import cz.inovatika.sdnnt.index.utils.HistoryObjectUtils;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.TransitionType;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.MarcRecordDependencyStore;
import cz.inovatika.sdnnt.model.workflow.SwitchStateOptions;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxy;

public class DuplicateProxy extends DocumentProxy {
    
    
    public static final String ACCEPT_ALL_KEYWORD = "all";
    
    private List<MarcRecord> allFollowers;
    private List<MarcRecord> affectedFollowers;
    private List<Zadost> reqs;
    
    
    public DuplicateProxy(MarcRecord origin,Zadost zadostOrigin, List<MarcRecord> followers, List<Zadost> reqs) {
        super(origin,zadostOrigin);
        this.allFollowers = followers;
        this.affectedFollowers = new ArrayList<>();
        this.reqs = reqs;
    }
    

    @Override
    public void switchWorkflowState(MarcRecordDependencyStore depStore, SwitchStateOptions options, CuratorItemState itm, String license, boolean changingLicenseState, Period period, String originator, String user, String poznamka) {
        
        Map<String,MarcRecord> map = new HashMap<>();

        this.allFollowers.stream().forEach(m-> {
            map.put(m.identifier, m);
        });
        
        
        if (options.getOptions() != null && options.getOptions().length >= 1) {
            if(options.getOptions()[0].toLowerCase().equals(ACCEPT_ALL_KEYWORD)) {
                this.affectedFollowers = new ArrayList<>(this.allFollowers);
            } else {
                Arrays.stream(options.getOptions()).forEach(selected-> {
                    if (map.containsKey(selected)) {
                        this.affectedFollowers.add(map.get(selected));
                    }
                });
            }
        }
        
        //if (dep)
        if (depStore != null) {
            this.affectedFollowers.forEach(depStore::addMarcRecord);
        }
        
        // vymenuje v zadostech puvodni dila za nasledniky 
        DuplicateUtils.changeRequests(this.marcRecord, this.affectedFollowers, reqs);
        // presune vlatnosti na naslednika
        DuplicateUtils.moveProperties(depStore, this.marcRecord, this.affectedFollowers, (mr) -> {
            if (mr.kuratorstav!= null && mr.kuratorstav.size() > 0) {
                
                // pokud je dx, musi se pri zmene stavu vratit stav z historie
                if (mr.kuratorstav.get(0).equals(CuratorItemState.DX.name())) {
                    List<String> previous = mr.kuratorstav;
                    mr.kuratorstav = mr.dntstav; // ?? Verejny stav ?? 
                    JSONObject historyObject = HistoryObjectUtils.historyObjectParent(mr.kuratorstav.get(0), mr.license, originator, user, poznamka, MarcRecord.FORMAT.format(new Date()));
                    mr.historie_kurator_stavu.put(historyObject);
                    mr.previousKuratorstav = previous;
                }
            }
        });
        super.switchWorkflowState(depStore, options, itm, license, changingLicenseState, period, originator, user, poznamka);
        this.marcRecord.followers = this.affectedFollowers.stream().map(m-> {
            return m.identifier;
        }).collect(Collectors.toList());
    }
    


    @Override
    public List<Pair<String, SolrInputDocument>> getStateToSave(SwitchStateOptions options) {
        
        List<Pair<String, SolrInputDocument>> retlist = new ArrayList<>();
        retlist.add(Pair.of(DataCollections.catalog.name(), marcRecord.toSolrDoc()));

        this.affectedFollowers.stream().forEach(f-> {
            retlist.add(Pair.of(DataCollections.catalog.name(), f.toSolrDoc()));
        });
         this.reqs.stream().forEach(r-> {
            retlist.add(Pair.of(DataCollections.zadost.name(), r.toSolrInputDocument()));
        });
        return retlist;
    }


    @Override
    public boolean hasRejectableWorkload() {
        return true;
    }
    
    
    
    

    @Override
    public void rejectWorkflowState(String originator, String user, String poznamka) {
        super.rejectWorkflowState(originator, user, poznamka);

        if (this.marcRecord.kuratorstav!=null && 
            this.marcRecord.kuratorstav.size() > 0 && 
            this.marcRecord.kuratorstav.get(0).equals(CuratorItemState.DX.name()) &&
            this.marcRecord.dntstav != null &&
            this.marcRecord.dntstav.size() > 0) {

            // if marc  record has kontext 
            List<String> dntstav = this.marcRecord.dntstav;
            CuratorItemState itm = CuratorItemState.valueOf(dntstav.get(0));
            super.switchWorkflowState(null, null, itm, getLicense(), false, null, originator, user, poznamka);

            this.marcRecord.followers = this.affectedFollowers.stream().map(m-> {
                return m.identifier;
            }).collect(Collectors.toList());
        }
        
    }
}
