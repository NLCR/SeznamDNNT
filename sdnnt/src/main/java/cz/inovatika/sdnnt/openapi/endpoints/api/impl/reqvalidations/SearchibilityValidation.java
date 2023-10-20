package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.json.JSONObject;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail.StateEnum;
import cz.inovatika.sdnnt.services.AccountService;

public class SearchibilityValidation extends DNNTRequestApiServiceValidation{
    
    public static final String GENERIC_ERR_MESSAGE = "The following records are not accessible: %s!";
    public static final String ERR_MESSAGE = "The record is not accessible.";
    
    private List<String> nonExistentIdentifiers = new ArrayList<>();
    
    
    public SearchibilityValidation(SolrClient solr) {
        super(solr);
    }

    @Override
    public boolean validate(User user, AccountService accountService, Zadost zadost, List<String> identifiers,
            CatalogSearcher catalogSearcher)  {

        // validace na dohledatelnost #461
        for (String documentId :  identifiers) {
            Map<String, String> parameters = new HashMap<>();
            // proverit ?? 
            parameters.put("fullCatalog", "true");
            parameters.put("q", documentId);
            JSONObject search = catalogSearcher.search(parameters, new ArrayList<>(), user);
            if (search.has("response")) {
                JSONObject response = search.getJSONObject("response");
                if (response.has("numFound")) {
                    if (response.has("numFound")) {
                        int numFound = response.getInt("numFound");
                        if (numFound == 0) {
                            nonExistentIdentifiers.add(documentId);
                        }
                    }
                }
            }
        }
        
        
        return nonExistentIdentifiers.isEmpty();
    }

    @Override
    public String getErrorMessage() {
        return String.format(GENERIC_ERR_MESSAGE, this.nonExistentIdentifiers.toString());
    }

    
    
//    @Override
//    public DividedIdentifiers divideIdentifiers(List<String> identifiers) {
//        List<String> valid = new ArrayList<>();
//
//        identifiers.forEach(it-> {
//            if (!nonExistentIdentifiers.contains(it)) {
//                valid.add(it);
//            }
//        });
//        
//        this.dividedIdentifiers =  new DividedIdentifiers(valid, this.nonExistentIdentifiers);
//        return this.dividedIdentifiers;
//    }
//
//    @Override
//    public DividedIdentifiers getDividedIdentifiers() {
//        return this.dividedIdentifiers;
//    }
    
    
    
    
    @Override
    public boolean isSoftValidation() {
        return true;
    }

    @Override
    public List<Detail> getErrorDetails(String navrh) {
        return this.nonExistentIdentifiers.stream().map(id-> {
            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_MESSAGE);
            return detail;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getInvalidIdentifiers() {
        return this.nonExistentIdentifiers;
    }
}
