package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.openapi.endpoints.api.*;
import cz.inovatika.sdnnt.openapi.endpoints.model.*;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.UserControlerImpl;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.model.License;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

/**
 *  Catalog search service
 */
public class DNNTCatalogApiServiceImpl extends CatalogApiService {

    public static final Logger LOGGER = Logger.getLogger(DNNTCatalogApiServiceImpl.class.getName());

    public static final String SE_FMT_VALUE = "SE";


    CatalogSearcher catalogSearcher = new CatalogSearcher();



    @Override
    public Response catalogGet(String query, String state, String license, String fmt,  Integer integer, Integer integer1,  SecurityContext securityContext, ContainerRequestContext crc) throws NotFoundException {
        Map<String,String> map = new HashMap<>();
        map.put("q", query);
        map.put("rows", integer.toString());
        map.put("page", integer1.toString());

        List<String> filters = new ArrayList<>();
        if (state != null && state.length()> 0) {
            filters.add(DNTSTAV_FIELD+":"+state);
        }
        if (license !=null  && license.length() > 0) {
            filters.add(LICENSE_FIELD+":"+license.toLowerCase());
        }
        if (fmt != null && fmt.length() > 0) {
            filters.add("fmt:"+fmt.toUpperCase());
        }

        User user = null;
        String headerString = crc.getHeaderString(DNNTRequestApiServiceImpl.API_KEY_HEADER);
        if (headerString != null) {
            try {
                user = new UserControlerImpl(null).findUserByApiKey(headerString);
            } catch (UserControlerException e) {
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
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

                        //associatedRequest.setUser(zadost.getString("user"));

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

                if (!associatedWithItem.isEmpty()) {
                    item.associatedRequests(associatedWithItem);
                }
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

        String fmt = doc.optString("fmt", "");

        List<String> licenses = new ArrayList<>();
        List<String> stavy  = new ArrayList<>();
        List<String> nazev  = new ArrayList<>();
        List<String> autor  = new ArrayList<>();
        List<String> nakladatel  = new ArrayList<>();
        List<String> historieStavu  = new ArrayList<>();
        List<String> sigla = new ArrayList<>();
        List<String> links = new ArrayList<>();
        List<String> pids = new ArrayList<>();
        List<CatalogItemBaseGranularity> granularities = new ArrayList<>();


        doc.getJSONArray(NAZEV_FIELD).forEach(o-> nazev.add(o.toString()));
        // author fields
        if (doc.has(AUTHOR_FIELD)) {
            doc.getJSONArray("author").forEach(o -> autor.add(o.toString()));
        }
        if (doc.has(MARC_700_a)) {
            doc.getJSONArray(MARC_700_a).forEach(o-> autor.add(o.toString()));
        }
        // dntstav
        if (doc.has(MarcRecordFields.DNTSTAV_FIELD)) {
            doc.getJSONArray(MarcRecordFields.DNTSTAV_FIELD).forEach(o-> stavy.add(o.toString()));
        }
        // publishers
        if (doc.has(NAKLADATEL_FIELD)) {
            doc.getJSONArray(NAKLADATEL_FIELD).forEach(o-> nakladatel.add(o.toString()));
        }

        // neni potreba - soucasti nakladatele
//        if (doc.has(MARC_264_B)) {
//            doc.getJSONArray(MARC_264_B).forEach(o-> nakladatel.add(o.toString()));
//        }

        if (doc.has(HISTORIE_STAVU_FIELD)) {
            doc.getJSONArray(HISTORIE_STAVU_FIELD).forEach(o-> {
                JSONObject object = (JSONObject)o;
                if (object.has(DNTSTAV_FIELD)) {
                    historieStavu.add(object.getString(DNTSTAV_FIELD));
                } else if (object.has("stav")) {
                    historieStavu.add(object.getString("stav"));
                }
            });

        }

        // marc 910a a marc 040a
        if (doc.has(MARC_910_A)) {
            doc.getJSONArray(MARC_910_A).forEach(o-> sigla.add(o.toString()));
        } else if (doc.has(MARC_040_A)) {
            doc.getJSONArray(MARC_040_A).forEach(o-> sigla.add(o.toString()));
        }

        if (doc.has(MARC_911_U)) {
            doc.getJSONArray(MARC_911_U).forEach(o-> links.add(o.toString()));
        }

        if (links.isEmpty() && doc.has(MARC_956_U)) {
            String itemFmt = doc.getString(FMT_FIELD);
            // serialy mohou mit granularitu, pokud ano, marc_956u ma vice linku - granularitu davat do pole granularity
            if (itemFmt != null && itemFmt.equals(SE_FMT_VALUE)) {
                links.add(doc.getJSONArray(MARC_956_U).getString(0));
            } else {
                doc.getJSONArray(MARC_956_U).forEach(l-> {
                    links.add(l.toString());
                });
            }
        }

        if (doc.has(LICENSE_FIELD)) {
             doc.getJSONArray(LICENSE_FIELD).forEach(o -> licenses.add(o.toString()));
        }

        if (doc.has(GRANULARITY_FIELD) && doc.has(FMT_FIELD) && doc.getString(FMT_FIELD).equals(SE_FMT_VALUE)){
            JSONArray granularityJSON = doc.getJSONArray(GRANULARITY_FIELD);
            for (int i=0,ll=granularityJSON.length();i<ll;i++) {
                // skip first
                if (i> 0) {
                    CatalogItemBaseGranularity granularity = new CatalogItemBaseGranularity();
                    JSONObject itemObject = (JSONObject) granularityJSON.getJSONObject(i);
                    List<String> states = new ArrayList<>();
                    itemObject.optJSONArray("stav").forEach(stav-> {  states.add(stav.toString()); });
                    granularity.states(states);
                    granularity.link(itemObject.optString("link",""));
                    if (itemObject.has("cislo")) {
                        granularity.number(itemObject.getString("cislo"));
                    }
                    if (itemObject.has("rocnik")) {
                        //granularity.year(itemObject.getString("rocnik"));
                    }
                    if (states.contains("A") && states.contains("NZ")) {
                        granularity.license(License.dnntt.name());
                    } else if (states.contains("A")) {
                        granularity.license(License.dnnto.name());
                    }
                    granularities.add(granularity);

                }

            }
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
                .stateshistory(historieStavu)
                //.pids(pids)
                .fmt(fmt);


        if (!nakladatel.isEmpty()) {
            item.publisher(nakladatel);
        }

        if (!links.isEmpty()) {
            item.links(links);
        }

        if (!pids.isEmpty()) {
            item.pids(pids);
        }

        if (!sigla.isEmpty()) {
            item.sigla(sigla);
        }

        if (!licenses.isEmpty()) {
            item.license(licenses);
        }

        if (!autor.isEmpty()) {
            item.author(autor);
        }


        if (!granularities.isEmpty()) {
            item.granularity(granularities);
        }

        // identifiers
        if (doc.has(MARC_015_A) || doc.has(MARC_020_A) || doc.has(MARC_022_A) || doc.has(MARC_902_A)) {
            JSONArray ccnb = doc.optJSONArray(MARC_015_A);
            JSONArray isbn = doc.optJSONArray(MARC_020_A);
            JSONArray isbn2 = doc.optJSONArray(MARC_902_A);
            JSONArray issn = doc.optJSONArray(MARC_022_A);
            CatalogItemBaseOtherIdentifiers otherIdentifiers = new CatalogItemBaseOtherIdentifiers();
            if (ccnb != null && ccnb.length() > 0) {
                ccnb.forEach(oneCcnb -> {
                    otherIdentifiers.addCcnbItem(oneCcnb.toString());
                });
            }

            if (isbn != null && isbn.length() > 0)   {
                isbn.forEach(oneIsbn -> {
                    otherIdentifiers.addIsbnItem(oneIsbn.toString());
                });
            }
            if (isbn2 != null && isbn2.length() > 0) {
                isbn2.forEach(oneIsbn -> {
                    otherIdentifiers.addIsbnItem(oneIsbn.toString());
                });

            }
            if (issn != null && issn.length() > 0)  {
                issn.forEach(oneIssn -> {
                    otherIdentifiers.addIssnItem(oneIssn.toString());
                });
            }
            item.otherIdentifiers(otherIdentifiers);
        }
        return item;
    }


}
