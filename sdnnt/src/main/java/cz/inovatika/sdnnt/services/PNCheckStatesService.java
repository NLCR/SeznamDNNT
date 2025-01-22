package cz.inovatika.sdnnt.services;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public interface PNCheckStatesService extends RequestService, LoggerAware { 

    public List<Pair<String, String>> check();

}
