package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.FMT_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.GRANULARITY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_911_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_956_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SIGLA_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.YEAR_OF_PUBLICATION_1;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.YEAR_OF_PUBLICATION_2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.utils.MarcRecordUtils;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.services.EUIPOInitalExportService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.services.utils.ISO693Converter;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class EUIPOInitialExportServiceImpl implements EUIPOInitalExportService {
    
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_AUTHOR_LENGTH = 100;
    
    private static final int MAX_YEAR = 2100;
    private static final int MIN_YEAR = 1440;

    private Logger logger = Logger.getLogger(EUIPOInitalExportService.class.getName());

    
    /** Configuration key for filters; only one for both types **/
    private static final String FILTERS_KEY = "filters";

    private static final String NONPARSABLE_DATES_KEY  ="nonparsabledates";
    
    
    /** SE template key */
    private static final String SE_TEMPLATE_KEY = "setemplate";
    
    /** BK template  key */
    private static final String BK_TEMPLATE_KEY = "bktemplate";
    
    /** Set of iteration states key  */
    private static final String STATES_KEY = "states";

    /** Maximum rows in spreadsheet */
    private static final String MAXROWS_KEY = "maxrows";

    /** Maximum rows in spreadsheet */
    private static final String BATCHROWS_KEY = "batchrows";

    /** Output folder key */
    private static final String FOLDER_KEY = "folder";

//    public static final Map<String, String> ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY = new HashMap<>();
//    static {
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("alb", "sqi");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("arm", "hye");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("baq", "eus");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("baq", "eus");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("tib", "bod");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("bur", "mya");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("bur", "mya");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("cze", "ces");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("cze", "ces");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("chi", "zho");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("wel", "cym");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("cze", "ces");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("ger", "deu");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("dut", "nld");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("gre", "ell");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("baq", "eus");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("per", "fas");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("fre", "fra");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("geo", "kat");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("ice", "isl");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("mac", "mkd");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("mao", "mri");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("may", "msa");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("may", "msa");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("bur", "mya");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("dut", "nld");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("rum", "ron");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("slo", "slk");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("wel", "cym");
//        ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.put("chi", "zho");
//    }
    
    /** Default output folder  */
    public static final String DEFAULT_OUTPUT_FOLDER = System.getProperty("user.home") + File.separator + ".sdnnt/dump";

    // default initial filter
    /** Default initial bk filter */
    //private static final List<String> DEFAULT_INITIAL_BK_FILTER = Arrays.asList("( (NOT date1_int:*) OR  date1_int:[* TO 2003] )", "setSpec:SKC");
    //private static final List<String> DEFAULT_INITIAL_BK_FILTER = Arrays.asList("( (NOT date1_int:*) )", "setSpec:SKC");
    private static final List<String> DEFAULT_INITIAL_BK_FILTER = Arrays.asList( "setSpec:SKC");
    
    /** Default initial se filter */
    private static final List<String> DEFAULT_INITIAL_SE_FILTER = Arrays.asList("setSpec:SKC");
    
    public static final List<Pattern> DEFAULT_REGULAR_EXPRESSIONS_NONPARSABLE_DATES  = Arrays.asList(
        Pattern.compile("1.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("200.*", Pattern.CASE_INSENSITIVE)
    );
    
    // Default states for initial import
    public static final List<String> DEFAULT_STATES = Arrays.asList("A", "PA", "NL", "NPA");

    protected String loggerPostfix;

    public static final int UPDATE_BATCH_LIMIT = 100;
    public static final int SPREADSHEET_LIMIT = 20000;

    public static final int FETCH_LIMIT = 1000;

    private List<String> states = DEFAULT_STATES;

    //private List<String> filters = new ArrayList<>(Arrays.asList("identifier:\"oai:aleph-nkp.cz:SKC01-000208367\""));
    private List<String> filters = null;// DEFAULT_INITIAL_BK_FILTER;
    private List<Pattern> compiledPatterns = DEFAULT_REGULAR_EXPRESSIONS_NONPARSABLE_DATES;
    
    
    
    private String outputFolder = DEFAULT_OUTPUT_FOLDER;
    
    private String bkTemplate = null;
    private String seTemplate = null;
    
    private int spredsheetLimit = SPREADSHEET_LIMIT;

    private int updateBatchLimit = UPDATE_BATCH_LIMIT;
    
    /**
     * Vsem stavajicim zaznamum prideli identifiaktor a zaroven vytvori inicalni
     * export
     * 
     * @param logger
     * @param iteration
     * @param results
     */
    public EUIPOInitialExportServiceImpl(String logger, JSONObject iteration, JSONObject results) {
        this.loggerPostfix = logger;
        if (iteration != null) {
            iterationConfig(iteration);
        }
        if (results != null) {
            iterationResults(results);
        }

        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }

    }

    private void iterationResults(JSONObject results) {
        if (results != null) {
            String container = results.optString(FOLDER_KEY);
            if (container != null) {
                this.outputFolder = results.optString(FOLDER_KEY);
            }
            
            if (results.has(MAXROWS_KEY)) {
                this.spredsheetLimit = results.getInt(MAXROWS_KEY);
            }
            
            if (results.has(BATCHROWS_KEY)) {
                this.updateBatchLimit = results.getInt(BATCHROWS_KEY);
            }
            
        }
    }

    public EUIPOInitialExportServiceImpl() {
        //int size = ISO_639_2_BIBLIOGRAPHIC_2_TERMINOLOGY.keySet().size();
    }

    protected void iterationConfig(JSONObject iteration) {
        if (iteration != null) {
            if (iteration.has(STATES_KEY)) {
                this.states = new ArrayList<>();
                JSONArray iterationOverStates = iteration.optJSONArray(STATES_KEY);
                if (iterationOverStates != null) {
                    iterationOverStates.forEach(it -> {
                        states.add(it.toString());
                    });
                }
            }
            
            if (iteration.has(BK_TEMPLATE_KEY)) {
                this.bkTemplate = iteration.getString(BK_TEMPLATE_KEY);
            }
            if (iteration.has(SE_TEMPLATE_KEY)) {
                this.seTemplate = iteration.getString(SE_TEMPLATE_KEY);
            }
            
            if (iteration.has(FILTERS_KEY)) {
                JSONArray filters = iteration.optJSONArray(FILTERS_KEY);
                List<String> listFilters = new ArrayList<>();
                if (filters != null) {
                    filters.forEach(f -> {
                        listFilters.add(f.toString());
                    });
                }
                if (listFilters != null) {
                    this.filters = listFilters;
                }
            }
            if (iteration.has(NONPARSABLE_DATES_KEY)) {
                JSONArray nonParsble = iteration.optJSONArray(NONPARSABLE_DATES_KEY);
                List<String> listExpressions = new ArrayList<>();
                if (listExpressions != null) {
                    nonParsble.forEach(f -> {
                        listExpressions.add(f.toString());
                    });
                }
                if (listExpressions != null) {
                    this.compiledPatterns = listExpressions.stream().map(Pattern::compile).collect(Collectors.toList());
                }
            }
        }
    }

    public List<String> check(String formatFilter) {

        getLogger().info(String.format(" Config for iteration -> iteration states %s; templates %s, %s; filters %s; nonparsable dates %s ", this.states.toString(), this.bkTemplate, this.seTemplate, this.filters, this.compiledPatterns));

        List<String> foundCandidates = new ArrayList<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + FETCH_LIMIT);

        List<String> plusFilter = new ArrayList<>();
        if (!this.states.isEmpty()) {
            
            List<String> publicItemStates = Arrays.stream(PublicItemState.values()).map(PublicItemState::name).collect(Collectors.toList());
           
            String collected = states.stream().map(st-> {
                if (publicItemStates.contains(st)) {
                    return DNTSTAV_FIELD+":"+st;
                } else {
                    return KURATORSTAV_FIELD+":"+st;
                }
            }).collect(Collectors.joining(" OR "));
            
            
            plusFilter.add("(" + collected + ")");
        }

        plusFilter.add(String.format("fmt:%s", formatFilter));

        if (this.filters != null && !this.filters.isEmpty()) {
            plusFilter.addAll(this.filters);
        }
        
        if (this.filters == null || this.filters.isEmpty()) {
            if ("BK".equals(formatFilter)) {
                plusFilter.addAll(DEFAULT_INITIAL_BK_FILTER);
            } else {
                plusFilter.addAll(DEFAULT_INITIAL_SE_FILTER);
            }
        } else {
            if (!this.filters.isEmpty()) {
                plusFilter.addAll(this.filters);
            }
        }

        logger.info("Current iteration filter " + plusFilter);

        try (SolrClient solrClient = buildClient()) {
            support.iterate(
                    solrClient, reqMap, null, plusFilter, Arrays.asList(KURATORSTAV_FIELD + ":X",
                            KURATORSTAV_FIELD + ":D", MarcRecordFields.ID_EUIPO + ":*"),
                    Arrays.asList(IDENTIFIER_FIELD), (rsp) -> {
                        Object identifier = rsp.getFieldValue("identifier");
                        foundCandidates.add(identifier.toString());
                    }, IDENTIFIER_FIELD);
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return foundCandidates;

    }

    @Override
    public void update(String format, String identifier, List<String> docs)
            throws AccountException, IOException, ConflictException, SolrServerException {
        if (!docs.isEmpty()) {

            List<String> toRemove = new ArrayList<>();
            
            try (SolrClient solrClient = buildClient()) {
                getLogger().info("Export identifier '" + identifier + "' number of records "+docs.size());

                int numberOfUpdateBatches = docs.size() / updateBatchLimit;
                if (docs.size() % updateBatchLimit > 0) {
                    numberOfUpdateBatches += 1;
                }
                Map<String, Map<String, List<String>>> recordValues = new HashMap<>();

                for (int i = 0; i < numberOfUpdateBatches; i++) {
                    int startIndex = i * updateBatchLimit;
                    int endIndex = (i + 1) * updateBatchLimit;
                    List<String> subList = docs.subList(startIndex, Math.min(endIndex, docs.size()));

                    ModifiableSolrParams params = new ModifiableSolrParams();
                    params.set("fl",
                            "identifier, raw, date1, date2, date1_int, date2_int, controlfield_008, leader, fmt, type_of_date");

                    SolrDocumentList byids = solrClient.getById(DataCollections.catalog.name(), subList, params);
                    for (int j = 0; j < byids.size(); j++) {
                        SolrDocument doc = byids.get(j);
                        
                        Object date1int = doc.getFieldValue("date1_int");
                        
                        Object ident = doc.getFieldValue("identifier");
                        Object raw = doc.getFieldValue("raw");
                        Object date1 = doc.getFieldValue("date1");
                        Object date2 = doc.getFieldValue("date2");
                        
                        Object leader = doc.getFieldValue(MarcRecordFields.LEADER_FIELD);
                        Object fmt = doc.getFieldValue(MarcRecordFields.FMT_FIELD);
                        Object typeOfDate = doc.getFieldValue(MarcRecordFields.TYPE_OF_DATE);

                        Object controlField008 = doc.getFirstValue("controlfield_008");
                        
                        if (accept(fmt, date1int,  date1)) {
                            
                            JSONObject object = new JSONObject(raw.toString());
                            Map<String, List<String>> oneRecordValues = new HashMap<>();

                            // title
                            oneRecordValues.put("Title", generateTitle(object));

                            // author
                            oneRecordValues.put("AuthorPerfomer", generateAuthor(object));

                            // description
                            List<String> description = generateDescription(object);
                            if (description != null && !description.isEmpty()) {
                                oneRecordValues.put("Description", description);
                            }

                            // publisher
                            List<String> publisher = generatePublisher(object);
                            if (publisher != null && !publisher.isEmpty()) {
                                oneRecordValues.put("Publisher", publisher);
                            }

                            // language
                            List<String> language = generateLanguage(object);
                            if (language != null && !language.isEmpty()) {
                                oneRecordValues.put("Language", language);
                            } else {
                                String field = controlField008.toString();
                                oneRecordValues.put("Language", Arrays.asList(field.substring(35, 38)));
                            }

                            // specific for bk
                            if (fmt.equals("BK")) {
                                // isbn
                                List<String> isbn = generateISBN(object);
                                if (isbn != null && !isbn.isEmpty()) {
                                    String isbnText = isbn.get(0);
                                    if (StringUtils.isAnyString(isbnText)) {
                                        oneRecordValues.put("ISN", isbn);
                                        oneRecordValues.put("ISNType", Arrays.asList("ISBN"));
                                    }
                                }
                                // TODO: changes
                                if ( date2 != null && !date2.toString().trim().startsWith("99") && !date2.toString().trim().contains("--")
                                        && !date2.toString().trim().contains("u") && StringUtils.isAnyString(date2.toString()) && 
                                        !typeOfDate.equals("t") ) {
                                    
                                    
                                    if (isValidDate(date1.toString())) {
                                        oneRecordValues.put("PublisherDate",
                                                Arrays.asList(date1.toString() + "/" + date2.toString()));
                                    }
                                } else {
                                    if (isValidDate(date1.toString())) {
                                        oneRecordValues.put("PublisherDate", Arrays.asList(date1.toString()));
                                    }
                                }

                                oneRecordValues.put("Type", Arrays.asList("INDIVIDUAL"));

                            } else {
                                // specific for SE
                                // issn
                                List<String> isbn = generateISSN(object);
                                if (isbn != null && !isbn.isEmpty()) {
                                    String isbnText = isbn.get(0);
                                    if (StringUtils.isAnyString(isbnText)) {
                                        oneRecordValues.put("ISN", isbn);
                                        oneRecordValues.put("ISNType", Arrays.asList("ISSN"));
                                    }
                                }
                                // type is set
                                oneRecordValues.put("Type", Arrays.asList("SET"));
                            }

                            oneRecordValues.put("euipo", Arrays.asList(MarcRecordUtils.generateEUIPOIdent()));
                            recordValues.put(ident.toString(), oneRecordValues);
                        } else {
                            toRemove.add(ident.toString());
                        }
                    }
                }
                
                // remove skipped monograph
                //toRemove.stream().forEach(docs::remove);
                
                docs.removeAll(toRemove);
                
                int numberOfSpredsheets = docs.size() / this.spredsheetLimit;
                if (docs.size() % this.spredsheetLimit > 0) {
                    numberOfSpredsheets += 1;
                }
                for (int i = 0; i < numberOfSpredsheets; i++) {
                    int startIndex = i * this.spredsheetLimit;
                    int endIndex = (i + 1) * this.spredsheetLimit;
                    List<String> spreadSheetBatch = docs.subList(startIndex, Math.min(endIndex, docs.size()));

                    getLogger().info(String.format("Generating spreadsheet number %d and size is %d", i, spreadSheetBatch.size()));

                    try {
                        File nFile = generateSpreadSheet(format, identifier, i, spreadSheetBatch, recordValues);
                        getLogger().info(String.format("Generated file is %s", nFile.getAbsolutePath()));
                        
                        int numberOfUpdates = spreadSheetBatch.size() / updateBatchLimit;
                        if (spreadSheetBatch.size() % updateBatchLimit > 0) {
                            numberOfUpdates += 1;
                        }
                       
                        // update items from spreadsheet
                        for (int update = 0; update < numberOfUpdates; update++) {
                            int upateStartIndex = update * updateBatchLimit;
                            int endStartIndex = (update + 1) * updateBatchLimit;
                            getLogger().info(String.format("Updating records from spreadsheet  %d and index of update %d", i, update));
                            
                            List<String> updateBatch = spreadSheetBatch.subList(upateStartIndex,
                                    Math.min(endStartIndex, spreadSheetBatch.size()));
 
                            //getLogger().fine(String.format("First 5 identifiers %s", updateBatch.subList(0, 5).toString()));
                            

                            UpdateRequest uReq = new UpdateRequest();
                            for (String id : updateBatch) {
                                SolrInputDocument idoc = new SolrInputDocument();
                                idoc.setField(IDENTIFIER_FIELD, id);

                                String generateEUIPOIdent = recordValues.get(id).get("euipo").get(0);

                                SolrJUtilities.atomicAddDistinct(idoc, generateEUIPOIdent, MarcRecordFields.ID_EUIPO);
                                SolrJUtilities.atomicAddDistinct(idoc, "euipo", MarcRecordFields.EXPORT);
                                SolrJUtilities.atomicAddDistinct(idoc, identifier, MarcRecordFields.ID_EUIPO_EXPORT);

                                uReq.add(idoc);
                            }

                            if (!uReq.getDocuments().isEmpty()) {
                                UpdateResponse response = uReq.process(solrClient, DataCollections.catalog.name());
                                if (response.getStatus() != 200) {
                                }
                            }

                            SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
                        }

                        File finalFile = new File(nFile.getParentFile(), nFile.getName().replace("proc", "xlsx"));
                        
                        com.google.common.io.Files.move(nFile, finalFile);
                        
                        if (finalFile.length() == nFile.length()) {
                            nFile.delete();
                        }

                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (InvalidFormatException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private boolean isValidDate(String string) {
        if (string != null) {
            try {
                int parsed = Integer.parseInt(string);
                return parsed > MIN_YEAR && parsed < MAX_YEAR;
            } catch (NumberFormatException e) {
                getLogger().info(String.format("Date omitted: %s", string));
                // ommit 
            }
        }
        return false;
    }

    private boolean accept(Object fmt, Object date1int, Object date1) {
        if (fmt != null && fmt.toString().equals("BK")) {
            if (date1int == null) {
                String date1str = date1.toString();
                for (Pattern pattern : this.compiledPatterns) {
                    if (pattern.matcher(date1str).matches()) {
                        return true;
                    }
                }
                return false;
            } else {
                Integer compareVal = (Integer) date1int;
                return compareVal <= 2003;
            }
        } else return true;
    }

    private File generateSpreadSheet(String format, String exportid, int batchNumber, List<String> subList,
            Map<String, Map<String, List<String>>> recordValues)
            throws IOException, FileNotFoundException, InvalidFormatException {

        InputStream templateStream = null;
        if (format.equals("BK") && this.bkTemplate != null) {
            templateStream = new FileInputStream(this.bkTemplate);
        } else if (format.equals("SE") && this.seTemplate != null) {
            templateStream = new FileInputStream(this.seTemplate);
            
        } else {
            templateStream = this.getClass().getResourceAsStream("OOC_bulk template_standard import.xlsx");
        }

        File parentFolder = new File(new File(this.outputFolder), exportid);
        parentFolder.mkdirs();

        SimpleDateFormat sdf = new SimpleDateFormat("hhMMss");

        File nFile = new File(parentFolder,
                String.format("%s_%s_%d_%s.proc", format, exportid, batchNumber, sdf.format(new Date())));
        nFile.createNewFile();

        try (XSSFWorkbook workbook = new XSSFWorkbook(templateStream)) {
            XSSFSheet recordSheet = workbook.getSheet("Record");
            XSSFSheet useOfWorkSheet = workbook.getSheet("Use of Work");
            XSSFSheet holdingInsitutionSheet = workbook.getSheet("Holding Institution");

            for (int j = 0; j < subList.size(); j++) {
                String skcIdent = subList.get(j);
                int rowIndex = j + 1;
                // Record sheet
                recordRow(recordValues, recordSheet, skcIdent, rowIndex);
                // Use of work sheet
                userOfWorkRow(recordValues, useOfWorkSheet, skcIdent, rowIndex);
                // holding institution sheet
                holdingInstitutionRow(recordValues, holdingInsitutionSheet, skcIdent, rowIndex);
            }
            // temp // move after submit
            FileOutputStream fos = new FileOutputStream(nFile);
            workbook.write(fos);
            fos.close();
        }
        return nFile;
    }

    private void holdingInstitutionRow(Map<String, Map<String, List<String>>> recordValues,
            XSSFSheet holdingInstitutionSheet, String skcIdent, int rowIndex) {

        XSSFRow newRow = holdingInstitutionSheet.createRow(rowIndex);

        newRow.createCell(SpreadSheetIndexMapper.A).setCellValue(recordValues.get(skcIdent).get("euipo").get(0));
        newRow.createCell(SpreadSheetIndexMapper.B).setCellValue("National Library of the Czech Republic");
        newRow.createCell(SpreadSheetIndexMapper.C).setCellValue("dnnt-podpora@nkp.cz");
        newRow.createCell(SpreadSheetIndexMapper.D).setCellValue("+420221663111");
        newRow.createCell(SpreadSheetIndexMapper.E).setCellValue("https://www.en.nkp.cz/");
        newRow.createCell(SpreadSheetIndexMapper.F).setCellValue("CZ");

    }

    private void userOfWorkRow(Map<String, Map<String, List<String>>> recordValues, XSSFSheet useOfWorkSheet,
            String skcIdent, int rowIndex) {
        XSSFRow newRow = useOfWorkSheet.createRow(rowIndex);

        newRow.createCell(SpreadSheetIndexMapper.A).setCellValue(recordValues.get(skcIdent).get("euipo").get(0));
        newRow.createCell(SpreadSheetIndexMapper.B).setCellValue("LICENCE");
        newRow.createCell(SpreadSheetIndexMapper.D).setCellValue("CZ");

        newRow.createCell(SpreadSheetIndexMapper.E).setCellValue("National Library of the Czech Republic");
        newRow.createCell(SpreadSheetIndexMapper.F).setCellValue("dnnt-podpora@nkp.cz");
        newRow.createCell(SpreadSheetIndexMapper.G).setCellValue("+420221663111");
        newRow.createCell(SpreadSheetIndexMapper.H).setCellValue("https://www.en.nkp.cz/");
        newRow.createCell(SpreadSheetIndexMapper.I).setCellValue("CZ");

        newRow.createCell(SpreadSheetIndexMapper.K)
                .setCellValue("DILIA, divadelní, literární, audiovizuální agentura, z.s.");
        newRow.createCell(SpreadSheetIndexMapper.L).setCellValue("kraupnerova@dilia.cz");
        newRow.createCell(SpreadSheetIndexMapper.M).setCellValue("+420266199813");
        newRow.createCell(SpreadSheetIndexMapper.N).setCellValue("https://www.dilia.eu/");
        newRow.createCell(SpreadSheetIndexMapper.O).setCellValue("CZ");
    }

    // record row
    private void recordRow(Map<String, Map<String, List<String>>> recordValues, XSSFSheet recordSheet, String skcIdent,
            int rowIndex) {
        XSSFRow newRow = recordSheet.createRow(rowIndex);

        newRow.createCell(SpreadSheetIndexMapper.A).setCellValue(recordValues.get(skcIdent).get("Type").get(0));
        newRow.createCell(SpreadSheetIndexMapper.B).setCellValue(recordValues.get(skcIdent).get("euipo").get(0));
        newRow.createCell(SpreadSheetIndexMapper.C).setCellValue("LITERARY");

        String title = recordValues.get(skcIdent).get("Title").stream().collect(Collectors.joining(" "));
        if (title != null && title.trim().endsWith("/")) {
            int slashIndex = title.lastIndexOf('/');
            title = title.substring(0, slashIndex);
        }
        
        if (title != null && title.length() >= MAX_TITLE_LENGTH) {
            Pair<Integer, Integer> pair = maxIndexOfWord(MAX_TITLE_LENGTH - 4, title);
            if (pair != null) {
                title = title.substring(0,pair.getLeft())+" ...";
            } else {
                title = title.substring(0, MAX_TITLE_LENGTH);
            }
            
        }
        
        newRow.createCell(SpreadSheetIndexMapper.E)
                .setCellValue(title);

        // author performer - 5, 6

        List<String> authors = recordValues.get(skcIdent).get("AuthorPerfomer");
        if (authors.size() > 1) {
            getLogger().fine(String.format(" Identifier %s, Authors %s", recordValues.get(skcIdent).get("euipo").get(0), authors.toString()));
        }
        for (int iaddress = 0, idata = 0; idata < authors.size(); iaddress += 2, idata++) {
            int authorPerformerNameIndex = SpreadSheetIndexMapper.F + iaddress;
            
            String author = authors.get(idata).trim();
            if (author.endsWith(",")) {
                author = author.substring(0, author.length() - 1);
            }
            if (author.length() > MAX_AUTHOR_LENGTH) {
                author  = author.substring(0, MAX_AUTHOR_LENGTH-1);
            }
            newRow.createCell(authorPerformerNameIndex).setCellValue(author);
        }

        if (recordValues.get(skcIdent).containsKey("Description")) {
            String desc = recordValues.get(skcIdent).get("Description").get(0);
            if (StringUtils.isAnyString(desc)) {
                newRow.createCell(SpreadSheetIndexMapper.BP).setCellValue(desc);
            }
        }

        if (recordValues.get(skcIdent).containsKey("Publisher")) {
            String publisher = recordValues.get(skcIdent).get("Publisher").stream().collect(Collectors.joining(""));
            if (publisher.trim().endsWith(",")) {
                publisher = publisher.substring(0, publisher.length() - 1);
            }
            newRow.createCell(SpreadSheetIndexMapper.BR).setCellValue(publisher);
        } else {
            newRow.createCell(SpreadSheetIndexMapper.BR).setCellValue("Unknown");
        }

        // Country
        newRow.createCell(SpreadSheetIndexMapper.BS).setCellValue("CZ");

        if (recordValues.get(skcIdent).containsKey("PublisherDate")) {
            newRow.createCell(SpreadSheetIndexMapper.BT)
                    .setCellValue(recordValues.get(skcIdent).get("PublisherDate").get(0));
        }

        // TODO: Delete
        if (recordValues.get(skcIdent).containsKey("Language")) {
            ISO693Converter converter = new ISO693Converter();
            
            List<String> language = recordValues.get(skcIdent).get("Language");
            language = language.stream().map(converter::convertToISO6933).collect(Collectors.toList());
            String joined = language.stream().collect(Collectors.joining(";"));
            newRow.createCell(SpreadSheetIndexMapper.BU).setCellValue(joined);
        }

        if (recordValues.get(skcIdent).containsKey("ISNType")) {
            newRow.createCell(SpreadSheetIndexMapper.BV).setCellValue(recordValues.get(skcIdent).get("ISNType").get(0));
            newRow.createCell(SpreadSheetIndexMapper.BW).setCellValue(recordValues.get(skcIdent).get("ISN").get(0));
        }
    }

    private List<String> generateDescription(JSONObject object) {
        return extractField(object, "505", "a");
    }

    private List<String> generatePublisher(JSONObject object) {
        List<String> gen1 = extractField(object, "260", "b");
        if (gen1 != null && !gen1.isEmpty()) {
            return Arrays.asList(gen1.get(0));
        }
        List<String> gen2 = extractField(object, "264", "b");
        if (gen2 != null && !gen2.isEmpty()) {
            return Arrays.asList(gen2.get(0));
        }

        return new ArrayList<>();
    }

    private List<String> generateAuthor(JSONObject object) {
        List<String> retvals = new ArrayList<>();
        // StringBuilder builder = new StringBuilder();
        List<String> author1 = extractField(object, "100", "a", "b");
        if (author1 != null && !author1.isEmpty())
            retvals.addAll(author1);
        List<String> author2 = extractField(object, "110", "a", "b");
        if (author2 != null && !author2.isEmpty())
            retvals.addAll(author2);
        List<String> author3 = extractField(object, "700", "a", "b");
        if (author3 != null && !author3.isEmpty())
            retvals.addAll(author3);
        return retvals;
    }

    private List<String> generateTitle(JSONObject object) {
        //return extractField(object, "245", "a", "b", "c", "n", "p");
        return extractField(object, "245", "a", "b",  "n", "p");
    }

    private List<String> generateLanguage(JSONObject object) {
        return extractField(object, "041", "a");
    }

    private List<String> generateISBN(JSONObject object) {
        return extractField(object, "020", "a");
    }

    private List<String> generateISSN(JSONObject object) {
        return extractField(object, "022", "a");
    }
    
    public static Pair<Integer,Integer> maxIndexOfWord(int maxCharacters, String input) {
        List<Pair<Integer,Integer>> pairs = new ArrayList<>();
        
        BreakIterator breakIterator = BreakIterator.getWordInstance();
        breakIterator.setText(input);

        // Nalezení začátku prvního slova
        int start = breakIterator.first();

        // Vypsání jednotlivých slov
        while (BreakIterator.DONE != breakIterator.next()) {
            int end = breakIterator.current();
            if (end > maxCharacters) {
                break;
            }
            pairs.add(Pair.of(start,end));

            start = end;
        }
        
        return !pairs.isEmpty() ?  pairs.get(pairs.size() -1) : null;
    }

    private List<String> extractField(JSONObject object, String datafieldKey, String... subfields) {
        Map<String, List<String>> mapping = new HashMap<>();

        if (object.has("dataFields")) {
            // 245a, 245b, 245n, 245p
            JSONObject dataFields = object.getJSONObject("dataFields");
            if (dataFields != null) {
                if (dataFields.has(datafieldKey)) {
                    JSONArray jsonArray = dataFields.getJSONArray(datafieldKey);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject dataField = jsonArray.getJSONObject(i);
                        if (dataField.has("subFields")) {
                            JSONObject subFields = dataField.getJSONObject("subFields");
                            for (String sf : subfields) {
                                if (subFields.has(sf)) {
                                    List<String> sfvals = new ArrayList<>();
                                    JSONArray sfArray = subFields.getJSONArray(sf);
                                    sfArray.forEach(sfObject -> {
                                        String val = ((JSONObject) sfObject).getString("value");
                                        sfvals.add(val);
                                    });

                                    if (mapping.containsKey(datafieldKey + sf)) {
                                        mapping.get(datafieldKey + sf).addAll(sfvals);
                                    } else {
                                        mapping.put(datafieldKey + sf, sfvals);
                                    }
                                    
                                }
                            }
                        }
                    }
                }
            }
        }
        List<String> keys = Arrays.stream(subfields).map(it -> datafieldKey + "" + it).collect(Collectors.toList());
        List<String> vals = new ArrayList<>();
        keys.forEach(key -> {
            if (mapping.containsKey(key)) {
                vals.addAll(mapping.get(key));
            }
        });

        return vals;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    // not used in inital export
    @Override
    public String getLastExportedDate() {
        return null;
    }


    public static void main(String[] args)
            throws AccountException, ConflictException, IOException, SolrServerException, InvalidFormatException {
//        long start = System.currentTimeMillis();
//        EUIPOInitialExportServiceImpl impl = new EUIPOInitialExportServiceImpl();
//        List<String> checkBK = impl.check("BK");
//        //System.out.println(checkBK);
//        impl.update("BK", "inital-bk", checkBK);
//        System.out.println("It took: " + (System.currentTimeMillis() - start) + "ms; Size: " + checkBK.size());
//
//        String title = "Spalovací turbiny, turbodmychadla a ventilátory : přeplňování spalovacích motorů / Jan Macek, Vladimír Kliment";
//        Pair<Integer,Integer> maxIndexOfWord = maxIndexOfWord(49-3, title);
//        System.out.println(maxIndexOfWord);
//        System.out.println(title.substring(0, maxIndexOfWord.getLeft())+"...");
//        System.out.println(title.substring(0, maxIndexOfWord.getLeft()).length());
//        
//        
        
    }
}
