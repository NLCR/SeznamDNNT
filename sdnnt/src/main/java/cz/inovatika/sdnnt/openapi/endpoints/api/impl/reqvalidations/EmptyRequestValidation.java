package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.DNNTRequestApiServiceImpl.BadRequestEmptyIdentifiersException;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations.DNNTRequestApiServiceValidation.DividedIdentifiers;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.services.AccountService;

public class EmptyRequestValidation extends DNNTRequestApiServiceValidation {

    public static final String ERR_MESSAGE = "The request must contain at least one identifier!";
    
    public EmptyRequestValidation(SolrClient solr) {
        super(solr);
    }

    @Override
    public boolean validate(User user, AccountService accountService, Zadost zadost, List<String> identifiers,
            CatalogSearcher catalogSearcher) {
        if (identifiers.isEmpty()) {
            return false;
        } else return true;
    }

    @Override
    public String getErrorMessage() {
        return ERR_MESSAGE;
    }

    
    @Override
    public List<Detail> getErrorDetails() {
        // TODO Auto-generated method stub
        return null;
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
