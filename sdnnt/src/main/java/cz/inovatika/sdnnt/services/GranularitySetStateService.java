package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;

/**
 * Calculate states and licenses for granularity items & create history record
 * @author happy
 */
public interface GranularitySetStateService {
    
    /**
     * Returns logger of the service 
     * @return
     */
    Logger getLogger();
    
    /**
     * Set sates for whole set 
     * @param addFilters
     * @throws IOException
     */
    public void setStates(List<String> addFilters) throws IOException;
    
    
    /**
     * Set states for one record
     * @throws IOException
     */
    public void setStatesForOneRecord(MarcRecord marcRecord) throws IOException;
    
    
    
    
}
