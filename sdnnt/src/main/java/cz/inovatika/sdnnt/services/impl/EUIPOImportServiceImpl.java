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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.services.EUIPOImportService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.services.utils.ISO693Converter;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class EUIPOImportServiceImpl extends AbstractEUIPOService implements EUIPOImportService {
    
    private Logger logger = Logger.getLogger(EUIPOImportService.class.getName());

    public static final List<String> DEFAULT_STATES = Arrays.asList("A", "PA", "NL", "NPA");

    protected String loggerName;

    /**
     * Vsem stavajicim zaznamum prideli identifiaktor a zaroven vytvori inicalni
     * export
     * 
     * @param logger
     * @param iteration
     * @param results
     */
    public EUIPOImportServiceImpl(String logger, JSONObject iteration, JSONObject results) {
        this.states = DEFAULT_STATES;
        
        this.loggerName = logger;
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

//    public void iterationResults(JSONObject results) {
//        if (results != null) {
//            String container = results.optString(AbstractEUIPOService.FOLDER_KEY);
//            if (container != null) {
//                this.outputFolder = results.optString(AbstractEUIPOService.FOLDER_KEY);
//            }
//            
//            if (results.has(AbstractEUIPOService.MAXROWS_KEY)) {
//                this.spredsheetLimit = results.getInt(AbstractEUIPOService.MAXROWS_KEY);
//            }
//            
//            if (results.has(AbstractEUIPOService.BATCHROWS_KEY)) {
//                this.updateBatchLimit = results.getInt(AbstractEUIPOService.BATCHROWS_KEY);
//            }
//            
//        }
//    }
//
//
//    public void iterationConfig(JSONObject iteration) {
//        if (iteration != null) {
//            if (iteration.has(AbstractEUIPOService.STATES_KEY)) {
//                this.states = new ArrayList<>();
//                JSONArray iterationOverStates = iteration.optJSONArray(AbstractEUIPOService.STATES_KEY);
//                if (iterationOverStates != null) {
//                    iterationOverStates.forEach(it -> {
//                        states.add(it.toString());
//                    });
//                }
//            }
//            
//            if (iteration.has(AbstractEUIPOService.BK_TEMPLATE_KEY)) {
//                this.bkTemplate = iteration.getString(AbstractEUIPOService.BK_TEMPLATE_KEY);
//            }
//            if (iteration.has(AbstractEUIPOService.SE_TEMPLATE_KEY)) {
//                this.seTemplate = iteration.getString(AbstractEUIPOService.SE_TEMPLATE_KEY);
//            }
//            
//            if (iteration.has(AbstractEUIPOService.FILTERS_KEY)) {
//                JSONArray filters = iteration.optJSONArray(AbstractEUIPOService.FILTERS_KEY);
//                List<String> listFilters = new ArrayList<>();
//                if (filters != null) {
//                    filters.forEach(f -> {
//                        listFilters.add(f.toString());
//                    });
//                }
//                if (listFilters != null) {
//                    this.filters = listFilters;
//                }
//            }
//            if (iteration.has(AbstractEUIPOService.NONPARSABLE_DATES_KEY)) {
//                JSONArray nonParsble = iteration.optJSONArray(AbstractEUIPOService.NONPARSABLE_DATES_KEY);
//                List<String> listExpressions = new ArrayList<>();
//                if (listExpressions != null) {
//                    nonParsble.forEach(f -> {
//                        listExpressions.add(f.toString());
//                    });
//                }
//                if (listExpressions != null) {
//                    this.compiledPatterns = listExpressions.stream().map(Pattern::compile).collect(Collectors.toList());
//                }
//            }
//        }
//    }

    public List<String> check(String formatFilter) {

        getLogger().info(String.format(" Config for iteration -> iteration states %s; templates %s, %s; filters %s; nonparsable dates %s ", this.states.toString(), this.bkTemplate, this.seTemplate, this.filters, this.compiledPatterns));

        List<String> foundCandidates = new ArrayList<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + AbstractEUIPOService.FETCH_LIMIT);

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
        // tady filtr pro format 
        plusFilter.add(String.format("fmt:%s", formatFilter));

        if (this.filters != null && !this.filters.isEmpty()) {
            plusFilter.addAll(this.filters);
        }
        
        if (this.filters == null || this.filters.isEmpty()) {
            if ("BK".equals(formatFilter)) {
                plusFilter.addAll(AbstractEUIPOService.DEFAULT_INITIAL_BK_FILTER);
            } else {
                plusFilter.addAll(AbstractEUIPOService.DEFAULT_INITIAL_SE_FILTER);
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
        
        
//        List<List<String>> retvals = new ArrayList<>();
//        
//        int itemsInBatch = DEFAULT_MAX_EXPORT_ITEMS;
//        int numberOfBatches =  foundCandidates.size() / itemsInBatch;
//        numberOfBatches += foundCandidates.size() % itemsInBatch == 0 ? 0 : 1;
//        for (int i = 0; i < numberOfBatches; i++) {
//            int start = i*itemsInBatch;
//            int stop = Math.min((i+1)*itemsInBatch, foundCandidates.size());
//            retvals.add(foundCandidates.subList(start, stop));
//
//        }
//        
//        return retvals;
        
        return foundCandidates;
    }

    
    
    @Override
    public void createExport(String exportIdentifier, int numberOfDocs)
            throws AccountException, IOException, ConflictException, SolrServerException {
        try (SolrClient solrClient = buildClient()) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("id", exportIdentifier);
            doc.setField("export_date", new Date());
            doc.setField("export_folder", new File(new File(this.outputFolder), exportIdentifier).getAbsolutePath());
            doc.setField("export_num_docs", numberOfDocs);
            doc.setField("export_type", "IOCP");

            UpdateRequest uReq = new UpdateRequest();
            uReq.add(doc);
            UpdateResponse response = uReq.process(solrClient, DataCollections.exports.name());
            if (response.getStatus() != 200) {
            }
            SolrJUtilities.quietCommit(solrClient, DataCollections.exports.name());
        }        
    }

    @Override
    public int update(String format,  List<String> docs)
            throws AccountException, IOException, ConflictException, SolrServerException {
        
        AtomicInteger retVal = new AtomicInteger();
        if (!docs.isEmpty()) {

            List<String> toRemove = new ArrayList<>();
            
            try (SolrClient solrClient = buildClient()) {

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
                            "identifier, raw, date1, date2, date1_int, date2_int, controlfield_008, leader, fmt, type_of_date, id_euipo_export");

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
                            
                            Map<String, List<String>> oneRecordValues = prepareDataRecordForExcel(raw, date1, date2, fmt,
                                    typeOfDate, controlField008);

                            
                            recordValues.put(ident.toString(), oneRecordValues);
                        } else {
                            toRemove.add(ident.toString());
                        }
                    }
                }
                
                long start = System.currentTimeMillis();
                docs.removeAll(toRemove);
                //getLogger().info("Removing all items took "+(System.currentTimeMillis() - start)+" ms ");
                
                int numberOfExports = numberOfExports(docs);
                for (int export = 0; export < numberOfExports; export++) {
                    
                    List<String> exportPids = subList(docs, export, this.exportLimit);
                    SimpleDateFormat nameformat = new SimpleDateFormat("yyyy_MMMMM_dd_hh_mm_ss.SSS");
                    String exportName = String.format("euipo_iocp_%s_%s",format, nameformat.format(new Date()));
                    getLogger().info(String.format("Creating  export %s ", exportName));
                    this.createExport(exportName, exportPids.size());
                    
                    int numberOfSpredsheets = numberOfSpreadsheets(exportPids);
                    for (int i = 0; i < numberOfSpredsheets; i++) {

                        List<String> spreadSheetBatch =  subList(exportPids, i, spredsheetLimit);

                        getLogger().info(String.format("Generating spreadsheet number %d and size is %d", i, spreadSheetBatch.size()));

                        try {
                            File nFile = generateSpreadSheet(format, exportName, i, spreadSheetBatch, recordValues);
                            getLogger().info(String.format("Generated file is %s", nFile.getAbsolutePath()));
                            
                            int numberOfUpdates = numberOfUpdates(spreadSheetBatch);
                           
                            // update items from spreadsheet
                            for (int update = 0; update < numberOfUpdates; update++) {
                                int upateStartIndex = update * updateBatchLimit;
                                int endStartIndex = (update + 1) * updateBatchLimit;

                                getLogger().info(String.format("Updating records from spreadsheet  %d and index of update %d (%d-%d)", i, update, upateStartIndex, endStartIndex));
                                
                                List<String> updateBatch =  subList(spreadSheetBatch, update, updateBatchLimit);
     
                                UpdateRequest uReq = new UpdateRequest();
                                for (String id : updateBatch) {
                                    SolrInputDocument idoc = new SolrInputDocument();
                                    idoc.setField(IDENTIFIER_FIELD, id);

                                    String generateEUIPOIdent = recordValues.get(id).get("euipo").get(0);

                                    SolrJUtilities.atomicAddDistinct(idoc, generateEUIPOIdent, MarcRecordFields.ID_EUIPO);
                                    /** Must be set manually */
                                    //SolrJUtilities.atomicAddDistinct(idoc, "euipo", MarcRecordFields.EXPORT);
                                    
                                    SolrJUtilities.atomicAddDistinct(idoc, exportName, MarcRecordFields.ID_EUIPO_EXPORT);
                                    SolrJUtilities.atomicSet(idoc, exportName, MarcRecordFields.ID_EUIPO_EXPORT_ACTIVE);

                                    uReq.add(idoc);
                                }

                                if (!uReq.getDocuments().isEmpty()) {
                                    UpdateResponse response = uReq.process(solrClient, DataCollections.catalog.name());
                                    if (response.getStatus() != 200) {

                                        retVal.addAndGet(updateBatch.size());

                                        getLogger().info(String.format("Updated retval  %d", retVal.get()));

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
        
        //
        return retVal.get();
    }

    private boolean accept(Object fmt, Object date1int, Object date1) {
        if (fmt != null && fmt.toString().equals("BK")) {
            // nonparsable date
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
                return compareVal <= this.maxAcceptingBKYear;
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
        
        if (title != null && title.length() >= AbstractEUIPOService.MAX_TITLE_LENGTH) {
            Pair<Integer, Integer> pair = maxIndexOfWord(AbstractEUIPOService.MAX_TITLE_LENGTH - 4, title);
            if (pair != null) {
                title = title.substring(0,pair.getLeft())+" ...";
            } else {
                title = title.substring(0, AbstractEUIPOService.MAX_TITLE_LENGTH);
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
            if (author.length() > AbstractEUIPOService.MAX_AUTHOR_LENGTH) {
                author  = author.substring(0, AbstractEUIPOService.MAX_AUTHOR_LENGTH-1);
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


}
