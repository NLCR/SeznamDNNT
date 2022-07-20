package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.logging.Logger;

public interface GranularityService  {
    
    Logger getLogger();
    
    public void refershGranularity() throws IOException;
}
