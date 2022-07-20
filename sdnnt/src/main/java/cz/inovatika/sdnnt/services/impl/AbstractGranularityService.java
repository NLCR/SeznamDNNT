package cz.inovatika.sdnnt.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;

public abstract class AbstractGranularityService {

    protected void atomicAdd(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("add", fValue);
        idoc.addField(fName, modifier);
    }

    protected void atomicSet(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("set", fValue);
        idoc.addField(fName, modifier);
    }
}
