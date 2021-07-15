package cz.inovatika.sdnnt.openapi.endpoints.api;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-15T08:56:28.035Z[GMT]")public abstract class CatalogApiService {
    public abstract Response catalogGet( @NotNull String query, Integer rows, Integer page, String sort,SecurityContext securityContext) throws NotFoundException;
}
