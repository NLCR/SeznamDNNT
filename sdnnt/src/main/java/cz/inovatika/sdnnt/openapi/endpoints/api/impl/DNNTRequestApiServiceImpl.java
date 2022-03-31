package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;
import cz.inovatika.sdnnt.openapi.endpoints.api.*;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.DNNTRequestApiServiceImpl.AlreadyUsedException;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.DNNTRequestApiServiceImpl.EmptyIdentifiers;
import cz.inovatika.sdnnt.openapi.endpoints.model.*;

import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.AbstractUserController;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.ResourceBundleServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.common.util.Pair;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.store.blockcache.ReusedBufferedIndexOutput;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
public class DNNTRequestApiServiceImpl extends RequestApiService {


    public static final Logger LOGGER = Logger.getLogger(DNNTRequestApiServiceImpl.class.getName());

    public static final String API_KEY_HEADER = "X-API-KEY";

    private static final int LIMIT = 10000;


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

                //UserControlerImpl userControler = new UserControlerImpl(null);//.findUserByApiKey(headerString);
                OpenApiLoginSupportImpl openApiLoginSupport = new OpenApiLoginSupportImpl(headerString);

                AccountService accountService = new AccountServiceImpl( openApiLoginSupport, new ResourceBundleServiceImpl(crc));

                if (openApiLoginSupport.getUser() != null) {
                    List<Request> batch = body.getBatch();
                    for (Request req : batch) {

                        try {
                           
                            Zadost mockZadost = new Zadost("-1");
                            mockZadost.setNavrh(navrh);

                            verifyIdentifiers(openApiLoginSupport.getUser(), accountService,mockZadost, req.getIdentifiers());

                            JSONObject prepare = accountService.prepare(navrh);
                            Zadost zadost = Zadost.fromJSON(prepare.toString());

                            if (req.getIdentifiers() != null) {
                                zadost.setIdentifiers(req.getIdentifiers());
                            } else {
                                zadost.setIdentifiers(new ArrayList<>());
                            }

                            zadost.setPozadavek(req.getPozadavek());
                            zadost.setPoznamka(req.getPoznamka());

                            try {
                                JSONObject jsonObject = accountService.userCloseRequest(zadost.toJSON().toString());
                                response.getSaved().add(objectMapper.readValue(jsonObject.toString(), SuccessRequestSaved.class));
                            } catch ( ConflictException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);

                                FailedRequestNotSaved ns = new FailedRequestNotSaved();
                                ns.identifiers(req.getIdentifiers())
                                        //.datumZadani(req.getDatumZadani())
                                        .pozadavek(req.getPozadavek())
                                        .poznamka(req.getPoznamka());

                                response.getNotsaved().add(ns.reason(e.getMessage()));

                            }
                        } catch (AlreadyUsedException e) {
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());

                            response.getNotsaved().add(ns.reason("These identifiers are already used in requests :"+e.getIdents()));
                        } catch (NonExistentIdentifeirsException e) {
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());

                            response.getNotsaved().add(ns.reason("Cannot find identifiers or records don't have 'states' field. Identifiers :"+e.getIdents()));
                        } catch (InvalidIdentifiersException e) {
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());

                            response.getNotsaved().add(ns.reason("Invalid state of these documents: "+e.getIdents()));
                        } catch (SolrServerException e) {
                            // solr exception
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());

                            response.getNotsaved().add(ns.reason("Solr exception: "+e.getMessage()));
                        } catch(EmptyIdentifiers e) {
                            FailedRequestNotSaved ns = new FailedRequestNotSaved();
                            ns.identifiers(req.getIdentifiers())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());

                            response.getNotsaved().add(ns.reason("Empty array of identifiers is not allowed "));
                            
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

    private void verifyIdentifiers(User user, AccountService accountService, Zadost zadost, List<String> identifiers) throws NonExistentIdentifeirsException, InvalidIdentifiersException, IOException, SolrServerException, EmptyIdentifiers, AccountException, AlreadyUsedException {
        if (identifiers.isEmpty()) throw new EmptyIdentifiers();

        List<String> usedByUser = new ArrayList<>();
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

        List<String> nonExistentIdentifiers = new ArrayList<>();
        List<String> invalidIdentifiers = new ArrayList<>();

        for (String documentId :  identifiers) {
            try (SolrClient solr = buildClient()) {
                MarcRecord marcRecord = MarcRecord.fromIndex(solr, documentId);
                if (marcRecord != null) {
                    Workflow workflow = DocumentWorkflowFactory.create(marcRecord, zadost);
                    if (workflow == null) {
                        invalidIdentifiers.add(documentId);
                    }
                } else {
                    nonExistentIdentifiers.add(documentId);
                }
            }
        }
        if (!nonExistentIdentifiers.isEmpty()) throw new NonExistentIdentifeirsException(nonExistentIdentifiers);
        if (!invalidIdentifiers.isEmpty()) throw new InvalidIdentifiersException(invalidIdentifiers);
        if (!usedByUser.isEmpty()) throw new AlreadyUsedException(usedByUser);
   }

    /**
     * Empty or null identifiers exception
     *
     */
    public static class EmptyIdentifiers extends Exception {
        public EmptyIdentifiers() {
        }
    }
    
    /**
     * Non existent item exception
     *
     */
    public static class NonExistentIdentifeirsException extends Exception {
        private List<String> idents;

        public NonExistentIdentifeirsException(List<String> pids) {
            this.idents = pids;
        }

        public List<String> getIdents() {
            return idents;
        }
    }

    /**
     * Invalid identifier exception
     */
    public static class InvalidIdentifiersException extends Exception {
        private List<String> idents;

        public InvalidIdentifiersException(List<String> idents) {
            this.idents = idents;
        }

        public List<String> getIdents() {
            return idents;
        }
    }
    
    
    public static class AlreadyUsedException extends Exception {

        private List<String> idents;

        public AlreadyUsedException(List<String> idents) {
            this.idents = idents;
        }

        public List<String> getIdents() {
            return idents;
        }
    }

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

}
