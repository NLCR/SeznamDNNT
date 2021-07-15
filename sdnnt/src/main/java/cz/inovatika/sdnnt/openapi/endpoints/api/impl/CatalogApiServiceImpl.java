package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.openapi.endpoints.api.CatalogApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class CatalogApiServiceImpl extends CatalogApiService {

    @Override
    public Response catalogGet(String query, Integer rows, Integer page, String sort, SecurityContext securityContext) throws NotFoundException {
        return null;
    }
}
