package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.openapi.endpoints.api.ListsApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
import org.json.JSONObject;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.Map;

public class DNNTListApiServiceImpl extends ListsApiService {

//    Map<String,String> map = new HashMap<>();
//        map.put("q", s);
//
//    User user = null;
//    String headerString = crc.getHeaderString(DNNTRequestApiServiceImpl.API_KEY_HEADER);
//        if (headerString != null) {
//        user = UserController.findUserByApiKey(headerString);
//    }
//
//    JSONObject search = catalogSearcher.search(map, user);
//        return Response.ok().entity(search.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();

    // Catalogue searcher
    CatalogSearcher catalogSearcher = new CatalogSearcher();

    @Override
    public Response listDnnto(Integer integer, Integer integer1, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        //Response.ok().c
        Map<String,String> map = new HashMap<>();
        map.put("rows", integer.toString());
        map.put("size", integer1.toString());
        JSONObject a = catalogSearcher.getA(map, null);
        return Response.ok().entity(a.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response listDnntt(Integer integer, Integer integer1, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        //Response.ok().c
        Map<String,String> map = new HashMap<>();
        map.put("rows", integer.toString());
        map.put("size", integer1.toString());
        JSONObject a = catalogSearcher.getA(map, null);
        return Response.ok().entity(a.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response listDnntoCsv(SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return null;
    }

    @Override
    public Response listDnnttCsv(SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return null;
    }
}
