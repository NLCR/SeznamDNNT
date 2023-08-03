package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.solr.client.solrj.SolrClient;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.services.AccountService;

public abstract class DNNTRequestApiServiceValidation {
    
    public static class DividedIdentifiers {
        
        private List<String> valid;
        private List<String> invalid;
        
        public DividedIdentifiers(List<String> valid, List<String> invalid) {
            super();
            this.valid = valid;
            this.invalid = invalid;
        }
        
        public List<String> getInvalid() {
            return invalid;
        }
        
        public List<String> getValid() {
            return valid;
        }

        @Override
        public int hashCode() {
            return Objects.hash(invalid, valid);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DividedIdentifiers other = (DividedIdentifiers) obj;
            return Objects.equals(invalid, other.invalid) && Objects.equals(valid, other.valid);
        }
    }
    
    private SolrClient solr;
    
    public DNNTRequestApiServiceValidation(SolrClient solr) {
        super();
        this.solr = solr;
    }

    public SolrClient getSolr() {
        return solr;
    }

    // return true or false
    public abstract boolean validate(User user, AccountService accountService, Zadost zadost, List<String> identifiers, CatalogSearcher catalogSearcher) throws Exception;

    public abstract String getErrorMessage();
    
    public abstract List<Detail> getErrorDetails();
    
//    public abstract DividedIdentifiers  divideIdentifiers(List<String> identifiers);
//    
//    // must be removable 
//    public abstract DividedIdentifiers getDividedIdentifiers();

    // soft validation - invalid identifiers can be removed and result is stored
    // hard validation - nothing to store
    public abstract boolean isSoftValidation();

    
    public abstract List<String> getInvalidIdentifiers();
    
    public List<String> getValidIdentifiers(List<String> identifiers) {
        List<String> invalid = getInvalidIdentifiers();
        List<String> retval = new ArrayList<>();
        identifiers.forEach(id-> {
            if (!invalid.contains(id)) {
                retval.add(id);
            }
        });
        return retval;
    }

}
