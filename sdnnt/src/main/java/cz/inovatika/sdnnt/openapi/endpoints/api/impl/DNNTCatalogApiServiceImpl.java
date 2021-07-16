package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.openapi.endpoints.api.CatalogApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class DNNTCatalogApiServiceImpl extends CatalogApiService {

    @Override
    public Response catalogGet(String s, Integer integer, Integer integer1, String s1, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        return null;
    }

}
