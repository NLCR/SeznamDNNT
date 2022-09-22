package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Fill states in granularity
 * @author happy
 */
public interface GranularitySetStateService {
    
    /**
     * Returns logger of the service 
     * @return
     */
    Logger getLogger();
    
    /**
     * Set sates
     * @param addFilters
     * @throws IOException
     */
    public void setStates(List<String> addFilters) throws IOException;

}
