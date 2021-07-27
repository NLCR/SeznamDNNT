package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.openapi.endpoints.api.ListsApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
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
import java.util.*;
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
    public Response listDnntoCsv(String instituion, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        List<String> plusList = (instituion != null && instituion.length() >3) ?  Arrays.asList("marc_911a:"+instituion, "marc_990a:A") :Arrays.asList("marc_990a:A");
        return csv(instituion,"DNNTO",plusList, Arrays.asList("marc_990a:NZ"), Arrays.asList("nazev", "identifier", "marc_856u", "marc_990a", "marc_992s", "marc_911u", "marc_910a","marc_911a"));
    }
    @Override
    public Response listDnnttCsv(String instituion, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        List<String> plusList = (instituion != null && instituion.length() >3) ?  Arrays.asList("marc_911a:"+instituion, "marc_990a:A","marc_990a:NZ"): Arrays.asList("marc_990a:A","marc_990a:NZ");
        return csv(instituion,"DNNTT",plusList, new ArrayList<>(), Arrays.asList("nazev", "identifier", "marc_856u", "marc_990a", "marc_992s", "marc_911u", "marc_910a","marc_911a"));
    }


    private Response csv(String selectedInstitution, String label, List<String> plusList, List<String> minusList, List<String> fields) {
        try {
            File csvFile = File.createTempFile("temp","csv");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(csvFile), Charset.forName("UTF-8"));
            try (CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.DEFAULT.withHeader("pid","label","institution","name","Aleph SKC identifier"))) {
                Map<String, String> map = new HashMap<>();
                map.put("rows", "1000");
                this.catalogSearcher.iterate(map, null, plusList, minusList,fields, (doc)->{
                    // kody
                    Collection<Object> nazev = doc.getFieldValues("nazev");
                    String identifier = (String) doc.getFieldValue("identifier");

                    Collection<Object> mlinks911u = doc.getFieldValues("marc_911u");
                    Collection<Object> mlinks856u = null; //doc.getFieldValues("marc_856u");

                    // vsechny instituce, ktre to maji zdigitalizovane
                    //Collection<Object> minstitutions910a = doc.getFieldValues("marc_910a");
                    Collection<Object> minstitutions911a = doc.getFieldValues("marc_911a");

                    final List<String> links = new ArrayList<>();
                    if (mlinks911u != null && !mlinks911u.isEmpty()) {
                        mlinks911u.stream().map(Object::toString).forEach(links::add);
                    } else if (mlinks856u != null) {
                        mlinks856u.stream().map(Object::toString).forEach(links::add);
                    }

                    final List<String> institutions = new ArrayList<>();
                    if (minstitutions911a != null) { minstitutions911a.stream().map(Object::toString).forEach(institutions::add); }


                    if (!links.isEmpty()) {
                        // ma link do digitalni knihovny
                        List<String> krameriusLinks = links.stream().map(String::toLowerCase).filter(it -> it.contains("uuid:")).collect(Collectors.toList());
                        List<Integer> indicies = krameriusLinks.stream().map(it-> links.indexOf(it)).collect(Collectors.toList());
                        List<String> krameirusLibraries = indicies.stream().map(index -> {
                            return (index >=0 && index < institutions.size()) ? institutions.get(index) : "";
                        }).collect(Collectors.toList());

                        List<String> pids = krameriusLinks.stream().map(it -> {
                            int i = it.indexOf("uuid:");
                            return it.substring(i);
                        }).collect(Collectors.toList());

                        if (selectedInstitution!= null) {
                            int selected = institutions.indexOf(selectedInstitution);
                            if (selected > -1 && selected < pids.size()) {
                                try {
                                    printer.printRecord( pids.get(selected), "dnntt",selectedInstitution ,nazev, identifier);
                                } catch (IOException e) {
                                    LOGGER.log(Level.SEVERE,e.getMessage(),e);
                                }
                            } else {
                                for (int i = 0; i < pids.size(); i++) {
                                    try {
                                        printer.printRecord(pids.get(i), "dnntt",krameirusLibraries.get(i) ,nazev, identifier);
                                    } catch (IOException e) {
                                        LOGGER.log(Level.SEVERE,e.getMessage(),e);
                                    }
                                }
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

}
