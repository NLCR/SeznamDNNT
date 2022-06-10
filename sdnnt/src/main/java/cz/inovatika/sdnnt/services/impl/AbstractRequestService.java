package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONObject;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.ZadostTyp;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.RequestService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;

public abstract class AbstractRequestService implements RequestService {

    public static final int DEFAULT_NUMBER_ITEMS_FOR_UPDATE = 10000;
    public static final int DEFAULT_NUMBER_OF_ITEMS_REQ = 30;
    
    protected String typeOfRequest;
    protected int numberOfItems = DEFAULT_NUMBER_OF_ITEMS_REQ;

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

    
    
    protected abstract SolrClient buildClient();

}
