package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.common.util.Pair;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxyException;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateUtils;
import cz.inovatika.sdnnt.openapi.endpoints.api.ApiResponseMessage;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
import cz.inovatika.sdnnt.openapi.endpoints.api.RFC3339DateFormat;
import cz.inovatika.sdnnt.openapi.endpoints.api.RequestApiService;
import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfDetails;
import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfFailedRequest;
import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfSavedRequest;
import cz.inovatika.sdnnt.openapi.endpoints.model.BatchRequest;
import cz.inovatika.sdnnt.openapi.endpoints.model.BatchResponse;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.openapi.endpoints.model.FailedRequestNotSaved;
import cz.inovatika.sdnnt.openapi.endpoints.model.Request;
import cz.inovatika.sdnnt.openapi.endpoints.model.SuccessRequestSaved;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail.StateEnum;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.AbstractUserController;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.ResourceBundleServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;


@Service
public class DNNTRequestApiServiceImpl extends RequestApiService {


    public static final Logger LOGGER = Logger.getLogger(DNNTRequestApiServiceImpl.class.getName());

    public static final String API_KEY_HEADER = "X-API-KEY";

    private static final int LIMIT = 10000;

    CatalogSearcher catalogSearcher = new CatalogSearcher();


    @Override
    public Response requestBatchVnl(BatchRequest batchRequest, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return request(containerRequestContext,  batchRequest, "VNL");
    }

    @Override
    public Response requestBatchVnz(BatchRequest batchRequest, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return request(containerRequestContext,  batchRequest, "VNZ");
    }

    @Override
    public Response requestBatchNzn(BatchRequest batchRequest, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return request(containerRequestContext,  batchRequest, "NZN");
    }


    @Override
    public Response requestBatchVN(BatchRequest batchRequest, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return request(containerRequestContext,  batchRequest, "VN");
    }

    @Override
    public Response requestGet(String status, String internalStatus,String navrh, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String headerString = containerRequestContext.getHeaderString(API_KEY_HEADER);
        if (headerString != null) {
            AbstractUserController userControler = new UserControlerImpl(null);//.findUserByApiKey(headerString);
            OpenApiLoginSupportImpl loginSupport = new OpenApiLoginSupportImpl(headerString);
            if (loginSupport.getUser() != null) {
                ArrayOfSavedRequest arrayOfSavedRequest = new ArrayOfSavedRequest();
                try {

                    AccountService accountService = new AccountServiceImpl( loginSupport , new ResourceBundleServiceImpl(containerRequestContext));
                    JSONObject search = accountService.search(null, status, Arrays.asList(navrh), null, null, null, null, null, LIMIT, 0);
                    JSONObject response = search.getJSONObject("response");
                    JSONArray docs = response.getJSONArray("docs");
                    for (int i = 0, ll = docs.length();i<ll;i++) {

                        JSONObject jsonObject = docs.getJSONObject(i);
                        ArrayOfDetails details = details(jsonObject, internalStatus);
                        ObjectMapper objectMapper = getObjectMapper();
                        SuccessRequestSaved savedRequest = objectMapper.readValue(jsonObject.toString(), SuccessRequestSaved.class);
                        if (!details.isEmpty()) {
                            savedRequest.setDetails(details);
                        }
                        arrayOfSavedRequest.add(savedRequest);
                    }
                    return Response.ok().entity(arrayOfSavedRequest).build();

                } catch (SolrServerException | IOException | AccountException e) {
                    LOGGER.log(Level.SEVERE,e.getMessage());
                    return Response.accepted().status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
                }
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();
        }
    }

    private ArrayOfDetails details(JSONObject jsonObject, String intStatus) {
        if (jsonObject.has("process")) {
            JSONObject process = jsonObject.getJSONObject("process");
            ArrayOfDetails details = new ArrayOfDetails();
            process.keySet().stream().forEach(key -> {
                JSONObject detailJSON = process.getJSONObject(key);
                Detail.StateEnum state = Detail.StateEnum.fromValue(detailJSON.optString("state"));
                if (intStatus == null || (state != null && state.toString().equals(intStatus))) {
                    Detail detail =  new Detail()
                            .identifier(key)
                            .reason(detailJSON.optString("reason"))
                            .state(Detail.StateEnum.fromValue(detailJSON.optString("state")))
                            .user(detailJSON.optString("user"));
                    details.add(detail);
                }
            });
            return details;
        } else {
            return new ArrayOfDetails();
        }
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new RFC3339DateFormat());
        return objectMapper;
    }


    private Response request(ContainerRequestContext crc, BatchRequest body, String navrh) {
        try {
            BatchResponse response = new BatchResponse();
            response.setNotsaved(new ArrayOfFailedRequest());
            response.setSaved(new ArrayOfSavedRequest());

            ObjectMapper objectMapper = getObjectMapper();

            String headerString = crc.getHeaderString(API_KEY_HEADER);
            if (headerString != null) {
                OpenApiLoginSupportImpl openApiLoginSupport = new OpenApiLoginSupportImpl(headerString);

                AccountService accountService = new AccountServiceImpl( openApiLoginSupport, new ResourceBundleServiceImpl(crc));
                // validation results 
                List<String> alreadyUsedIdentifiers= new ArrayList<>();
                List<String> notExistentIdentifiers= new ArrayList<>();
                List<String> invalidFormatIdentifiers = new ArrayList<>();  
                List<String> invalidPlaceIdentifiers = new ArrayList<>();
                List<Pair<String,String>> invalidStateIdentifiers =  new ArrayList<>();

                if (openApiLoginSupport.getUser() != null) {
                    List<Request> batch = body.getBatch();
                    for (Request req : batch) {

                        try (SolrClient solr = buildClient()) {
                           
                            Zadost mockZadost = new Zadost("-1");
                            mockZadost.setNavrh(navrh);

                            // remap identifiers
                            req.setIdentifiers(req.getIdentifiers().stream().map(it-> {
                                try {
                                    return findId(it, solr);
                                } catch (SolrServerException | IOException e) {
                                    LOGGER.log(Level.SEVERE,e.getMessage(),e);
                                    return it;
                                }
                              }).collect(Collectors.toList()));

                            
                            List<String> identifiers = req.getIdentifiers();
                            
                            try {
                                verifyIdentifiers(solr, openApiLoginSupport.getUser(), accountService,mockZadost, identifiers);
                            } catch(BadFieldsValidationFailedException ie) {
                            
                                invalidFormatIdentifiers = ie.getInvalidFormatIdentifiers();
                                identifiers.removeAll(invalidFormatIdentifiers);
                                
                                invalidPlaceIdentifiers = ie.getInvalidPlaceIdentifiers();
                                identifiers.removeAll(invalidPlaceIdentifiers);
                                
                                invalidStateIdentifiers = ie.getInvalidStateIdentifiers();
                                identifiers.removeAll(invalidStateIdentifiers);
                                
                                alreadyUsedIdentifiers = ie.getAlreadyUsedIdents();
                                identifiers.removeAll(alreadyUsedIdentifiers);
                                
                                notExistentIdentifiers = ie.getNotExistentIdents();
                                identifiers.removeAll(notExistentIdentifiers);
                            }

                            JSONObject prepare = accountService.prepare(navrh);
                            Zadost zadost = Zadost.fromJSON(prepare.toString());

                            if (identifiers != null) {
                                zadost.setIdentifiers(identifiers);
                            } else {
                                zadost.setIdentifiers(new ArrayList<>());
                            }

                            zadost.setPozadavek(req.getPozadavek());
                            zadost.setPoznamka(req.getPoznamka());

                            try {
                                JSONObject jsonObject = accountService.userCloseRequest(zadost.toJSON().toString());
                                SuccessRequestSaved readValue = objectMapper.readValue(jsonObject.toString(), SuccessRequestSaved.class);
                                
                                ArrayOfDetails details = details(alreadyUsedIdentifiers,  notExistentIdentifiers, invalidFormatIdentifiers, invalidPlaceIdentifiers, invalidStateIdentifiers, openApiLoginSupport.getUser());
                                if (!details.isEmpty()) {
                                    readValue.setDetails(details);
                                }
                                
                                if (readValue.getIdentifiers() == null || readValue.getIdentifiers().isEmpty()) {
                                    throw new BadRequestEmptyIdentifiersException();
                                }
                                

                                response.getSaved().add(readValue);
                            } catch ( ConflictException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);

                                FailedRequestNotSaved ns = new FailedRequestNotSaved();
                                ns.identifiers(req.getIdentifiers())
                                        .pozadavek(req.getPozadavek())
                                        .poznamka(req.getPoznamka());

                                response.getNotsaved().add(ns.reason(e.getMessage()));

                            }
                        }catch (BadRequestMaximumNumberOfIdentifiersExceeded e) {
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());
                            response.getNotsaved().add(ns.reason("Maximum number of items exceeded. Number of items:"+e.getNumberOfIdentifiers()+". Maximum: "+e.getMaximum()));
                        } catch (SolrServerException e) {
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());
                            
                            
                            response.getNotsaved().add(ns.reason("Solr exception: "+e.getMessage()));
                        } catch(BadRequestEmptyIdentifiersException e) {
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());
                            
                            
                            ArrayOfDetails details = details(alreadyUsedIdentifiers, notExistentIdentifiers, invalidFormatIdentifiers, invalidPlaceIdentifiers,
                                    invalidStateIdentifiers, openApiLoginSupport.getUser());
                            if (!details.isEmpty()) {
                                ns.setDetails(details);
                            }

                            response.getNotsaved().add(ns.reason("Nothing to save !"));
                            
                        } catch (ConflictException e) {
                            // conflict
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());

                            response.getNotsaved().add(ns.reason("Conflict: "+e.getMessage()));
                        }

                    }
                    return Response.ok().entity(response).build();
                }
            }
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();
        } catch (IOException | AccountException e) {
            return Response.accepted().status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
    }

    private ArrayOfDetails details(List<String> alreadyUsedIdentifiers, List<String> notExistentIdentifiers, List<String> invalidFormatIdentifiers,
            List<String> invalidPlaceIdentifiers, List<Pair<String,String>> invalidStateIdentifiers, User user) {
        ArrayOfDetails details = new ArrayOfDetails();
        
        notExistentIdentifiers.stream().forEach(ni-> {
            Detail detail = new Detail();
            detail.setIdentifier(ni);
            detail.state(StateEnum.REJECTED);
            detail.setReason("Identifier doesnt exists");
            details.add(detail);
        });
        
        
        invalidFormatIdentifiers.stream().forEach(iI -> {
            Detail detail = new Detail();
            detail.setIdentifier(iI);
            detail.state(StateEnum.REJECTED);
            detail.setReason("Invalid format (!BK AND !SE)");
            details.add(detail);
            
        });
        
        
        invalidPlaceIdentifiers.stream().forEach(iP-> {
            Detail detail = new Detail();
            detail.setIdentifier(iP);
            detail.state(StateEnum.REJECTED);
            detail.setReason("Invalid place of publication (!xr)");
            details.add(detail);
            
        });
        invalidStateIdentifiers.stream().forEach(iS-> {

            Detail detail = new Detail();
            detail.setIdentifier(iS.getLeft());
            detail.state(StateEnum.REJECTED);
            
            detail.setReason("Invalid state '"+iS.getRight()+"'");
            details.add(detail);
            
        });
        
        alreadyUsedIdentifiers.stream().forEach(iS-> {

            Detail detail = new Detail();
            detail.setIdentifier(iS);
            detail.state(StateEnum.REJECTED);
            
            detail.setReason("Already used in request '"+iS+"'");
            details.add(detail);
            
        });

        return details;
    }

    private void verifyIdentifiers(SolrClient solr, User user, AccountService accountService, Zadost zadost, List<String> identifiers) throws  IOException, SolrServerException, BadRequestEmptyIdentifiersException, AccountException, BadRequestMaximumNumberOfIdentifiersExceeded, BadFieldsValidationFailedException {
        if (identifiers.isEmpty()) throw new BadRequestEmptyIdentifiersException();
        
       JSONObject apiObject = Options.getInstance().getJSONObject("api");
        if (apiObject != null) {
            int maximum = apiObject.optInt("maximumItemInRequest",-1);
            if (maximum > -1 ) {
                if (identifiers.size() > maximum) {
                    throw new BadRequestMaximumNumberOfIdentifiersExceeded(identifiers.size(), maximum);   
                }
            }
        }
        
        // Issue 430 // only remove duplicates
        if (!identifiers.isEmpty()) {
            Set<String> linkedHashSet = new LinkedHashSet<>(identifiers);
            identifiers = new ArrayList<>(linkedHashSet);
        }

        List<String> usedByUser = new ArrayList<>();
        List<String> usedStates = Arrays.asList(
                "open",
                "waiting",
                "waiting_for_automatic_process"
        );
        

        List<String> nonExistentIdentifiers = new ArrayList<>();
        List<String> invalidFormatIdentifiers = new ArrayList<>();
        List<String> invalidPlaceIdentifiers = new ArrayList<>();
        List<Pair<String,String>> invalidStateIdentifiers = new ArrayList<>();
        List<String> allUsed = new ArrayList<>();
        
        Set<String> invalidIdentifiers = new HashSet<>();
        

        // validace na jiz pouzite v zadostech
        accountService.findIdentifiersUsedInRequests(user.getUsername(), usedStates);
        identifiers.stream().forEach(ident-> {
            if (allUsed.contains(ident)) {
                usedByUser.add(ident);
            }
        });


        // validace na dntstav, place_of_pub, format
        for (String documentId :  identifiers) {
            MarcRecord marcRecord = MarcRecord.fromIndex(solr, documentId);
            if (marcRecord != null) {
                try {
                    Workflow workflow = DocumentWorkflowFactory.create(marcRecord, zadost);
                    //marcRecord.dntstav
                    if (workflow == null || (workflow.nextState() != null && !workflow.nextState().isFirstTransition())) {
                        invalidStateIdentifiers.add(Pair.of(documentId, marcRecord.dntstav != null && marcRecord.dntstav.size() > 0 ? marcRecord.dntstav.get(0) : "none"));
                        invalidIdentifiers.add(documentId);
                    }

                    SolrDocument solrDoc = solr.getById(DataCollections.catalog.name(), documentId);
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
                                invalidPlaceIdentifiers.add(documentId);
                            }
                        }
                    }
                } catch (DocumentProxyException e) {
                    LOGGER.log(Level.SEVERE,e.getMessage());
                }
            } else {
                nonExistentIdentifiers.add(documentId);
            }
        }
        
        // validace na dohledatelnost #461
        for (String documentId :  identifiers) {
            Map<String, String> parameters = new HashMap<>();
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
        
        
        if (!nonExistentIdentifiers.isEmpty() || 
                !invalidIdentifiers.isEmpty() || 
                !usedByUser.isEmpty()) {
       
            throw new BadFieldsValidationFailedException(
                    allUsed, 
                    nonExistentIdentifiers,
                    invalidFormatIdentifiers,
                    invalidPlaceIdentifiers,
                    invalidStateIdentifiers);
        }
        
        
   }

    private String findId(String documentId, SolrClient solr) throws SolrServerException, IOException {
        if (!documentId.startsWith("oai:aleph-nkp.cz") && documentId.contains("-")) {
            String[] splitted = documentId.split("-");
            if (splitted.length >= 2) {
                String m910a = splitted[0];
                String m910x = splitted[1];
                List<String> found = DuplicateUtils.findBy910ax(solr, m910a, m910x);
                if (found.size() == 1) {
                    documentId = found.get(0);
                }
            }
        }
        return documentId;
    }
    
    // 
    public static class BadRequestEmptyIdentifiersException extends Exception {
        public BadRequestEmptyIdentifiersException() {
        }
    }
    
    public static class BadRequestMaximumNumberOfIdentifiersExceeded extends Exception {
        private int numberOfIdentifiers = -1;
        private int maximum = -1;
        
        public BadRequestMaximumNumberOfIdentifiersExceeded(int numberOfIdentifiers, int maximum) {
            super();
            this.numberOfIdentifiers = numberOfIdentifiers;
            this.maximum = maximum;
        }

        public int getMaximum() {
            return maximum;
        }

        public int getNumberOfIdentifiers() {
            return numberOfIdentifiers;
        }
    }

    public static class BadFieldsValidationFailedException extends Exception {
        
        //private List<String> invalidIdents;
        private List<String> alreadyUsedIdents;
        private List<String> notExistentIdents;
        
        private List<String> invalidFormatIdentifiers;
        private List<String> invalidPlaceIdentifiers;
        private List<Pair<String,String>> invalidStateIdentifiers;
        

        
        public BadFieldsValidationFailedException(List<String> alreadyUsedIdents,
                List<String> notExistentIdents, 
                List<String> invalidFormatIdentifiers,
                List<String> invalidPlaceIdentifiers,
                List<Pair<String,String>> invalidStateIdentifiers) {
            super();
            this.alreadyUsedIdents = alreadyUsedIdents;
            this.notExistentIdents = notExistentIdents;
            
            this.invalidFormatIdentifiers = invalidFormatIdentifiers;
            this.invalidPlaceIdentifiers = invalidPlaceIdentifiers;
            this.invalidStateIdentifiers = invalidStateIdentifiers;
        }
        
        public List<String> getInvalidFormatIdentifiers() {
            return invalidFormatIdentifiers;
        }
        
        public List<String> getInvalidPlaceIdentifiers() {
            return invalidPlaceIdentifiers;
        }
        
        public List<Pair<String, String>> getInvalidStateIdentifiers() {
            return invalidStateIdentifiers;
        }
        
        public List<String> getAlreadyUsedIdents() {
            return alreadyUsedIdents;
        }
        
        public List<String> getNotExistentIdents() {
            return notExistentIdents;
        }
    }
    

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
