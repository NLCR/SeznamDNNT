package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.indexer.models.Import;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.openapi.endpoints.api.*;

import cz.inovatika.sdnnt.openapi.endpoints.model.*;

import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-09T09:07:54.515Z[GMT]")public class RequestApiServiceImpl extends RequestApiService {

    public static final Logger LOGGER = Logger.getLogger(RequestApiServiceImpl.class.getName());

    public static final String API_KEY_HEADER = "X-API-KEY";


    AccountService accountService = new AccountServiceImpl();
    @Override
    public Response requestBatch(ContainerRequestContext crc, BatchRequest body, SecurityContext securityContext) throws NotFoundException {
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

                        String s = objectMapper.writeValueAsString(req);

                        JSONObject rawObject = new JSONObject(s);
                        rawObject.put("id", user.username+UUID.randomUUID().toString());

                        rawObject.put("navrh","NZN");
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

                    }
                    return Response.ok().entity(response).build();
                    //accountService.saveRequest()
                }
            }
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();
        } catch (IOException e) {
            return Response.accepted().status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
        }
    }
    @Override
    public Response requestGEt(ContainerRequestContext crc, SecurityContext securityContext) throws NotFoundException {
        String headerString = crc.getHeaderString(API_KEY_HEADER);
        if (headerString != null) {
            User user = UserController.findUserByApiKey(headerString);
            if (user != null) {
                AllRequestsResponse allRequestsResponse = new AllRequestsResponse();

                try {
                    JSONObject search = accountService.search(null, null, null, user);

                    JSONObject response = search.getJSONObject("response");
                    JSONArray docs = response.getJSONArray("docs");
                    for (int i = 0, ll = docs.length();i<ll;i++) {
                        JSONObject jsonObject = docs.getJSONObject(i);
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                        SavedRequest savedRequest = objectMapper.readValue(jsonObject.toString(), SavedRequest.class);
                        allRequestsResponse.addSavedItem(savedRequest);
                    }

                    return Response.ok().entity(allRequestsResponse).build();

                } catch (SolrServerException | IOException e) {
                    return Response.accepted().status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, e.getMessage())).build();
                }
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();
        }
    }
}
