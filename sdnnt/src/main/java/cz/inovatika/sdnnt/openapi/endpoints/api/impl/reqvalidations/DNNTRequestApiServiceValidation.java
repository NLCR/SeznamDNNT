package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.services.AccountService;

public abstract class DNNTRequestApiServiceValidation {
    
    public static final Logger LOGGER = Logger.getLogger(DNNTRequestApiServiceValidation.class.getName());
    
    
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

    /*
     *         
        Set<String> identifiers = new LinkedHashSet<>();
        SolrQuery idQuery = new SolrQuery("*").setRows(100);
        idQuery = idQuery.addFilterQuery(String.format("marc_910a:%s AND marc_910x:%s",marc910a, marc910x));
        idQuery = idQuery.addFilterQuery("fmt:SE OR fmt:BK").setRows(1000);
        LOGGER.info("Query: "+idQuery);
        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();

     */
    
    protected List<String> format910ax(String identifier)  {
        List<String> m910 = new ArrayList<>();
        try {
            SolrDocument sDocument = documentById(identifier);
            if(sDocument != null) {
                String fValue = sDocument.getFieldValue("raw").toString();
                JSONObject rawJSON = new JSONObject(fValue);
                JSONObject dataFields = rawJSON.getJSONObject("dataFields");
                if (dataFields.has("910")) {
                    JSONArray m910Fields = dataFields.getJSONArray("910");
                    for (int i = 0; i < m910Fields.length(); i++) {
                        JSONObject one910Field = m910Fields.getJSONObject(i);

                        List<Triple<String,String,Integer>> subFieldsPairs = new ArrayList<>();
                        JSONObject subFields = one910Field.getJSONObject("subFields");
                        subFields.keySet().forEach(key-> {
                           JSONArray keyArray = subFields.getJSONArray(key);
                           for (int j = 0; j < keyArray.length(); j++) {
                               JSONObject subFieldObject = keyArray.getJSONObject(j);
                               Integer index = subFieldObject.optInt("index",0);
                               String value = subFieldObject.getString("value");
                               subFieldsPairs.add(Triple.of(key, value,index));
                           }
                        });
                        
                        List<String> collected = subFieldsPairs.stream().sorted((l,r)-> {
                            Integer leftI = l.getRight();
                            Integer rightI = r.getRight();
                            return Integer.compare(leftI, rightI);
                        }).map(t-> {
                            return "|"+t.getLeft()+" "+t.getMiddle();
                        }).collect(Collectors.toList());
                        
                        m910.add(String.format("910    %s", collected.stream().collect(Collectors.joining(" "))));
                    }
                }
            }
            
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        } catch (SolrServerException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }
        return m910; 
    }

    protected SolrDocument documentById(String documentId) throws SolrServerException, IOException {
        return getSolr().getById(DataCollections.catalog.name(), documentId);
    }

    protected MarcRecord markRecordFromSolr(String documentId) throws JsonProcessingException, SolrServerException, IOException {
        return MarcRecord.fromIndex(getSolr(), documentId);
    }
}
