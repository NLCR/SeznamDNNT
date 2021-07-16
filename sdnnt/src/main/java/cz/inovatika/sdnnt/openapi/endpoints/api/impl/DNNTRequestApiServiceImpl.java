package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

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
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
public class DNNTRequestApiServiceImpl extends RequestApiService {



    public static final Logger LOGGER = Logger.getLogger(DNNTRequestApiServiceImpl.class.getName());

    public static final String API_KEY_HEADER = "X-API-KEY";

    public static enum Navrh {
        NZN, VVS
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
                BatchResponse allRequestsResponse = new BatchResponse();
                try {
                    JSONObject search = accountService.search(null, status, navrh, user);
                    JSONObject response = search.getJSONObject("response");
                    JSONArray docs = response.getJSONArray("docs");
                    for (int i = 0, ll = docs.length();i<ll;i++) {
                        JSONObject jsonObject = docs.getJSONObject(i);

                        // somehome user jackson provider
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                        objectMapper.registerModule(new JavaTimeModule());
                        objectMapper.setDateFormat(new RFC3339DateFormat());

                        SavedRequest savedRequest = objectMapper.readValue(jsonObject.toString(), SavedRequest.class);
                        allRequestsResponse.addSavedItem(savedRequest);
                    }
                    return Response.ok().entity(allRequestsResponse).build();

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



    private Response request(ContainerRequestContext crc, BatchRequest body, Navrh navrh) {
        try {
            BatchResponse response = new BatchResponse();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String headerString = crc.getHeaderString(API_KEY_HEADER);
            if (headerString != null) {
                User user = UserController.findUserByApiKey(headerString);
                if (user != null) {
                    List<Request> batch = body.getBatch();
                    for (Request req : batch) {

                        if (verifyIndetifiers(req.getIdentifiers())) {
                            String s = objectMapper.writeValueAsString(req);

                            JSONObject rawObject = new JSONObject(s);
                            rawObject.put("id", user.username+ UUID.randomUUID().toString());

                            rawObject.put("navrh",navrh.name());
                            rawObject.put("process",new JSONObject());
                            rawObject.put("state","waiting");

                            try {
                                JSONObject object = accountService.saveRequest(rawObject.toString(), user);
                                response.addSavedItem(objectMapper.readValue(object.toString(), SavedRequest.class));

                            } catch (SolrServerException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);

                                NotSavedRequest ns = new NotSavedRequest();
                                ns.identifiers(req.getIdentifiers())
                                        .datumZadani(req.getDatumZadani())
                                        .pozadavek(req.getPozadavek())
                                        .poznamka(req.getPoznamka());

                                response.addNotsavedItem(ns.reason(e.getMessage()));
                            }
                        } else {
                            NotSavedRequest ns = new NotSavedRequest();
                            ns.identifiers(req.getIdentifiers())
                                    .datumZadani(req.getDatumZadani())
                                    .pozadavek(req.getPozadavek())
                                    .poznamka(req.getPoznamka());

                            response.addNotsavedItem(ns.reason("Cannot  find any of these identifiers "+req.getIdentifiers()));
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
    private boolean verifyIndetifiers(List<String> identifiers) {
        for (String ident :  identifiers) {
            // if (cannotfind) return false;
            //catalogSearcher.search()
        }
        return true;
    }

}
