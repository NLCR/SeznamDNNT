package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations.DNNTRequestApiServiceValidation.DividedIdentifiers;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail.StateEnum;
import cz.inovatika.sdnnt.services.AccountService;

public class UserUsedIdentifierValidation extends DNNTRequestApiServiceValidation {
    
    private static String GENERIC_ERR_MESSAGE = "The identifiers %s are already used in other requests!";
    private static String ERR_MESSAGE = "The identifier is already used in other requests!";
    
    private List<String> usedByUser = new ArrayList<>();
    
    
    public UserUsedIdentifierValidation(SolrClient solr) {
        super(solr);
    }

    @Override
    public boolean validate(User user, AccountService accountService, Zadost zadost, List<String> identifiers,
            CatalogSearcher catalogSearcher)  throws Exception{

        List<String> usedStates = Arrays.asList(
                "open",
                "waiting",
                "waiting_for_automatic_process"
        );

        List<String> allUsed = accountService.findIdentifiersUsedInRequests(user.getUsername(), usedStates);
        identifiers.stream().forEach(ident-> {
            if (allUsed.contains(ident)) {
                usedByUser.add(ident);
            }
        });
        
        return usedByUser.isEmpty();
    }

    @Override
    public String getErrorMessage() {
        return String.format(GENERIC_ERR_MESSAGE, this.usedByUser);
    }

//    @Override
//    public DividedIdentifiers getDividedIdentifiers() {
//        return this.dividedIdentifiers;
//    }
//    
//    
//
//    @Override
//    public DividedIdentifiers divideIdentifiers(List<String> identifiers) {
//        List<String> valid = new ArrayList<>();
//        identifiers.forEach(it-> {
//            if (!usedByUser.contains(it)) {
//                valid.add(it);
//            }
//        });
//        this.dividedIdentifiers =  new DividedIdentifiers(valid, this.usedByUser);
//        return this.dividedIdentifiers;
//    }


    
    
    @Override
    public boolean isSoftValidation() {
        return true;
    }

    @Override
    public List<Detail> getErrorDetails() {
        return this.usedByUser.stream().map(id-> {

            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_MESSAGE);
            return detail;
            
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getInvalidIdentifiers() {
        return this.usedByUser;
    }
}
