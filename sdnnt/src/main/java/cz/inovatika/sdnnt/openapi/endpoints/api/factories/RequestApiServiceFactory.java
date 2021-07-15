package cz.inovatika.sdnnt.openapi.endpoints.api.factories;

import cz.inovatika.sdnnt.openapi.endpoints.api.RequestApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.RequestApiServiceImpl;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-15T08:56:28.035Z[GMT]")public class RequestApiServiceFactory {
    private final static RequestApiService service = new RequestApiServiceImpl();

    public static RequestApiService getRequestApi() {
        return service;
    }
}
