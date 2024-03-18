package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.MarcRecordFlags;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.StringUtils;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.YEAR_OF_PUBLICATION_1;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.YEAR_OF_PUBLICATION_2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// History in case of destination state
public abstract class AbstractPXService extends AbstractRequestService  {


    public static final List<String> SUPPORTED_STATES = Arrays.asList("N","A","X", "PX");

    public static final int CHECK_SIZE = 90;
    public static final int LIMIT = 1000;

    // iteration properties
    protected String yearConfiguration = null;
    protected List<String> states = new ArrayList<>();
    //
    protected String destinationState;
    protected String loggerPostfix;
    
    protected AbstractPXService() { }

    public AbstractPXService(String loggerPostfix, JSONObject iteration, JSONObject results) {
        this.loggerPostfix = loggerPostfix;
        if (results != null) {
            requestsConfig(results);
        }
        if (iteration != null) {
            iterationConfig(iteration);
        }
    }

    protected void requestsConfig(JSONObject results) {
        if (results != null) {
            destinationState = results.optString("state");
            if (StringUtils.isAnyString(destinationState) && !SUPPORTED_STATES.contains(destinationState)) {
                throw new IllegalStateException("Podporovane stavy pro proces "+SUPPORTED_STATES+" pozdavony stav: "+destinationState);
            }
            if (results.has("request")) {
                typeOfRequest = results.getJSONObject("request").optString("type");
                numberOfItems = results.getJSONObject("request").optInt("items");
            }
        }
    }

    protected void iterationConfig(JSONObject iteration) {
        if (iteration != null) {
            yearConfiguration = iteration.optString("date_range");
            if (iteration.has("states")) {
                JSONArray iterationOverStates = iteration.optJSONArray("states");
                if (iterationOverStates != null) {
                    iterationOverStates.forEach(it -> {
                        states.add(it.toString());
                    });
                }
            }
        }
    }

    // todo: remove
    protected void atomicUpdate(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("set", fValue);
        idoc.addField(fName, modifier);
    }

    protected  abstract Options getOptions();
    protected abstract SolrClient buildClient();

    protected String yearFilter() {
        String first = String.format("(" + YEAR_OF_PUBLICATION_1 + ":%s AND -" + YEAR_OF_PUBLICATION_2 + ":*)", this.yearConfiguration);
        String second = String.format("(" + YEAR_OF_PUBLICATION_1 + ":%s AND " + YEAR_OF_PUBLICATION_2 + ":%s)", this.yearConfiguration,this.yearConfiguration);
        String fq = String.format("(%s OR %s)", first, second);
        return fq;
    }
    
    
    protected void enahanceContextInformation(SolrInputDocument idoc, boolean flag) {
        idoc.addField(MarcRecordFields.FLAG_PUBLIC_IN_DL, flag);
    }

    protected void removeContextInformation(SolrInputDocument idoc, boolean flag) {
        idoc.addField(MarcRecordFields.FLAG_PUBLIC_IN_DL, flag);
    }

    
    protected SolrInputDocument changeContextInformation(SolrClient solr, String identifier, boolean flag) throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mr = MarcRecord.fromIndex(solr, identifier);
        if (mr != null) {
            if (mr.recordsFlags == null) {
                mr.recordsFlags = new MarcRecordFlags(true);
            }
            mr.recordsFlags.setPublicInDl(flag);
            return mr.toSolrDoc();
        } else return null;
    }
    
}
