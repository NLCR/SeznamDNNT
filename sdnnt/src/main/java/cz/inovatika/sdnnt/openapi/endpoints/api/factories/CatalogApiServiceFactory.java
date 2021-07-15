package cz.inovatika.sdnnt.openapi.endpoints.api.factories;

import cz.inovatika.sdnnt.openapi.endpoints.api.CatalogApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.CatalogApiServiceImpl;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-15T08:56:28.035Z[GMT]")public class CatalogApiServiceFactory {

    private final static CatalogApiService service = new CatalogApiServiceImpl();

    public static CatalogApiService getCatalogApi() {
        return service;
    }
}
