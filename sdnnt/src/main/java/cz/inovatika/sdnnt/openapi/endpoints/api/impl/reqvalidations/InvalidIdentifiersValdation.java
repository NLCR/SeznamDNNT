package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxyException;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations.DNNTRequestApiServiceValidation.DividedIdentifiers;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail.StateEnum;
import cz.inovatika.sdnnt.services.AccountService;

public class InvalidIdentifiersValdation extends DNNTRequestApiServiceValidation {


    public static String GENERIC_ERR_PLACE_MSG = "The following identifiers %s have an incorrect place of publication. Expecting Czech Republic (control field 008, position 15-17, expecting value 'xr').";
    public static String ERR_PLACE_MSG = "The identifier has an incorrect place of publication. Expecting Czech Republic (control field 008, position 15-17, expecting value 'xr').";

    public static String GENERIC_ERR_FORMAT_MSG = "The following identifiers %s have an incorrect format. Expecting a book or a serial (BK, SE).";
    public static String ERR_FORMAT_MSG = "The identifier has an incorrect format. Expecting a book or a serial (BK, SE).";

    public static String GENERIC_ERR_WORKFLOW_MSG = "The following identifiers %s cannot be included in the DNNT list proposal.";
    public static String ERR_WORKFLOW_MSG = "The identifier %s cannot be included in the DNNT list proposal.";

    public static String GENERIC_ERR_NONEXISTENT_MSG = "The following records %s do not exist.";
    public static String ERR_NONEXISTENT_MSG = "The record does not exist.";
    
    public static final Logger LOGGER = Logger.getLogger(InvalidIdentifiersValdation.class.getName());

    private List<Pair<String, String>> invalidStateIdentifiers = new ArrayList<>();
    private List<String> invalidIdentifiers = new ArrayList<>();

    private List<String> invalidPlaceIdentifiers = new ArrayList<>();
    private List<String> nonExistentIdentifiers = new ArrayList<>();
    private List<String> invalidFormatIdenfitiers = new ArrayList<>();

    
    
    public InvalidIdentifiersValdation(SolrClient solr) {
        super(solr);
    }

    @Override
    public boolean validate(User user, AccountService accountService, Zadost zadost, List<String> identifiers,
            CatalogSearcher catalogSearcher) throws Exception {

        // validace na dntstav, place_of_pub, format
        for (String documentId : identifiers) {
            MarcRecord marcRecord = markRecordFromSolr( documentId);
            if (marcRecord != null) {
                try {
                    Workflow workflow = DocumentWorkflowFactory.create(marcRecord, zadost);
                    // marcRecord.dntstav
                    if (workflow == null
                            || (workflow.nextState() != null && !workflow.nextState().isFirstTransition())) {
                        invalidStateIdentifiers.add(Pair.of(documentId,
                                marcRecord.dntstav != null && marcRecord.dntstav.size() > 0 ? marcRecord.dntstav.get(0)
                                        : "none"));
                        invalidIdentifiers.add(documentId);
                    }

                    SolrDocument solrDoc = documentById(documentId);
                    String placeOfPub = (String) solrDoc.getFieldValue("place_of_pub");
                    if (placeOfPub != null) {
                        if (!placeOfPub.trim().toLowerCase().equals("xr")) {
                            if (!invalidIdentifiers.contains(documentId)) {
                                invalidPlaceIdentifiers.add(documentId);
                                invalidIdentifiers.add(documentId);
                            }
                        }
                    }
                    String format = (String) solrDoc.getFieldValue("fmt");
                    if (format != null) {
                        if (!format.trim().toUpperCase().equals("BK") && !format.trim().toUpperCase().equals("SE")) {
                            if (!invalidIdentifiers.contains(documentId)) {
                                invalidIdentifiers.add(documentId);
                                invalidFormatIdenfitiers.add(documentId);
                                //invalidPlaceIdentifiers.add(documentId);
                            }
                        }
                    }
                } catch (DocumentProxyException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage());
                }
            } else {
                invalidIdentifiers.add(documentId);
                nonExistentIdentifiers.add(documentId);
            }

        }
        return invalidIdentifiers.isEmpty();
    }

    protected SolrDocument documentById( String documentId) throws SolrServerException, IOException {
        return getSolr().getById(DataCollections.catalog.name(), documentId);
    }

    protected MarcRecord markRecordFromSolr( String documentId)
            throws JsonProcessingException, SolrServerException, IOException {
        return MarcRecord.fromIndex(getSolr(), documentId);
    }

    @Override
    public String getErrorMessage() {
        List<String> messages = new ArrayList<>();
        if (!this.invalidPlaceIdentifiers.isEmpty()) {
            messages.add(String.format(GENERIC_ERR_PLACE_MSG, this.invalidPlaceIdentifiers));
        }
        if (!this.invalidStateIdentifiers.isEmpty()) {
            messages.add(String.format(GENERIC_ERR_WORKFLOW_MSG, this.invalidStateIdentifiers));
        }        
        if (!this.invalidFormatIdenfitiers.isEmpty()) {
            messages.add(String.format(GENERIC_ERR_FORMAT_MSG, this.invalidFormatIdenfitiers));
        }        
        if (!this.nonExistentIdentifiers.isEmpty()) {
            messages.add(String.format(GENERIC_ERR_NONEXISTENT_MSG, this.nonExistentIdentifiers));
        }        
        return messages.stream().collect(Collectors.joining("\n"));
    }

    
    
    
    
    @Override
    public List<Detail> getErrorDetails() {
        List<Detail> retdetails = new ArrayList<>();
        
        // nonexistent 
        this.nonExistentIdentifiers.stream().map(id-> {
            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_NONEXISTENT_MSG);
            return detail;
        }).forEach(retdetails::add);
        // invalid format
        this.invalidFormatIdenfitiers.stream().map(id-> {
            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_FORMAT_MSG);
            return detail;
        }).forEach(retdetails::add);
        // invalid place
        this.invalidPlaceIdentifiers.stream().map(id-> {
            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_PLACE_MSG);
            return detail;
        }).forEach(retdetails::add);

        this.invalidStateIdentifiers.stream().map(p-> {
            Detail detail = new Detail();
            detail.setIdentifier(p.getLeft());
            detail.state(StateEnum.REJECTED);
            detail.setReason(String.format(ERR_WORKFLOW_MSG, p.toString()));
            return detail;
        }).forEach(retdetails::add);
        
        return retdetails;
    }

    @Override
    public List<String> getInvalidIdentifiers() {
        return this.invalidIdentifiers;
    }

    @Override
    public boolean isSoftValidation() {
        return true;
    }
    
    
}
