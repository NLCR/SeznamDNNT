package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.MarcRecordFlags;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.ZadostTyp;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxy;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// History in case of destination state
public abstract class AbstractPXService {

    public static final Logger LOGGER = Logger.getLogger(AbstractPXService.class.getName());


    public static final int CHECK_SIZE = 10;
    public static final int LIMIT = 1000;

    protected String typeOfRequest;
    protected int numberOfItems;
    // iteration properties
    protected String yearConfiguration = null;
    protected List<String> states = new ArrayList<>();
    //
    protected String destinationState;

    protected AbstractPXService() { }

    public AbstractPXService(JSONObject iteration, JSONObject results) {
        if (results != null) {
            requestsConfig(results);
        }
        if (iteration != null) {
            iterationConfig(iteration);
        }
    }

    protected static User getSchedulerUser() {
        User user = new User();
        user.setJmeno("scheduler");
        user.setPrijmeni("scheduler");
        user.setUsername("scheduler");
        return user;
    }




    protected AccountServiceImpl buildAccountService() {
        ApplicationUserLoginSupport appSupport = new StaticApplicationLoginSupport(getSchedulerUser());
        return new AccountServiceImpl(appSupport, null);
    }

    protected void requestsConfig(JSONObject results) {
        if (results != null) {
            destinationState = results.optString("state");
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

    public void request(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException {
        if (!identifiers.isEmpty()) {
            AccountService accountService = buildAccountService();
            JSONObject px = accountService.prepare(this.typeOfRequest);
            Zadost zadost = Zadost.fromJSON(px.toString());
            zadost.setTypeOfRequest(ZadostTyp.scheduler.name());
            identifiers.stream().forEach(zadost::addIdentifier);
            zadost.setState("waiting");
            accountService.schedulerDefinedCloseRequest(zadost.toJSON().toString());
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
    
    protected  SolrInputDocument changeProcessState(SolrClient solrClient, String identifier, String state) throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mr = MarcRecord.fromIndex(solrClient, identifier);
        CuratorItemState kstav = CuratorItemState.valueOf(state);
        PublicItemState pstav = kstav.getPublicItemState(new DocumentProxy(mr));
        if (pstav != null && pstav.equals(PublicItemState.A) || pstav.equals(PublicItemState.PA)) {
          mr.license = License.dnnto.name();
        } else if (pstav != null && pstav.equals(PublicItemState.NL)) {
          mr.license = License.dnntt.name();
        } else {
            mr.license = null;
        }
        mr.setKuratorStav(kstav.name(), pstav.name(), null, "scheduler", "scheduler", new JSONArray());
        return mr.toSolrDoc();
    }

    protected void enahanceContextInformation(SolrInputDocument idoc) {
        idoc.addField(MarcRecordFields.FLAG_PUBLIC_IN_DL, true);
    }

    protected SolrInputDocument changeContextInformation(SolrClient solr, String identifier) throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mr = MarcRecord.fromIndex(solr, identifier);
        if (mr != null) {
            if (mr.recordsFlags == null) {
                mr.recordsFlags = new MarcRecordFlags(true);
            }
            mr.recordsFlags.setPublicInDl(true);
            return mr.toSolrDoc();
        } else return null;
    }
    
}
