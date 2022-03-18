package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.ZadostTyp;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

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

}
