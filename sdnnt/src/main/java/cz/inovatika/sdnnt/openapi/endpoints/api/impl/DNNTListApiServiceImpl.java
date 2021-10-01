package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.openapi.endpoints.api.ListsApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists.CSVSolrDocumentOutput;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists.ModelDocumentOutput;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists.SolrDocumentOutput;
import cz.inovatika.sdnnt.openapi.endpoints.model.*;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.wflow.License;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
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
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_022_A;

// zmenit
public class DNNTListApiServiceImpl extends ListsApiService {

    // number of concurrent clients in case of exporting long csv file
    public static final int DEFAULT_CONCURRENT_CLIENTS = 3;
    protected static final Semaphore SEMAPHORE = new Semaphore(DEFAULT_CONCURRENT_CLIENTS);

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");
//if (doc.getFieldValue(MARC_015_A) != null || doc.getFieldValue(MARC_020_A) != null ||doc.getFieldValue(MARC_902_A) != null ) {

    public static final List<String> CATALOG_FIELDS = Arrays.asList("nazev", "identifier", "marc_856u", "dntstav", "historie_stavu", "marc_911u", MarcRecordFields.SIGLA_FIELD, "marc_911a",
            GRANULARITY_FIELD,
            MARC_015_A,
            MARC_020_A,
            MARC_902_A
            );

    static Logger LOGGER = Logger.getLogger(DNNTListApiServiceImpl.class.getName());

    CatalogIterationSupport catalogIterationSupport = new CatalogIterationSupport();

    @Override
    public Response addedDnnto(String instituion, OffsetDateTime dateTime, Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        List<String> plusList = (instituion != null) ?  new ArrayList<>(Arrays.asList(MarcRecordFields.SIGLA_FIELD+":"+instituion, "license:"+ License.dnnto.name())) :  new ArrayList<>(Arrays.asList("license:"+ License.dnnto.name()));
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }

        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);

        this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, new ArrayList<String>(),CATALOG_FIELDS, (rsp)->{
            String nextCursorMark = rsp.getNextCursorMark();
            SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem);
            for (SolrDocument resultDoc: rsp.getResults()) {
                emitDocument(instituion, License.dnnto.name(), false, new HashSet<String>(), resultDoc, solrDocumentOutput);
            }
            response.setNumFound((int) rsp.getResults().getNumFound());

            response.setResumptiontoken(nextCursorMark);
        });

        return Response.ok().entity(response).build();
    }

    @Override
    public Response addedDnntt(String institution, OffsetDateTime dateTime,  Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        List<String> plusList = (institution != null) ?   new ArrayList<>(Arrays.asList("marc_911a:"+institution, "license:"+License.dnntt.name())):  new ArrayList<>(Arrays.asList("license:"+License.dnntt.name()));
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }

        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);

        this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, new ArrayList<String>(),CATALOG_FIELDS, (rsp)->{
            String nextCursorMark = rsp.getNextCursorMark();
            SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem);
            for (SolrDocument resultDoc: rsp.getResults()) {
                emitDocument(institution, License.dnntt.name(), false, new HashSet<String>(), resultDoc, solrDocumentOutput);
            }
            response.setNumFound((int) rsp.getResults().getNumFound());
            response.setResumptiontoken(nextCursorMark);
        });

        return Response.ok().entity(response).build();
    }

    @Override
    public Response removedDnntt(String institution, OffsetDateTime dateTime,  Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        List<String> plusList = (institution != null) ?   new ArrayList<>(Arrays.asList(MarcRecordFields.SIGLA_FIELD+":"+institution, "license_history:"+License.dnntt.name())):  new ArrayList<>(Arrays.asList("license_history:"+License.dnntt.name()));
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }
        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);

        this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, new ArrayList<String>(),CATALOG_FIELDS, (rsp)->{
            String nextCursorMark = rsp.getNextCursorMark();
            SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem);
            for (SolrDocument resultDoc: rsp.getResults()) {
                emitDocument(institution, License.dnntt.name(), false, new HashSet<String>(), resultDoc, solrDocumentOutput);
            }
            response.setNumFound((int) rsp.getResults().getNumFound());
            response.setResumptiontoken(nextCursorMark);
        });

        return Response.ok().entity(response).build();
    }

    @Override
    public Response removedDnnto(String institution, OffsetDateTime dateTime,  Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        List<String> plusList = (institution != null) ?   new ArrayList<>(Arrays.asList(MarcRecordFields.SIGLA_FIELD+":"+institution, "license_history:"+License.dnnto.name())):  new ArrayList<>(Arrays.asList("license_history:"+License.dnnto.name()));
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }

        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);

        this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, new ArrayList<String>(),CATALOG_FIELDS, (rsp)->{
            String nextCursorMark = rsp.getNextCursorMark();
            SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem);
            for (SolrDocument resultDoc: rsp.getResults()) {
                emitDocument(institution, License.dnntt.name(), false, new HashSet<String>(), resultDoc, solrDocumentOutput);
            }
            response.setNumFound((int) rsp.getResults().getNumFound());
            response.setResumptiontoken(nextCursorMark);
        });

        return Response.ok().entity(response).build();
    }

    @Override
    public Response addedDnntoCsvExport(String institution, OffsetDateTime dateTime, Boolean uniq,SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = SEMAPHORE.tryAcquire();
            if (acquired) {
                List<String> plusList = (institution != null) ?  new ArrayList<>(Arrays.asList(MarcRecordFields.SIGLA_FIELD+":"+institution, "license:"+License.dnnto.name())) :  new ArrayList<>(Arrays.asList("license:"+ License.dnnto.name()));
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                return fullCSV(institution,License.dnnto.name(),uniq,plusList,new ArrayList<>(), CATALOG_FIELDS);
            } else {
                return Response.status(429).entity("Too many requests; Please wait and repeat request again").build();
            }
        } finally {
            if (acquired) {
                SEMAPHORE.release();
            }
        }
    }

    @Override
    public Response addedDnnttCsvExport(String institution, OffsetDateTime dateTime, Boolean uniq, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = SEMAPHORE.tryAcquire();
            if (acquired) {
                List<String> plusList = (institution != null) ?   new ArrayList<>(Arrays.asList(MarcRecordFields.SIGLA_FIELD+":"+institution, "license:"+License.dnntt.name())):  new ArrayList<>(Arrays.asList("license:"+License.dnntt.name()));
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                return fullCSV(institution,License.dnntt.name(), uniq,plusList, new ArrayList<>(), Arrays.asList("nazev", "identifier", "marc_856u", "dntstav", "historie_stavu", MarcRecordFields.MARC_911_U, MarcRecordFields.SIGLA_FIELD,"marc_911a"));
            } else {
                return Response.status(429).entity("Too many requests; Please wait and repeat request again").build();
            }
        } finally {
            if (acquired) {
                SEMAPHORE.release();
            }
        }
    }

    @Override
    public Response removedDnntoCsvExport(String institution, OffsetDateTime dateTime, Boolean uniq, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = SEMAPHORE.tryAcquire();
            if (acquired) {
                List<String> plusList = (institution != null) ?   new ArrayList<>(Arrays.asList(MarcRecordFields.SIGLA_FIELD+":"+institution, "license_history:"+License.dnnto.name())):  new ArrayList<>(Arrays.asList("license_history:"+License.dnnto.name()));
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                return fullCSV(institution,License.dnnto.name(), uniq,plusList, new ArrayList<>(), Arrays.asList("nazev", "identifier", "marc_856u", "dntstav", "historie_stavu", "marc_911u", "marc_910a","marc_911a"));
            } else {
                return Response.status(429).entity("Too many requests; Please wait and repeat request again").build();
            }
        } finally {
            if (acquired) {
                SEMAPHORE.release();
            }
        }
    }

    @Override
    public Response removedDnnttCsvExport(String institution, OffsetDateTime dateTime, Boolean uniq, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = SEMAPHORE.tryAcquire();
            if (acquired) {
                List<String> plusList = (institution != null) ?   new ArrayList<>(Arrays.asList(MarcRecordFields.SIGLA_FIELD+":"+institution, "license_history:"+License.dnntt.name())):  new ArrayList<>(Arrays.asList("license_history:"+License.dnntt.name()));
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                return fullCSV(institution,License.dnntt.name(), uniq,plusList, new ArrayList<>(), Arrays.asList("nazev", "identifier", "marc_856u", "dntstav", "historie_stavu", "marc_911u", "marc_910a","marc_911a"));
            } else {
                return Response.status(429).entity("Too many requests; Please wait and repeat request again").build();
            }
        } finally {
            if (acquired) {
                SEMAPHORE.release();
            }
        }
    }





    // TODO: Prodisktuovat instituce, marc911a,u, marc956u, marc856u a vazby na digitalni instance krameria
    private Response fullCSV( String selectedInstitution, String label, Boolean onlyUniqPids, List<String> plusList, List<String> minusList, List<String> fields) {
        try {
            Set<String> uniqe = new HashSet<>();

            File csvFile = File.createTempFile("temp","csv");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(csvFile), Charset.forName("UTF-8"));

            try (CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.EXCEL.withHeader("pid","label","institution","name","Aleph SKC identifier"))) {
                Map<String, String> map = new HashMap<>();

                SolrDocumentOutput documentOutput = new CSVSolrDocumentOutput(printer);
                this.catalogIterationSupport.iterate(map, null, plusList, minusList,fields, (doc)->{
                    emitDocument(selectedInstitution, label, onlyUniqPids, uniqe, doc, documentOutput);
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
                    }).header("Content-Disposition",contentDisposition).type("text/fullCSV").encoding("UTF-8").build();

        } catch (IOException e) {
            // todo
            throw new RuntimeException(e);
        }
    }

    /**
     * Print document to document output
     * @param selectedInstitution Selected institution
     * @param label Generating label
     * @param onlyUniqPids Flag says that pid should be unique in export
     * @param uniqe Set contains pids already outputted
     * @param doc Outputting doc
     * @param documentOutput DocumentOutput implementation
     */
    private void emitDocument(String selectedInstitution, String label, Boolean onlyUniqPids, Set<String> uniqe, SolrDocument doc, SolrDocumentOutput documentOutput) {
        Collection<Object> nazev = doc.getFieldValues("nazev");
        String identifier = (String) doc.getFieldValue("identifier");


        Collection<Object> mlinks911u = doc.getFieldValues("marc_911u");
        Collection<Object> mlinks856u =  doc.getFieldValues("marc_856u");
        Collection<Object> mlinks956u =  doc.getFieldValues("marc_956u");

        Collection<Object> minstitutions = doc.getFieldValues(MarcRecordFields.SIGLA_FIELD);
        //Collection<Object> minstitutions910a = doc.getFieldValues("marc_910a");

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




            if (mlinks911u != null && !mlinks911u.isEmpty() && minstitutions !=null && !minstitutions.isEmpty()) {

                List<String> institutions = new ArrayList(minstitutions);
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
                            //pidsForInstitution(selectedInstitution, label, printer, nazev, identifier, pid);
                            documentOutput.output(selectedInstitution, label, nazev, identifier, pid);
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
                                    //pidsForInstitution( s, label, printer, nazev, identifier, pids.get(i));
                                    documentOutput.output( s, label,  nazev, identifier, pids.get(i));
                                    uniqe.add(pids.get(i));
                                }
                            }
                        } else {
                            for (int i = 0; i < pids.size(); i++) {
                                String s = i < siglas.size() ? siglas.get(i) : "";
                                //pidsForInstitution( s, label, printer, nazev, identifier, pids.get(i));
                                documentOutput.output( s, label,  nazev, identifier, pids.get(i));
                            }
                        }
                }
            } else {
                // pid nepatri zadne instituci, uvedou se jenom pidy, je pritomno v reportu bez ohledu na vybranou knihovnu
                if (onlyUniqPids) {
                    for (int i = 0; i < pids.size(); i++) {
                        if (!uniqe.contains( pids.get(i))) {
                            //pidsForInstitution("", label, printer, nazev, identifier, pids.get(i));
                            documentOutput.output( "", label,  nazev, identifier, pids.get(i));
                            uniqe.add(pids.get(i));
                        }
                    }
                } else {
                    //pidsForInstitution("", label, printer, nazev, identifier, pids.toArray(new String[pids.size()]));
                    documentOutput.output( "", label, nazev, identifier, pids.toArray(new String[pids.size()]));
                }
            }
        }
    }


}









