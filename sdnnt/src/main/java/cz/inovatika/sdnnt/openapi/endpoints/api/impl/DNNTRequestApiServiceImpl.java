package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.openapi.endpoints.api.*;

import cz.inovatika.sdnnt.openapi.endpoints.model.*;

import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.common.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Service
public class DNNTRequestApiServiceImpl extends RequestApiService {

    static DateTimeFormatter FORMATTER =
            DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());



    public static final Logger LOGGER = Logger.getLogger(DNNTRequestApiServiceImpl.class.getName());

    public static final String API_KEY_HEADER = "X-API-KEY";

    public static enum Navrh {
        A {
            @Override
            public List<Navrh> allowingPredecessor() {
                return new ArrayList<>();
            }
        },
        PA {
            @Override
            public List<Navrh> allowingPredecessor() {
                return new ArrayList<>();
            }
        },
        N {
            @Override
            public List<Navrh> allowingPredecessor() {
                return new ArrayList<>();
            }
        },
        VN {
            @Override
            public List<Navrh> allowingPredecessor() {
                return new ArrayList<>();
            }
        },
        VS {
            @Override
            public List<Navrh> allowingPredecessor() {
                return new ArrayList<>();
            }
        },

        NZN{
            @Override
            public List<Navrh> allowingPredecessor() {
                return Arrays.asList(VN, VS, N);
            }
        },
        VVS {
            @Override
            public List<Navrh> allowingPredecessor() {
                return Arrays.asList(A, PA);
            }
        };

        public abstract List<Navrh> allowingPredecessor();
    }

    // account service
    AccountService accountService = new AccountServiceImpl();

    // Catalogue searcher
    CatalogSearcher catalogSearcher = new CatalogSearcher();


    @Override
    public Response requestBatchNzn(BatchRequest batchRequest, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return request(containerRequestContext,  batchRequest, Navrh.NZN);
    }

    @Override
    public Response requestBatchVvs(BatchRequest batchRequest, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return request(containerRequestContext,  batchRequest, Navrh.VVS);
    }


    @Override
    public Response requestGet(String status, String navrh, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String headerString = containerRequestContext.getHeaderString(API_KEY_HEADER);
        if (headerString != null) {
            User user = UserController.findUserByApiKey(headerString);
            if (user != null) {
                ArrayOfSavedRequest arrayOfSavedRequest = new ArrayOfSavedRequest();
                //InlineResponse2001 allRequestsResponse = new InlineResponse2001();
                try {
                    JSONObject search = accountService.search(null, status, navrh, user);
                    JSONObject response = search.getJSONObject("response");
                    JSONArray docs = response.getJSONArray("docs");
                    for (int i = 0, ll = docs.length();i<ll;i++) {
                        JSONObject jsonObject = docs.getJSONObject(i);

                        // use jackson provider
                        ObjectMapper objectMapper = getObjectMapper();

                        SuccessRequestSaved savedRequest = objectMapper.readValue(jsonObject.toString(), SuccessRequestSaved.class);
                        arrayOfSavedRequest.add(savedRequest);
                    }

                    return Response.ok().entity(arrayOfSavedRequest).build();

                } catch (SolrServerException | IOException e) {
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


    private Response request(ContainerRequestContext crc, BatchRequest body, Navrh navrh) {
        try {
            BatchResponse response = new BatchResponse();
            response.setNotsaved(new ArrayOfFailedRequest());
            response.setSaved(new ArrayOfSavedRequest());

            ObjectMapper objectMapper = getObjectMapper();

            String headerString = crc.getHeaderString(API_KEY_HEADER);
            if (headerString != null) {
                User user = UserController.findUserByApiKey(headerString);
                if (user != null) {
                    List<Request> batch = body.getBatch();
                    for (Request req : batch) {

                        try {
                            verifyIdentifiers(navrh, req.getIdentifiers());
                            String s = objectMapper.writeValueAsString(req);

                            JSONObject rawObject = new JSONObject(s);
                            rawObject.put("id", user.username+ UUID.randomUUID().toString());

                            rawObject.put("navrh",navrh.name());
                            rawObject.put("process",new JSONObject());
                            rawObject.put("state","waiting");

                            rawObject.put("datum_zadani" , FORMATTER.format(Instant.now()));

                            try {
                                JSONObject object = accountService.saveRequest(rawObject.toString(), user);
                                response.getSaved().add(objectMapper.readValue(object.toString(), SuccessRequestSaved.class));

                            } catch (SolrServerException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);

                                FailedRequestNotSaved ns = new FailedRequestNotSaved();
                                ns.identifiers(req.getIdentifiers())
                                        //.datumZadani(req.getDatumZadani())
                                        .pozadavek(req.getPozadavek())
                                        .poznamka(req.getPoznamka());

                                response.getNotsaved().add(ns.reason(e.getMessage()));

                            }
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

                            response.getNotsaved().add(ns.reason("Invalid state of these documents: "+e.getPids()));
                        }

                    }
                    return Response.ok().entity(response).build();
                }
            }
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();
        } catch (IOException e) {
            return Response.accepted().status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
    }

    // verify identifiers
    private void verifyIdentifiers(Navrh navrh, List<String> identifiers) throws NonExistentIdentifeirsException, InvalidIdentifiersException {
        List<String> predecessor = navrh.allowingPredecessor().stream().map(Navrh::name).collect(Collectors.toList());

        List<Pair<String, List<String>>>  identFromCatalog = catalogSearcher.existingIdentifiersAndStates(identifiers);

        List<String> nonExistentIdentifiers = new ArrayList<>(identifiers);
        List<String> invalidIdentifiers = new ArrayList<>();

        identFromCatalog.stream().map(Pair::getLeft).forEach(nonExistentIdentifiers::remove);

        identFromCatalog.forEach(pair-> {
            boolean found = false;
            List<String> documentStates = pair.getRight();
            for (String docState : documentStates) {
                if (predecessor.contains(docState)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                invalidIdentifiers.add(pair.getLeft());
            }
        });

        if (!nonExistentIdentifiers.isEmpty()) throw new NonExistentIdentifeirsException(nonExistentIdentifiers);
        if (!invalidIdentifiers.isEmpty()) throw new InvalidIdentifiersException(invalidIdentifiers);

    }

    public static class NonExistentIdentifeirsException extends Exception {
        private List<String> idents;

        public NonExistentIdentifeirsException(List<String> pids) {
            this.idents = pids;
        }

        public List<String> getIdents() {
            return idents;
        }
    }

    public static class InvalidIdentifiersException extends Exception {
        private List<String> pids;

        public InvalidIdentifiersException(List<String> pids) {
            this.pids = pids;
        }

        public List<String> getPids() {
            return pids;
        }
    }
}
