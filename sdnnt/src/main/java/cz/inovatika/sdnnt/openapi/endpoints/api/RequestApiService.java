package cz.inovatika.sdnnt.openapi.endpoints.api;

import cz.inovatika.sdnnt.openapi.endpoints.model.BatchRequest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-09T09:07:54.515Z[GMT]")public abstract class RequestApiService {
    public abstract Response requestBatch(ContainerRequestContext crc, BatchRequest body, SecurityContext securityContext) throws NotFoundException;
    public abstract Response requestGEt(ContainerRequestContext crc, SecurityContext securityContext) throws NotFoundException;
}
