package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Retrieve granularity from kramerius instances s
 * @author happy
 *
 */
public interface GranularityService  {
        
    /**
     * Returns logger of the service
     * @return
     */
    Logger getLogger();
    

    /**
     * Refreshing granularities
     * @throws IOException
     */
    public void refershGranularity() throws IOException;
}
