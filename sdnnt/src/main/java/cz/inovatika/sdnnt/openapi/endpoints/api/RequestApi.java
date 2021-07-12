package cz.inovatika.sdnnt.openapi.endpoints.api;

import cz.inovatika.sdnnt.openapi.endpoints.api.factories.RequestApiServiceFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import cz.inovatika.sdnnt.openapi.endpoints.model.AllRequestsResponse;
import cz.inovatika.sdnnt.openapi.endpoints.model.BatchRequest;
import cz.inovatika.sdnnt.openapi.endpoints.model.BatchResponse;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;


@Path("/request")
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-09T09:07:54.515Z[GMT]")public class RequestApi  {
   private final RequestApiService delegate;

   public RequestApi(@Context ServletConfig servletContext) {
      RequestApiService delegate = null;

      if (servletContext != null) {
         String implClass = servletContext.getInitParameter("RequestApi.implementation");
         if (implClass != null && !"".equals(implClass.trim())) {
            try {
               delegate = (RequestApiService) Class.forName(implClass).newInstance();
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         } 
      }

      if (delegate == null) {
         delegate = RequestApiServiceFactory.getRequestApi();
      }

      this.delegate = delegate;
   }

    @PUT
    @Path("/batch")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    public Response requestBatch(@Context ContainerRequestContext crc, @Parameter(in = ParameterIn.DEFAULT, description = "Vytvoreni zadosti" ,required=true) BatchRequest body, @Context SecurityContext securityContext)
    throws NotFoundException {

        return delegate.requestBatch(crc, body,securityContext);
    }

    @GET
    @Path("/get")
    @Produces({ "application/json" })
    public Response requestGEt(@Context ContainerRequestContext crc,  @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.requestGEt(crc,securityContext);
    }
}
