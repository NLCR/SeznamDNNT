package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.openapi.endpoints.api.CatalogApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
import cz.inovatika.sdnnt.openapi.endpoints.api.StringUtil;
import org.json.JSONObject;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.Map;

public class DNNTCatalogApiServiceImpl extends CatalogApiService {

    // Catalogue searcher
    CatalogSearcher catalogSearcher = new CatalogSearcher();

    @Override
    public Response catalogGet(String s, Integer integer, Integer integer1, String s1, SecurityContext securityContext, ContainerRequestContext crc) throws NotFoundException {
        Map<String,String> map = new HashMap<>();
        map.put("q", s);

        User user = null;
        String headerString = crc.getHeaderString(DNNTRequestApiServiceImpl.API_KEY_HEADER);
        if (headerString != null) {
            user = UserController.findUserByApiKey(headerString);
        }

        JSONObject search = catalogSearcher.search(map, user);
        return Response.ok().entity(search.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

}
