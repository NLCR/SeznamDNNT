package cz.inovatika.sdnnt.openapi.endpoints.api;

import cz.inovatika.sdnnt.openapi.endpoints.api.factories.CatalogApiServiceFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import cz.inovatika.sdnnt.openapi.endpoints.model.InlineResponse200;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.validation.constraints.*;


@Path("/catalog")


@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-15T08:56:28.035Z[GMT]")public class CatalogApi  {
   private final CatalogApiService delegate;

   public CatalogApi(@Context ServletConfig servletContext) {
      CatalogApiService delegate = null;

      if (servletContext != null) {
         String implClass = servletContext.getInitParameter("CatalogApi.implementation");
         if (implClass != null && !"".equals(implClass.trim())) {
            try {
               delegate = (CatalogApiService) Class.forName(implClass).newInstance();
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         } 
      }

      if (delegate == null) {
         delegate = CatalogApiServiceFactory.getCatalogApi();
      }

      this.delegate = delegate;
   }

    @GET
    
    
    @Produces({ "application/json" })
    @Operation(summary = "Základní funkcionalita pro nahlížení do katalogu", description = "Poskytuje základní funkcionalitu pro nahlížení do katalogu", security = {
        @SecurityRequirement(name = "api_key")    }, tags={ "Katalog" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Záznamy z katalogu", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InlineResponse200.class))) })
    public Response catalogGet(@Parameter(in = ParameterIn.QUERY, description = "",required=true) @QueryParam("query") String query
,@Parameter(in = ParameterIn.QUERY, description = "") @DefaultValue("20") @QueryParam("rows") Integer rows
,@Parameter(in = ParameterIn.QUERY, description = "") @DefaultValue("0") @QueryParam("page") Integer page
,@Parameter(in = ParameterIn.QUERY, description = "") @QueryParam("sort") String sort
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.catalogGet(query,rows,page,sort,securityContext);
    }
}
