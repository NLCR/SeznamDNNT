package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import cz.inovatika.sdnnt.Options;

import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.index.utils.GranularityUtils;
import cz.inovatika.sdnnt.openapi.endpoints.api.ListsApiService;
import cz.inovatika.sdnnt.openapi.endpoints.api.NotFoundException;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists.CSVSolrDocumentOutput;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists.ModelDocumentOutput;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists.SolrDocumentOutput;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport;
import cz.inovatika.sdnnt.openapi.endpoints.model.*;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration;
import cz.inovatika.sdnnt.utils.LinksUtilities;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.glassfish.jersey.media.multipart.ContentDisposition;
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

import static cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport.*;

import static cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists.SolrDocumentOutput.*;

// zmenit
public class DNNTListApiServiceImpl extends ListsApiService {

    public static final List<String> CATALOG_FIELDS = Arrays.asList(
            "nazev", 
            "identifier",
            ID_SDNNT , 
            "marc_856u", 
            "dntstav", 
            "historie_stavu", 
            "marc_911u", 
            MarcRecordFields.SIGLA_FIELD, 
            "marc_911a",
            GRANULARITY_FIELD,
            MARC_015_A,
            MARC_020_A,
            MARC_902_A,
            LICENSE_FIELD,
            FMT_FIELD,
            DIGITAL_LIBRARIES,
            ID_ISSN,
            ID_ISBN,
            ID_ISMN,
            
            "controlfield_001",
            "controlfield_003",
            "controlfield_005",
            "controlfield_008",
            MASTERLINKS_FIELD,
            MASTERLINKS_DISABLED_FIELD
            
            //MarcRecordFields.RAW_FIELD
    );

    // number of concurrent clients in case of exporting long csv file
    public static int DEFAULT_CONCURRENT_CSV_CLIENTS = Options.getInstance().intKey("openapi.threads.csv", 12);
    public static int DEFAULT_CONCURRENT_JSON_CLIENTS = Options.getInstance().intKey("openapi.threads.csv", 22);

    public static int MAXIMAL_NUMBER_OF_ITEMS_IN_REQUEST = Options.getInstance().intKey("openapi.maximumrows", 8000);;

    protected static final Semaphore CSV_SEMAPHORE = new Semaphore(DEFAULT_CONCURRENT_CSV_CLIENTS);
    protected static final Semaphore JSON_SEMAPHORE = new Semaphore(DEFAULT_CONCURRENT_JSON_CLIENTS);

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

    public static final List<String> DEFAULT_OUTPUT_FIELDS = Arrays.asList(PID_KEY, SELECTED_INSTITUTION_KEY, LABEL_KEY, NAZEV_KEY,IDENTIFIER_KEY, LICENSE_FIELD, FMT_FIELD, DNTSTAV_FIELD);
    static Logger LOGGER = Logger.getLogger(DNNTListApiServiceImpl.class.getName());




    protected CatalogIterationSupport catalogIterationSupport = new CatalogIterationSupport();
    protected CheckKrameriusConfiguration kramConf;
    protected Map<String,String> dlMap = new HashMap<>();
    
    public DNNTListApiServiceImpl() {
        
        JSONObject conf = Options.getInstance().getJSONObject("check_kramerius");
        this.kramConf = CheckKrameriusConfiguration.initConfiguration(conf);
        
        List<InstanceConfiguration> instances = this.kramConf.getInstances();
        for (InstanceConfiguration instance : instances) {
            String acronym = instance.getAcronym();
            String sigla = instance.getSigla() != null ? instance.getSigla() : acronym.toUpperCase();
            dlMap.put(acronym, sigla);
        }
    }
    
    @Override
    public Response addedDnnto(String dl, String format, String institution, OffsetDateTime dateTime, Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        
        List<String> plusList = new ArrayList<>(Arrays.asList("license:"+ License.dnnto.name()+" OR "+
        "(granularity_license_cut:"+License.dnnto.name()+")", "id_pid:uuid"));

        
        aStav(plusList);
        
        institutionFilterPlusList(institution, plusList);
        digitalLibrariesFilterPlusList(dl, plusList);
        formatFilterPlusList(format, plusList);

        ArrayList<String> minusList = new ArrayList<String>();
        removeDMinusList(minusList);
        
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }

        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);
        if (rows <= MAXIMAL_NUMBER_OF_ITEMS_IN_REQUEST) {
            this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, minusList,CATALOG_FIELDS, (rsp)->{
                String nextCursorMark = rsp.getNextCursorMark();
                SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem, MapUtils.invertMap(dlMap));
                for (SolrDocument resultDoc: rsp.getResults()) {
                    emitDocument(null, false, new HashSet<String>(), resultDoc, solrDocumentOutput, new ArrayList<>(), License.dnnto.name(), false);
                }
                //response.setNumFound((int) rsp.getResults().getNumFound());
                response.setResumptiontoken(nextCursorMark);
            });
            return Response.ok().entity(response).build();
        } else {
            return Response.status(400).entity(jsonError( "Maximum number of items exceeded")).build();
        }
    }

    private String license(SolrDocument resultDoc) {
        Collection<Object> fieldValues = resultDoc.getFieldValues(LICENSE_FIELD);
        if (fieldValues != null) {
            List<String> collect = fieldValues.stream().map(Object::toString).collect(Collectors.toList());
            return collect != null && collect.size() > 0 ? collect.get(0) : "";
        } else  return "";
    }

    @Override
    public Response addedDnntt(String dl, String format, String institution, OffsetDateTime dateTime,  Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        List<String> plusList = 
                new ArrayList<>(Arrays.asList("license:"+License.dnntt.name() +" OR "+
        "(granularity_license_cut:"+License.dnntt.name()+")","id_pid:uuid" ));
        
        aStav(plusList);
        
        institutionFilterPlusList(institution, plusList);
        digitalLibrariesFilterPlusList(dl, plusList);
        formatFilterPlusList(format, plusList);

        List<String> minusList = new ArrayList<>();
        removeDMinusList(minusList);
        
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }

        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);

        if (rows <= MAXIMAL_NUMBER_OF_ITEMS_IN_REQUEST) {
            this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, minusList,CATALOG_FIELDS, (rsp)->{
                String nextCursorMark = rsp.getNextCursorMark();
                SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem, MapUtils.invertMap(dlMap));
                for (SolrDocument resultDoc: rsp.getResults()) {
                    emitDocument(null,false, new HashSet<String>(), resultDoc, solrDocumentOutput, DEFAULT_OUTPUT_FIELDS, License.dnntt.name(), false);
                }
                //response.setNumFound((int) rsp.getResults().getNumFound());
                response.setResumptiontoken(nextCursorMark);
            });
            return Response.ok().entity(response).build();
        } else {

            return Response.status(400).entity(jsonError( "Maximum number of items exceeded")).build();
        }
    }

    private String jsonError(String msg) {
        JSONObject object = new JSONObject();
        object.put("error", msg);
        return object.toString();
    }

    
    
    @Override
    public Response removedDnntt(String dl, String format, String institution, OffsetDateTime dateTime,  Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        //historie_stavu_cut
        List<String> plusList = 
                new ArrayList<>(Arrays.asList("historie_stavu_cut:"+License.dnntt.name()+ " OR dntstav:N", "id_pid:uuid"));
        List<String> minusList = new ArrayList<>(
                Arrays.asList(MarcRecordFields.LICENSE_FIELD+":"+License.dnntt.name())
        );
        
        aStav(plusList);
        
        institutionFilterPlusList(institution, plusList);
        digitalLibrariesFilterPlusList(dl, plusList);
        formatFilterPlusList(format, plusList);
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }
        
        removeDMinusList(minusList);
        
        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);

        if (rows <= MAXIMAL_NUMBER_OF_ITEMS_IN_REQUEST) {
            this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String, String>(), null, plusList, minusList, CATALOG_FIELDS, (rsp) -> {
                String nextCursorMark = rsp.getNextCursorMark();
                SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem, MapUtils.invertMap(dlMap));
                for (SolrDocument resultDoc : rsp.getResults()) {
                    emitDocument(null, false, new HashSet<String>(), resultDoc, solrDocumentOutput, DEFAULT_OUTPUT_FIELDS, null, false);
                }
                //response.setNumFound((int) rsp.getResults().getNumFound());
                response.setResumptiontoken(nextCursorMark);
            });
            return Response.ok().entity(response).build();
        } else {
            return Response.status(400).entity(jsonError( jsonError("Maximum number of items exceeded"))).build();
        }
    }

    private void institutionFilterPlusList(String dl, List<String> plusList) {
        if (dl != null) {
            plusList.add(SIGLA_FIELD+":"+dl);
        }
    }

    private void digitalLibrariesFilterPlusList(String dl, List<String> plusList) {
        if (dl != null) {
            if (dlMap.containsKey(dl)) {
                dl = this.dlMap.get(dl);
            }
            plusList.add("digital_libraries:"+dl);
        }
    }


    private void formatFilterPlusList(String fmt, List<String> plusList) {
        if (fmt != null) {
            plusList.add(MarcRecordFields.FMT_FIELD+":"+fmt);
        }
    }

    private void removeDMinusList(List<String> minusList) {
        minusList.add(MarcRecordFields.DNTSTAV_FIELD+":D");
        minusList.add(MarcRecordFields.DNTSTAV_FIELD+":PA");
    }

    
    @Override
    public Response removedDnnto(String dl, String format, String institution, OffsetDateTime dateTime,  Integer rows, String resumptionToken, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        String token = resumptionToken != null ? resumptionToken : "*";
        List<String> plusList = 
                new ArrayList<>(Arrays.asList("historie_stavu_cut:"+License.dnnto.name()+ " OR dntstav:N", "id_pid:uuid"));
        List<String> minusList = new ArrayList<>(
                Arrays.asList(MarcRecordFields.LICENSE_FIELD+":"+License.dnnto.name())
        );
        
        aStav(plusList);
        
        digitalLibrariesFilterPlusList(dl, plusList);
        formatFilterPlusList(format, plusList);
        if (dateTime != null) {
            String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
            plusList.add("datum_stavu:["+utc+" TO *]");
        }
        
        removeDMinusList(minusList);
        
        final ListitemResponse response = new ListitemResponse();
        ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
        response.setItems(arrayOfListitem);

        if (rows <= MAXIMAL_NUMBER_OF_ITEMS_IN_REQUEST) {
            this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, minusList,CATALOG_FIELDS, (rsp)->{
                String nextCursorMark = rsp.getNextCursorMark();
                SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem, MapUtils.invertMap(dlMap));
                for (SolrDocument resultDoc: rsp.getResults()) {
                    emitDocument(null,  false, new HashSet<String>(), resultDoc, solrDocumentOutput, DEFAULT_OUTPUT_FIELDS,null, false);
                }
                response.setNumFound((int) rsp.getResults().getNumFound());
                response.setResumptiontoken(nextCursorMark);
            });
            return Response.ok().entity(response).build();
        } else {
            return Response.status(400).entity(jsonError( "Maximum number of items exceeded")).build();
        }
    }



    @Override
    public Response addedDnntoCsvExport(String dl, String format, String institution, OffsetDateTime dateTime, Boolean uniq, Boolean donotemitparent,List<String> list,SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = CSV_SEMAPHORE.tryAcquire();
            if (acquired) {
                List<String> plusList = 
                        new ArrayList<>(Arrays.asList( "license:"+ License.dnnto.name()+" OR "+"granularity_license_cut:"+License.dnnto.name(), "id_pid:uuid"));
                
                aStav(plusList);
                institutionFilterPlusList(institution, plusList);
                digitalLibrariesFilterPlusList(dl, plusList);
                formatFilterPlusList(format, plusList);

                ArrayList<String> minusList = new ArrayList<>();
                removeDMinusList(minusList);
                
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                
                if (list == null || list.isEmpty()) {
                    return fullCSV(institution,License.dnnto.name(),uniq,plusList,minusList, CATALOG_FIELDS, DEFAULT_OUTPUT_FIELDS, donotemitparent);
                } else {
                    return fullCSV(institution,License.dnnto.name(),uniq,plusList,minusList, CATALOG_FIELDS, makeSurePids(list), donotemitparent);
                }
            } else {
                return Response.status(429).entity(jsonError("Maximum number of items exceeded")).build();
            }
        } finally {
            if (acquired) {
                CSV_SEMAPHORE.release();
            }
        }
    }

    private void aStav(List<String> plusList) {
        plusList.add(MarcRecordFields.DNTSTAV_FIELD+":A");
    }

    private List<String> makeSurePids(List<String> list) {
        // wrong openapi serialization
        if (list.size() ==1 && list.get(0).contains(",")) {
            list = Arrays.stream(list.get(0).split(",")).collect(Collectors.toList());
        }
        if(!list.contains(PID_KEY)) { list.add(0,PID_KEY); }
        return list;
    }


    @Override
    public Response addedDnnttCsvExport(String dl, String format, String institution, OffsetDateTime dateTime, Boolean uniq, Boolean donotemitparent,List<String> list, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = CSV_SEMAPHORE.tryAcquire();
            if (acquired) {
                List<String> plusList = 
                        new ArrayList<>(Arrays.asList("license:"+ License.dnntt.name()+" OR "+"granularity_license_cut:"+License.dnntt.name(), "id_pid:uuid"));

                aStav(plusList);
                
                
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                
                institutionFilterPlusList(institution, plusList);
                digitalLibrariesFilterPlusList(dl, plusList);
                formatFilterPlusList(format, plusList);
                
                ArrayList<String> minusList = new ArrayList<>();
                removeDMinusList(minusList);

                if (list != null && !list.isEmpty()) {
                    return fullCSV(institution,License.dnntt.name(), uniq,plusList, minusList, CATALOG_FIELDS, makeSurePids(list), donotemitparent);
                } else {
                    return fullCSV(institution,License.dnntt.name(), uniq,plusList, minusList, CATALOG_FIELDS,DEFAULT_OUTPUT_FIELDS, donotemitparent);
                }
            } else {
                return Response.status(429).entity(jsonError("Maximum number of items exceeded")).build();
            }
        } finally {
            if (acquired) {
                CSV_SEMAPHORE.release();
            }
        }
    }


    @Override
    public Response removedDnntoCsvExport(String dl, String format, String institution, OffsetDateTime dateTime, Boolean uniq,List<String> list, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = CSV_SEMAPHORE.tryAcquire();
            if (acquired) {
                //historie_stavu_cut
                List<String> plusList = 
                        new ArrayList<>(Arrays.asList("historie_stavu_cut:"+License.dnnto.name()+ " OR dntstav:N", "id_pid:uuid"));
                List<String> minusList = new ArrayList<>(
                        Arrays.asList(MarcRecordFields.LICENSE_FIELD+":"+License.dnnto.name())
                );
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                institutionFilterPlusList(institution, plusList);
                digitalLibrariesFilterPlusList(dl, plusList);
                formatFilterPlusList(format, plusList);
                
                removeDMinusList(minusList);

                if (list != null && !list.isEmpty()) {
                    return fullCSV(institution,License.dnnto.name(), uniq,plusList,minusList, CATALOG_FIELDS, makeSurePids(list),false);
                } else {
                    return fullCSV(institution,License.dnnto.name(), uniq,plusList, minusList, CATALOG_FIELDS, DEFAULT_OUTPUT_FIELDS, false);
                }

            } else {
                return Response.status(429).entity(jsonError("Too many requests; Please wait and repeat request again")).build();
            }
        } finally {
            if (acquired) {
                CSV_SEMAPHORE.release();
            }
        }
    }


    @Override
    public Response removedDnnttCsvExport(String dl, String format, String institution, OffsetDateTime dateTime, Boolean uniq,List<String> list, SecurityContext securityContext, ContainerRequestContext containerRequestContext) throws NotFoundException {
        boolean acquired =  false;
        try {
            acquired = CSV_SEMAPHORE.tryAcquire();
            if (acquired) {

                List<String> plusList = 
                        new ArrayList<>(Arrays.asList("historie_stavu_cut:"+License.dnntt.name()+ " OR dntstav:N", "id_pid:uuid"));
                List<String> minusList = new ArrayList<>(
                        Arrays.asList(MarcRecordFields.LICENSE_FIELD+":"+License.dnntt.name())
                );

                
                if (dateTime != null) {
                    String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                    plusList.add("datum_stavu:["+utc+" TO *]");
                }
                institutionFilterPlusList(institution, plusList);
                digitalLibrariesFilterPlusList(dl, plusList);
                formatFilterPlusList(format, plusList);
                
                removeDMinusList(minusList);
                
                if (list != null && !list.isEmpty()) {
                    return fullCSV(institution,License.dnntt.name(), uniq,plusList, minusList, CATALOG_FIELDS, makeSurePids(list),false);
                } else {
                    return fullCSV(institution,License.dnntt.name(), uniq,plusList, minusList, CATALOG_FIELDS, DEFAULT_OUTPUT_FIELDS, false);
                }
            } else {
                return Response.status(429).entity(jsonError("Too many requests; Please wait and repeat request again")).build();
            }
        } finally {
            if (acquired) {
                CSV_SEMAPHORE.release();
            }
        }
    }


    // TODO: Prodisktuovat instituce, marc911a,u, marc956u, marc856u a vazby na digitalni instance krameria
    private Response fullCSV( String selectedInstitution, String label, Boolean onlyUniqPids, List<String> plusList, List<String> minusList, List<String> fetchingFields, List<String> outputFields, Boolean doNotEmitParent) {
        try {
            Set<String> uniqe = new HashSet<>();
            File csvFile = File.createTempFile("temp","csv");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(csvFile), Charset.forName("UTF-8"));

            // printer header
            // list - header
            try (CSVPrinter printer = new CSVPrinter(outputStreamWriter, CSVFormat.EXCEL.withHeader(outputFields.toArray(new String[outputFields.size()])))) {
                Map<String, String> map = new HashMap<>();

                SolrDocumentOutput documentOutput = new CSVSolrDocumentOutput(printer);
                // select only this fields
                this.catalogIterationSupport.iterate(map, null, plusList, minusList,fetchingFields, (doc)->{
                    emitDocument(null,  onlyUniqPids, uniqe, doc, documentOutput, outputFields, label, doNotEmitParent);
                }, "identifier");
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
     * Vyblije dokument do csv nebo do modelu 
     * @param selectedInstitution Selected institution
     * @param documentLicense Generating documentLicense
     * @param onlyUniqPids Flag says that pid should be unique in export
     * @param uniqe Set contains pids already outputted
     * @param doc Outputting doc
     * @param documentOutput DocumentOutput implementation
     */
    private void emitDocument(Pair<String,String> digitalLibraryFilter, Boolean onlyUniqPids, Set<String> uniqe, SolrDocument doc, SolrDocumentOutput documentOutput, List<String> outputFields, String requestedLicense, Boolean doNotEmit) {
        Collection<Object> nazev = doc.getFieldValues("nazev");
        String identifier = (String) doc.getFieldValue("identifier");
        
        
        String stav  = (String) doc.getFirstValue(MarcRecordFields.DNTSTAV_FIELD);
        String documentLicense = license(doc);
        
        
        //Collection<Object> mdigitallibraries = doc.getFieldValues("digital_libraries");
        Object fmt = doc.getFieldValue(FMT_FIELD);

        Collection<Object> granularityField = doc.getFieldValues("granularity");


        List<String> minstitutions = doc.getFieldValues(MarcRecordFields.SIGLA_FIELD) != null ? doc.getFieldValues(SIGLA_FIELD).stream().map(Object::toString).collect(Collectors.toList())  : new ArrayList<>();
        List<String> mdigitallibraries = doc.getFieldValues("digital_libraries") != null ? doc.getFieldValues("digital_libraries").stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();

        // 911u a 856u 
        final List<String> links = LinksUtilities.krameriusMergedLinksFromDocument(doc);
        if (!links.isEmpty()) {
            
            /**
             * pidy z granularity / 
             */
            
            List<String> granularity = granularityField != null ? granularityField.stream().map(Object::toString).collect(Collectors.toList()): new ArrayList<>();
            
            /**
             * Pokud je pritomno pole 911a a 911u, pak je mapovani pid - instituce - Pokud ne, pak se neda rict komu patri - zadna instituce
             * uz prebito granularitou
             * kazda polozka ma acronym 
             */
            // Vraci vsechny linky do krameriu -> filtruje jine
            List<String> krameriusLinks = links.stream().map(String::toLowerCase).filter(it -> it.contains("uuid:")).collect(Collectors.toList());

            // Bordel v datech ?? Obsahuji spatne prefixy a postfixy ?   musi se odfiltorvat !!! 
            List<String> pids = krameriusLinks.stream().map(PIDSupport::pidFromLink).map(PIDSupport::pidNormalization).collect(Collectors.toList());
            if (granularity != null && !granularity.isEmpty()) {
                
                for (String str : granularity) {
                    JSONObject jsonStr = new JSONObject(str);
                    if (GranularityUtils.isGranularityItem(jsonStr)) {
                        String link = jsonStr.getString("link");
                        String pidFromGranularity =  pidNormalization(pidFromLink(link));
                        pids.remove(pidFromGranularity);
                    }
                }
            }
            
            if (onlyUniqPids) {
                for (int i = 0; i < pids.size(); i++) {
                    if (!uniqe.contains( pids.get(i))) {
                        Map<String,Object> d =  doc(mdigitallibraries,minstitutions,  documentLicense, stav ,  nazev, identifier, granularity,fmt.toString(), pids.get(i));
                        d = enIdSdnnt(d, doc);
                        d = enMarcFields(d, doc);
                        d = enMasterLinks(d, doc);
                        documentOutput.output(digitalLibraryFilter, d,outputFields, requestedLicense, doNotEmit);
                        uniqe.add(pids.get(i));
                    }
                }
            } else {
                Map<String,Object> d = doc(mdigitallibraries, minstitutions,  documentLicense,stav, nazev, identifier, granularity, fmt.toString(),pids.toArray(new String[pids.size()]));
                d = enIdSdnnt(d, doc);
                d = enMarcFields(d, doc);
                d = enMasterLinks(d, doc);
                documentOutput.output(digitalLibraryFilter,d,outputFields, requestedLicense, doNotEmit);
            }
        }
    }
    
    
    
    private Map<String, Object> enMarcFields(Map<String, Object> document, SolrDocument doc) {
        String cField01 =  (String) doc.getFirstValue("controlfield_001");
        String cField03 = (String) doc.getFirstValue("controlfield_003");
        String cField05 = (String) doc.getFirstValue("controlfield_005");
        String cField08 = (String) doc.getFirstValue("controlfield_008");

        String raw = (String) doc.getFirstValue(MarcRecordFields.RAW_FIELD);

        document.put(SolrDocumentOutput.CONTROL_FIELD_001_KEY, cField01);
        document.put(SolrDocumentOutput.CONTROL_FIELD_003_KEY, cField03);
        document.put(SolrDocumentOutput.CONTROL_FIELD_005_KEY, cField05);
        document.put(SolrDocumentOutput.CONTROL_FIELD_008_KEY, cField08);
        if (raw != null) {
            document.put(SolrDocumentOutput.RAW_KEY, raw);
        }

        return document;
    }


    private Map<String, Object> doc(List<String> digitalLibs, List<String> instituions, String label, String stav, Collection<Object> nazev, String identifier,List<String> granularity, String fmt, String ... p) {
        
        Map<String, Object> document = new HashMap<>();
        document.put(SolrDocumentOutput.SELECTED_INSTITUTION_KEY, instituions);
        document.put(SolrDocumentOutput.SELECTED_DL_KEY, digitalLibs);
        document.put(SolrDocumentOutput.LABEL_KEY, label);
        document.put(SolrDocumentOutput.DNTSTAV_KEY, stav);
        document.put(SolrDocumentOutput.NAZEV_KEY, nazev.stream().map(Object::toString).collect(Collectors.joining(" ")));
        document.put(SolrDocumentOutput.IDENTIFIER_KEY, identifier);

        List<String> pids = new ArrayList<>();
        Arrays.stream(p).forEach(pids::add);

        document.put(SolrDocumentOutput.PIDS_KEY, pids);
        document.put(GRANUARITY_KEY, granularity);
        document.put(FMT_KEY, fmt);

        return document;
    }
    
    private Map<String, Object> enDoc(Map<String, Object> doc, String key, Object val) {
        doc.put(key, val);
        return doc;
    }
    
    private Map<String, Object> enIdSdnnt(Map<String, Object> doc, SolrDocument sdoc) {
        Collection<Object> fieldValues = sdoc.getFieldValues(MarcRecordFields.ID_SDNNT);
        if (fieldValues != null && !fieldValues.isEmpty()) {
            return enDoc(doc, SolrDocumentOutput.SDNNT_ID_KEY, fieldValues.toArray()[0]);
        }
        return doc;
    }

    private Map<String, Object> enMasterLinks(Map<String, Object> doc, SolrDocument sdoc) {
        Collection<Object> fieldValues = sdoc.getFieldValues(MarcRecordFields.MASTERLINKS_FIELD);
        Object mdisabled = sdoc.getFieldValue(MarcRecordFields.MASTERLINKS_DISABLED_FIELD);
        enDoc(doc, SolrDocumentOutput.MASTERLINKS_KEY, fieldValues != null ? fieldValues.stream().map(Objects::toString).collect(Collectors.toList()) : new ArrayList<>());
        if (mdisabled != null) {
            enDoc(doc, SolrDocumentOutput.MASTERLINKS_DISABLED_KEY,   Boolean.valueOf(mdisabled.toString()));
        }
        return doc;
    }

//    boolean acquired =  false;
//    try {
//        acquired = CSV_SEMAPHORE.tryAcquire();
//        if (acquired) {
//
//            List<String> plusList = 
//                    new ArrayList<>(Arrays.asList("historie_stavu_cut:"+License.dnntt.name()+ " OR dntstav:N", "id_pid:uuid"));
//            List<String> minusList = new ArrayList<>(
//                    Arrays.asList(MarcRecordFields.LICENSE_FIELD+":"+License.dnntt.name())
//            );
//
//            
//            if (dateTime != null) {
//                String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
//                plusList.add("datum_stavu:["+utc+" TO *]");
//            }
//            institutionFilterPlusList(institution, plusList);
//            digitalLibrariesFilterPlusList(dl, plusList);
//            formatFilterPlusList(format, plusList);
//            
//            removeDMinusList(minusList);
//            
//            if (list != null && !list.isEmpty()) {
//                return fullCSV(institution,License.dnntt.name(), uniq,plusList, minusList, CATALOG_FIELDS, makeSurePids(list),false);
//            } else {
//                return fullCSV(institution,License.dnntt.name(), uniq,plusList, minusList, CATALOG_FIELDS, DEFAULT_OUTPUT_FIELDS, false);
//            }
//        } else {
//            return Response.status(429).entity(jsonError("Too many requests; Please wait and repeat request again")).build();
//        }
//    } finally {
//        if (acquired) {
//            CSV_SEMAPHORE.release();
//        }
//    }

    
    @Override
    public Response changes(String dl, String format, String institution, OffsetDateTime dateTime, Integer rows, String resumptionToken, SecurityContext securityContext,
            ContainerRequestContext containerRequestContext) throws NotFoundException {
          boolean acquired =  false;
          try {
              acquired = JSON_SEMAPHORE.tryAcquire();
              if (acquired) {
                  String token = resumptionToken != null ? resumptionToken : "*";
                  List<String> minusList = new ArrayList<>();
    
                  List<String> plusList = new ArrayList<>(Arrays.asList("id_pid:uuid", 
                          "dntstav:*"
                  ));
                  
                  institutionFilterPlusList(institution, plusList);
                  digitalLibrariesFilterPlusList(dl, plusList);
                  formatFilterPlusList(format, plusList);
    
                  removeDMinusList(minusList);
                  
                  if (dateTime != null) {
                      String utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(dateTime.truncatedTo(ChronoUnit.MILLIS));
                      plusList.add("datum_stavu:["+utc+" TO *]");
                  }
    
                  final ListitemResponse response = new ListitemResponse();
                  ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
                  response.setItems(arrayOfListitem);
                  if (rows <= MAXIMAL_NUMBER_OF_ITEMS_IN_REQUEST) {
                      this.catalogIterationSupport.iterateOnePage(rows, token, new HashMap<String,String>(),null, plusList, minusList,CATALOG_FIELDS, (rsp)->{
                          String nextCursorMark = rsp.getNextCursorMark();
                          SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem, MapUtils.invertMap(dlMap));
                          for (SolrDocument resultDoc: rsp.getResults()) {
                              emitDocument(null,  false, new HashSet<String>(), resultDoc, solrDocumentOutput, new ArrayList<>(), null, false);
                          }
                          response.setNumFound((int) rsp.getResults().getNumFound());
                          response.setResumptiontoken(nextCursorMark);
                      });
                      return Response.ok().entity(response).build();
                  } else {
                      return Response.status(400).entity(jsonError( "Maximum number of items exceeded")).build();
                  }
              
              } else {
                  return Response.status(429).entity(jsonError("Too many requests; Please wait and repeat request again")).build();
              }
            } finally {
            if (acquired) {
                JSON_SEMAPHORE.release();
            }
        }

          
    }

    
    @Override
    public Response info(String ident,  String digitalLibrary, SecurityContext securityContext,ContainerRequestContext crc) throws NotFoundException {
        // do some magic!
        try {
            int maxRowsInCaseOfIdent = 1000;
            try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
                final ListitemResponse response = new ListitemResponse();
                ArrayOfListitem arrayOfListitem = new ArrayOfListitem();
                response.setItems(arrayOfListitem);
                
                SolrQuery q = (new SolrQuery("*")).setRows(maxRowsInCaseOfIdent).setSort(SolrQuery.SortClause.asc("identifier"));
                q.addFilterQuery("NOT dntstav:D");
                q.addFilterQuery("dntstav:*");
                q.addFilterQuery("id_pid:uuid");
                
                Pair<String,String> digitalLibraryPair = null;
                
                //String dgFilterValue = null;
                if (digitalLibrary != null) {
                    // acronym <=> sigla 
                    // zadal acronym 
                    if (this.dlMap.containsKey(digitalLibrary.toLowerCase())) {
                        String value = this.dlMap.get(digitalLibrary.toLowerCase());
                        digitalLibraryPair = Pair.of(digitalLibrary.toLowerCase(), value);
                    } else {
                        Set<String> keySet = this.dlMap.keySet();
                        for (String key : keySet) {
                            String value = this.dlMap.get(key);
                            if (value.toLowerCase().equals(digitalLibrary.toLowerCase())) {
                                digitalLibraryPair = Pair.of(key, value);
                            }
                            break;
                        }
                    }
                }
                
                if (digitalLibraryPair != null) {
                    q.addFilterQuery("digital_libraries:"+digitalLibraryPair.getRight());
                }
                
                q.addFilterQuery("id_all_identifiers:\""+ident+"\" OR id_pid:\""+ident+"\" OR identifier:\""+ident+"\" OR id_sdnnt:\""+ident+"\"");
                QueryResponse rsp = solr.query(DataCollections.catalog.name(),q);
                SolrDocumentOutput solrDocumentOutput = new ModelDocumentOutput(arrayOfListitem, MapUtils.invertMap(dlMap));
                for (SolrDocument resultDoc: rsp.getResults()) {
                    emitDocument(digitalLibraryPair,  false, new HashSet<String>(), resultDoc, solrDocumentOutput, new ArrayList<>(), null, false);
                }
                response.setNumFound((int) rsp.getResults().getNumFound());
                return Response.ok().entity(response).build();
            }
        } catch (SolrServerException | IOException e) {
            return Response.status(500).entity(jsonError( e.getMessage() )).build();
        }
    }
}

