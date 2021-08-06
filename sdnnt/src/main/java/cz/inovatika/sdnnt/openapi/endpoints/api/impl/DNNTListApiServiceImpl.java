package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.openapi.endpoints.api.ListsApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
import cz.inovatika.sdnnt.openapi.endpoints.api.StringUtil;
import org.apache.commons.collections4.iterators.ArrayListIterator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DNNTListApiServiceImpl extends ListsApiService {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

    static Logger LOGGER = Logger.getLogger(DNNTListApiServiceImpl.class.getName());

    // Catalogue searcher
    CatalogSearcher catalogSearcher = new CatalogSearcher();

//    @Override
//    public Response listDnnto(Integer integer, Integer integer1, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
//        //Response.ok().c
//        Map<String,String> map = new HashMap<>();
//        map.put("rows", integer.toString());
//        map.put("size", integer1.toString());
//        JSONObject a = catalogSearcher.getA(map, null);
//        return Response.ok().entity(a.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
//    }
//
//    @Override
//    public Response listDnntt(Integer integer, Integer integer1, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
//        //Response.ok().c
//        Map<String,String> map = new HashMap<>();
//        map.put("rows", integer.toString());
//        map.put("size", integer1.toString());
//        JSONObject a = catalogSearcher.getA(map, null);
//        return Response.ok().entity(a.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
//    }




    @Override
    public Response listDnntoCsv(String instituion, OffsetDateTime dateTime, Boolean uniq, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        List<String> plusList = (instituion != null) ?  new ArrayList<>(Arrays.asList("marc_911a:"+instituion, "marc_990a:A")) :  new ArrayList<>(Arrays.asList("marc_990a:A"));
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }
        return csv(instituion,"dnnto",uniq,plusList, Arrays.asList("marc_990a:NZ"), Arrays.asList("nazev", "identifier", "marc_856u", "marc_990a", "marc_992s", "marc_911u", "marc_910a","marc_911a"));
    }


    @Override
    public Response listDnnttCsv(String instituion, OffsetDateTime dateTime,Boolean uniq, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        List<String> plusList = (instituion != null) ?   new ArrayList<>(Arrays.asList("marc_911a:"+instituion, "marc_990a:A","marc_990a:NZ")):  new ArrayList<>(Arrays.asList("marc_990a:A","marc_990a:NZ"));
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }
        return csv(instituion,"dnntt", uniq,plusList, new ArrayList<>(), Arrays.asList("nazev", "identifier", "marc_856u", "marc_990a", "marc_992s", "marc_911u", "marc_910a","marc_911a"));
    }


    // TODO: Prodisktuovat instituce, marc911a,u, marc956u, marc856u a vazby na digitalni instance krameria
    private Response csv(String selectedInstitution,String label,Boolean onlyUniqPids, List<String> plusList, List<String> minusList, List<String> fields) {
        try {
            Set<String> uniqe = new HashSet<>();

            File csvFile = File.createTempFile("temp","csv");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(csvFile), Charset.forName("UTF-8"));
            try (CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.EXCEL.withHeader("pid","label","institution","name","Aleph SKC identifier"))) {
                Map<String, String> map = new HashMap<>();
                map.put("rows", "1000");


                this.catalogSearcher.iterate(map, null, plusList, minusList,fields, (doc)->{


                    Collection<Object> nazev = doc.getFieldValues("nazev");
                    String identifier = (String) doc.getFieldValue("identifier");


                    Collection<Object> mlinks911u = doc.getFieldValues("marc_911u");
                    Collection<Object> mlinks856u =  doc.getFieldValues("marc_856u");
                    Collection<Object> mlinks956u =  doc.getFieldValues("marc_956u");

                    Collection<Object> minstitutions911a = doc.getFieldValues("marc_911a");
                    Collection<Object> minstitutions910a = doc.getFieldValues("marc_910a");

                    final List<String> links = new ArrayList<>();
                    if (mlinks911u != null && !mlinks911u.isEmpty()) {
                        mlinks911u.stream().map(Object::toString).forEach(links::add);
                    } else if (mlinks856u != null) {
                        mlinks856u.stream().map(Object::toString).forEach(links::add);
                    } else if (mlinks956u != null) {
                        mlinks956u.stream().map(Object::toString).forEach(links::add);
                    }

                    if (!links.isEmpty()) {
                        /**
                         * Pokud je pritomno pole 911a a 911u, pak je mapovani pid - instituce - Pokud ne, pak se neda rict komu patri - zadna instituce
                         */


                        // Vraci vsechny linky do krameriu -> filtruje jine
                        List<String> krameriusLinks = links.stream().map(String::toLowerCase).filter(it -> it.contains("uuid:")).collect(Collectors.toList());
                        // Z linku posbirane pidy pokud obsahuji subsgring uuid
                        List<String> pids = krameriusLinks.stream().map(it -> {
                            int i = it.indexOf("uuid:");
                            return it.substring(i);
                        }).collect(Collectors.toList());


                        if (mlinks911u != null && !mlinks911u.isEmpty() && minstitutions911a !=null && !minstitutions911a.isEmpty()) {

                            List<String> institutions = new ArrayList(minstitutions911a);
                            // indexy do puvodni pole linku
                            List<Integer> indicies = krameriusLinks.stream().map(it-> links.indexOf(it)).collect(Collectors.toList());
                            // Sigly knihoven podle linku
                            List<String> siglas = indicies.stream().map(index -> {
                                return (index >=0 && index < institutions.size()) ? institutions.get(index) : "";
                            }).collect(Collectors.toList());

                            // pokud neni vybrano, pro vsechny, pokud je vybrano, pouze pro tu jednu
                            if (selectedInstitution != null && siglas.contains(selectedInstitution)) {
                                int indexOf = siglas.indexOf(selectedInstitution);
                                if (indexOf > -1 && indexOf< pids.size()) {
                                    String pid = pids.get(indexOf);
                                    if ((onlyUniqPids && !uniqe.contains(pid)) || !onlyUniqPids) {
                                        pidsForInstitution(selectedInstitution, label, printer, nazev, identifier, pid);
                                        uniqe.add(pid);
                                    }
                                } else {
                                    LOGGER.log(Level.WARNING, String.format("Cannot find institution '%s' in record '%s'",selectedInstitution, identifier));
                                }

                            } else if (selectedInstitution == null ) {
                                    // nevybrana instituce, bere vsechny
                                    if (onlyUniqPids) {
                                        for (int i = 0; i < pids.size(); i++) {
                                            String s = i < siglas.size() ? siglas.get(i) : "";
                                            if (!uniqe.contains( pids.get(i))) {
                                                pidsForInstitution( s, label, printer, nazev, identifier, pids.get(i));
                                                uniqe.add(pids.get(i));
                                            }
                                        }
                                    } else {
                                        for (int i = 0; i < pids.size(); i++) {
                                            String s = i < siglas.size() ? siglas.get(i) : "";
                                            pidsForInstitution( s, label, printer, nazev, identifier, pids.get(i));
                                        }
                                    }
                            }
                        } else {
                            // pid nepatri zadne instituci, uvedou se jenom pidy, je pritomno v reportu bez ohledu na vybranou knihovnu
                            if (onlyUniqPids) {
                                for (int i = 0; i < pids.size(); i++) {
                                    if (!uniqe.contains( pids.get(i))) {
                                        pidsForInstitution("", label, printer, nazev, identifier, pids.get(i));
                                        uniqe.add(pids.get(i));
                                    }
                                }
                            } else {
                                pidsForInstitution("", label, printer, nazev, identifier, pids.toArray(new String[pids.size()]));
                            }
                        }
                    }

                });
            }


            ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                    .fileName(String.format("%s-%s.csv", label, SIMPLE_DATE_FORMAT.format(new Date()))).creationDate(new Date()).build();

            return Response.ok(
                    new StreamingOutput() {
                        @Override
                        public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                            IOUtils.copy(new FileInputStream(csvFile), outputStream);
                        }
                    }).header("Content-Disposition",contentDisposition).type("text/csv").encoding("UTF-8").build();

        } catch (IOException e) {
            // todo
            throw new RuntimeException(e);
        }
    }

    private void pidsForInstitution(String selectedInstitution, String label, CSVPrinter printer, Collection<Object> nazev, String identifier, String... pids) {
        for (int i = 0; i < pids.length; i++) {
            try {
                printer.printRecord(pids[i], label, selectedInstitution ,nazev, identifier);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
        }
    }

}
