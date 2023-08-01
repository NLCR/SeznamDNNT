package cz.inovatika.sdnnt.model.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import cz.inovatika.sdnnt.indexer.models.MarcRecord;
    
/**
 */
public class MarcRecordDependencyStore {
    
    private Map<String,List<MarcRecord>> prevProcessedRecords = new HashMap<>();
    
    public void addMarcRecord(MarcRecord mr) {
        if (!prevProcessedRecords.containsKey(mr.identifier)) {
            prevProcessedRecords.put(mr.identifier, new ArrayList<>());
        }
        this.prevProcessedRecords.get(mr.identifier).add(mr);
    }
    
    public void removeMarcRecord(MarcRecord mr) {
        if(prevProcessedRecords.containsKey(mr.identifier)) {
           prevProcessedRecords.get(mr.identifier).remove(mr);
        }
    }
    
    public List<MarcRecord> getMarcRecord(String identifier) {
        return new ArrayList<>( this.prevProcessedRecords.get(identifier) );
    }
    
    public boolean containsKey(String identifier) {
        return this.prevProcessedRecords.containsKey(identifier);
    }
}
