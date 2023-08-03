package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.DNNTRequestApiServiceImpl.BadRequestMaximumNumberOfIdentifiersExceeded;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations.DNNTRequestApiServiceValidation.DividedIdentifiers;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.services.AccountService;

public class MaximumSizeExceededDNNTRequestValidation extends DNNTRequestApiServiceValidation{

    public static final String ERR_MESSAGE = "The maximum number of identifiers in the request has been exceeded.";
    
    
    
    public MaximumSizeExceededDNNTRequestValidation(SolrClient solr) {
        super(solr);
    }

    @Override
    public boolean validate(User user, AccountService accountService, Zadost zadost, List<String> identifiers,
            CatalogSearcher catalogSearcher) {
        
        int maximum = maximumIntInRequest();
        if (maximum > -1) {
            if (identifiers.size() > maximum) return false;
        }
        
        return true;
    }
    
    protected int maximumIntInRequest() {
        JSONObject apiObject = Options.getInstance().getJSONObject("api");
        if (apiObject != null) {
            int maximum = apiObject.optInt("maximumItemInRequest",-1);
            return maximum;
        }
        return -1;
   }

    @Override
    public String getErrorMessage() {
        return ERR_MESSAGE;
    }

    
    
    

    @Override
    public List<Detail> getErrorDetails() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getInvalidIdentifiers() {
        return new ArrayList<>();
    }

    @Override
    public boolean isSoftValidation() {
        return false;
    }
}
