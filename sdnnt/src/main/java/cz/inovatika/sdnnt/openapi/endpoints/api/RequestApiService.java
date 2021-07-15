package cz.inovatika.sdnnt.openapi.endpoints.api;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import cz.inovatika.sdnnt.openapi.endpoints.model.BatchRequest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-15T08:56:28.035Z[GMT]")public abstract class RequestApiService {
    public abstract Response requestBatchNzn(ContainerRequestContext crc, BatchRequest body, SecurityContext securityContext) throws NotFoundException;
    public abstract Response requestBatchVvs(ContainerRequestContext crc,BatchRequest body,SecurityContext securityContext) throws NotFoundException;
    public abstract Response requestGet( ContainerRequestContext crc, String status, String navrh,SecurityContext securityContext) throws NotFoundException;
}
