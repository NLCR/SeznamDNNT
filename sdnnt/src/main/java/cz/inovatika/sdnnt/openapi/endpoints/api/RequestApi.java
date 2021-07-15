package cz.inovatika.sdnnt.openapi.endpoints.api;

import cz.inovatika.sdnnt.openapi.endpoints.api.factories.RequestApiServiceFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import cz.inovatika.sdnnt.openapi.endpoints.model.BatchRequest;
import cz.inovatika.sdnnt.openapi.endpoints.model.BatchResponse;
import cz.inovatika.sdnnt.openapi.endpoints.model.InlineResponse2001;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.ServletConfig;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.validation.constraints.*;


@Path("/request")


@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen", date = "2021-07-15T08:56:28.035Z[GMT]")public class RequestApi  {
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
    @Path("/batch/nzn")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Umožňuje podávat hromadnou žádost", description = "Řeší hromadnou žádost", security = {
        @SecurityRequirement(name = "api_key")    }, tags={ "Požadavky" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Žádost vytvořena a ve stavu waiting", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BatchResponse.class))),
        
        @ApiResponse(responseCode = "400", description = "Špatný dotaz (syntaxe atd..)"),
        
        @ApiResponse(responseCode = "404", description = "Identifikátory nenalezen(y)"),
        
        @ApiResponse(responseCode = "405", description = "Chyba validace (datumy atd..)") })
    public Response requestBatchNzn(@Context ContainerRequestContext crc, @Parameter(in = ParameterIn.DEFAULT, description = "Vytvoření hromadné žádosti" ,required=true) BatchRequest body

, @Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.requestBatchNzn(crc,body,securityContext);
    }
    @PUT
    @Path("/batch/vvs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @Operation(summary = "Umožňuje podávat hromadnou žádost", description = "Řeší hromadnou žádost", security = {
        @SecurityRequirement(name = "api_key")    }, tags={ "Požadavky" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Žádost vytvořena a ve stavu waiting", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BatchResponse.class))),
        
        @ApiResponse(responseCode = "400", description = "Špatný dotaz (syntaxe atd..)"),
        
        @ApiResponse(responseCode = "404", description = "Identifikátory nenalezen(y)"),
        
        @ApiResponse(responseCode = "405", description = "Chyba validace (datumy atd..)") })
    public Response requestBatchVvs(@Context ContainerRequestContext crc,@Parameter(in = ParameterIn.DEFAULT, description = "Vytvoření hromadné žádosti" ,required=true) BatchRequest body

,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.requestBatchVvs(crc, body,securityContext);
    }
    @GET
    
    
    @Produces({ "application/json" })
    @Operation(summary = "Vrací všechny žádosti pro uživatele", description = "Vrací všechny žádosti, případně filtrované dle stavu", security = {
        @SecurityRequirement(name = "api_key")    }, tags={ "Požadavky" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Všechny uložené žádosti", content = @Content(mediaType = "application/json", schema = @Schema(implementation = InlineResponse2001.class))) })
    public Response requestGet(@Context ContainerRequestContext crc, @Parameter(in = ParameterIn.QUERY, description = "", schema=@Schema(allowableValues={ "open", "waiting", "processed", "rejected", "approved" })
) @QueryParam("status") String status
,@Parameter(in = ParameterIn.QUERY, description = "", schema=@Schema(allowableValues={ "NZN", "VVS" })
) @QueryParam("navrh") String navrh
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.requestGet(crc, status,navrh,securityContext);
    }
}
