package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxyException;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.openapi.endpoints.model.DetailMarc;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail.StateEnum;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.utils.StringUtils;

public class InvalidIdentifiersValdation extends DNNTRequestApiServiceValidation {


    public static String GENERIC_ERR_PLACE_MSG = "The following identifiers %s have an incorrect place of publication. Expecting Czech Republic (control field 008, position 15-17, expecting value 'xr').";
    public static String ERR_PLACE_MSG = "The identifier has an incorrect place of publication. Expecting Czech Republic (control field 008, position 15-17, expecting value 'xr').";

    public static String GENERIC_ERR_FORMAT_MSG = "The following identifiers %s have an incorrect format. Expecting a book or a serial (BK, SE).";
    public static String ERR_FORMAT_MSG = "The identifier has an incorrect format. Expecting a book or a serial (BK, SE).";

    public static String GENERIC_ERR_WORKFLOW_MSG = "The following identifiers %s cannot be included in the DNNT list proposal.";
    public static String ERR_WORKFLOW_NZN_MSG = "The identifier %s cannot be included in the DNNT list proposal.";
    public static String ERR_WORKFLOW_VN_MSG = "The identifier %s cannot be added to the proposal.";

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
    public List<Detail> getErrorDetails(String navrh) {
        
        List<Detail> retdetails = new ArrayList<>();
        // nonexistent 
        this.nonExistentIdentifiers.stream().map(id-> {
            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_NONEXISTENT_MSG);
            
            /*
            List<String> format910ax = format910ax(id);
            if (format910ax != null) {
                DetailMarc marc = new DetailMarc();
                format910ax.stream().forEach(marc::addMarc910Item);
                detail.setMarc(marc);
            }*/
            
            return detail;
        }).forEach(retdetails::add);
        // invalid format
        this.invalidFormatIdenfitiers.stream().map(id-> {
            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_FORMAT_MSG);

            /*
            List<String> format910ax = format910ax(id);
            if (format910ax != null) {
                DetailMarc marc = new DetailMarc();
                format910ax.stream().forEach(marc::addMarc910Item);
                detail.setMarc(marc);
            }*/

            return detail;
        }).forEach(retdetails::add);
        // invalid place
        this.invalidPlaceIdentifiers.stream().map(id-> {
            Detail detail = new Detail();
            detail.setIdentifier(id);
            detail.state(StateEnum.REJECTED);
            detail.setReason(ERR_PLACE_MSG);
            
            /*
            List<String> format910ax = format910ax(id);
            if (format910ax != null) {
                DetailMarc marc = new DetailMarc();
                format910ax.stream().forEach(marc::addMarc910Item);
                detail.setMarc(marc);
                
            }*/

            return detail;
        }).forEach(retdetails::add);

        this.invalidStateIdentifiers.stream().map(p-> {
            Detail detail = new Detail();
            detail.setIdentifier(p.getLeft());
            detail.state(StateEnum.REJECTED);
            if (StringUtils.isAnyString(navrh) && (navrh.equals("NZN"))) {
                detail.setReason(String.format(ERR_WORKFLOW_NZN_MSG, p.toString()));
            } else {
                detail.setReason(String.format(ERR_WORKFLOW_VN_MSG, p.toString()));
            }
            
            /*
            List<String> format910ax = format910ax(p.getLeft());
            if (format910ax != null) {
                DetailMarc marc = new DetailMarc();
                format910ax.stream().forEach(marc::addMarc910Item);
                detail.setMarc(marc);
            }*/

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
