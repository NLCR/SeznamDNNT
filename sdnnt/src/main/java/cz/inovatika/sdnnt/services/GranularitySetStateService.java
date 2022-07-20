package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public interface GranularitySetStateService {

    Logger getLogger();
    
    public void setStates(List<String> addFilters) throws IOException;

}
