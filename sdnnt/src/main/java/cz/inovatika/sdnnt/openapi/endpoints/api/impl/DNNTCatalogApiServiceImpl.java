package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.openapi.endpoints.api.*;
import cz.inovatika.sdnnt.openapi.endpoints.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DNNTCatalogApiServiceImpl extends CatalogApiService {

    // Catalogue searcher
    CatalogSearcher catalogSearcher = new CatalogSearcher();


    @Override
    public Response catalogGet(String query, String state, String license,Boolean fullCatalog, Integer integer, Integer integer1,  SecurityContext securityContext, ContainerRequestContext crc) throws NotFoundException {
        Map<String,String> map = new HashMap<>();
        map.put("q", query);
        map.put("rows", integer.toString());
        map.put("page", integer1.toString());
        if (fullCatalog) {
            map.put("fullCatalog", Boolean.TRUE.toString());
        }

        List<String> filters = new ArrayList<>();
        if (state != null && state.length()> 0) {
            filters.add("dntstav:"+state);
        }
        if (license !=null  && license.length() > 0) {
            filters.add("license:"+license.toLowerCase());
        }


        User user = null;
        String headerString = crc.getHeaderString(DNNTRequestApiServiceImpl.API_KEY_HEADER);
        if (headerString != null) {
            user = UserController.findUserByApiKey(headerString);
        }

        if (user != null) {
            JSONObject search = catalogSearcher.search(map, filters, user);

            if(search.has("zadosti")) {
                search.getJSONArray("zadosti");
            }

            List<AssociatedRequest> allRequests = new ArrayList<>();
            if (search.has("zadosti")) {
                JSONArray zadosti = search.getJSONArray("zadosti");
                for (int j = 0; j < zadosti.length(); j++) {

                    JSONObject zadost = zadosti.getJSONObject(j);
                    String zadostState = zadost.optString("state");
                    if (zadostState != null && zadostState.equals("waiting")) {
                        AssociatedRequest associatedRequest = new AssociatedRequest();


                        List<String> identifiers = new ArrayList<>();
                        zadost.getJSONArray("identifiers").forEach(id-> { identifiers.add(id.toString());} );
                        associatedRequest.setIdentifiers(identifiers);

                        associatedRequest.setUser(zadost.getString("user"));

                        if (zadost.has("datum_zadani")) {
                            TemporalAccessor datumZadaniTA = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).parse(zadost.getString("datum_zadani"));

                            OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(Instant.from(datumZadaniTA), ZoneId.systemDefault());
                            associatedRequest.setDatumZadani(offsetDateTime);
                        }
                        if (zadost.has("indextime")) {

                            TemporalAccessor indexTimeTA = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).parse(zadost.getString("indextime"));
                            OffsetDateTime offsetIndexTime = OffsetDateTime.ofInstant(Instant.from(indexTimeTA), ZoneId.systemDefault());
                            associatedRequest.setIndextime(offsetIndexTime);
                        }

                        associatedRequest.setPozadavek(zadost.optString("pozadavek"));
                        associatedRequest.setPoznamka(zadost.optString("poznamka"));
                        associatedRequest.setNavrh(zadost.optString("navrh"));
                        allRequests.add(associatedRequest);
                    }
                }

            }


            CatalogResponse response = new CatalogResponse();
            response.setNumFound(search.getJSONObject("response").getInt("numFound"));

            ArrayOfCatalogItem itemsArray = new ArrayOfCatalogItem();
            JSONArray jsonArray = search.getJSONObject("response").getJSONArray("docs");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject doc = jsonArray.getJSONObject(i);
                CatalogItem item = createcatalogItemFromJSON(doc);

                String frbr = null;
                if (doc.has("frbr")) {
                    frbr = doc.getString("frbr");
                    item.frbr(frbr);
                    JSONObject frbrObjects = catalogSearcher.frbr(frbr);
                    if (frbrObjects.has("response")) {
                        if (frbrObjects.getJSONObject("response").has("docs")) {
                            ArrayOfCatalogItemBase items = new ArrayOfCatalogItemBase();
                            frbrObjects.getJSONObject("response").getJSONArray("docs").forEach((obj) -> {
                                CatalogItem referencedItem = createcatalogItemFromJSON((JSONObject) obj);
                                if (!referencedItem.getIdentifier().equals(item.getIdentifier())) {
                                    items.add(createcatalogItemFromJSON((JSONObject)obj));
                                }
                            });
                            item.associatedItems(items);
                        }
                    }
                }

                ArrayOfAssociatedRequests associatedWithItem = new ArrayOfAssociatedRequests();
                allRequests.stream().filter(associatedRequest -> {
                    return associatedRequest.getIdentifiers().contains(item.getIdentifier());
                }).forEach(associatedWithItem::add);

                item.associatedRequests(associatedWithItem);
                itemsArray.add(item);

            }
            response.docs(itemsArray);
            return Response.ok().entity(response).build();

        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Not authorized")).build();

        }
    }

    private CatalogItem createcatalogItemFromJSON(JSONObject doc) {
        String ident = doc.getString("identifier");
        String catalog = doc.getJSONArray("marc_998a").optString(0);
        List<String> licenses = new ArrayList<>();
        List<String> stavy  = new ArrayList<>();
        List<String> nazev  = new ArrayList<>();
        List<String> autor  = new ArrayList<>();
        List<String> nakladatel  = new ArrayList<>();
        List<String> historieStavu  = new ArrayList<>();
        List<String> institutions = new ArrayList<>();
        List<String> links = new ArrayList<>();
        List<String> pids = new ArrayList<>();


        doc.getJSONArray("nazev").forEach(o-> nazev.add(o.toString()));
        if (doc.has("author")) {
            doc.getJSONArray("author").forEach(o-> autor.add(o.toString()));
        }
        if (doc.has("dntstav")) {
            doc.getJSONArray("dntstav").forEach(o-> stavy.add(o.toString()));
        }
        if (doc.has("nakladatel")) {
            doc.getJSONArray("nakladatel").forEach(o-> nakladatel.add(o.toString()));
        }
        if (doc.has("historie_stavu")) {
            new JSONArray(doc.getString("historie_stavu")).forEach(o-> {
                historieStavu.add(((JSONObject)o).getString("stav"));
            });
        }

        if (doc.has("marc_910a")) {
            doc.getJSONArray("marc_910a").forEach(o-> institutions.add(o.toString()));
        }

        if (doc.has("marc_911u")) {
            doc.getJSONArray("marc_911u").forEach(o-> links.add(o.toString()));
        }

        if (links.isEmpty() && doc.has("marc_956u")) {
            doc.getJSONArray("marc_956u").forEach(o-> links.add(o.toString()));

        }

        if (doc.has("license")) {
             doc.getJSONArray("license").forEach(o -> licenses.add(o.toString()));
        }


        links.stream().filter(str-> str.contains("uuid:")).map(str-> {
            int indexOf = str.indexOf("uuid:");
            if (indexOf > -1) {
                return str.substring(indexOf);
            } else return str;
        }).forEach(pids::add);

        CatalogItem item = new CatalogItem();
        item.identifier(ident)
                .catalog(catalog)
                .title(nazev)
                .states(stavy)
                .author(autor)
                .stateshistory(historieStavu)
                .publisher(nakladatel)
                .institutions(institutions)
                .links(links)
                .pids(pids)
                .license(licenses);
        return item;
    }


}
