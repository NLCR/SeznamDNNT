package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.services.EUIPOCancelService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.utils.ISO693Converter;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class EUIPOCancelServiceImpl extends AbstractEUIPOService implements EUIPOCancelService {

    //20220713
    public static final SimpleDateFormat HISTORY_DATE_INPUTFORMAT = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat HISTORY_DATE_OUTOUTFORMAT = new SimpleDateFormat("yyyy/MM/dd");
    
    public static final List<String> DEFAULT_STATES = Arrays.asList("N", "X", "D");
    private Logger logger = Logger.getLogger(EUIPOCancelServiceImpl.class.getName());

    protected String loggerName;

    
    public EUIPOCancelServiceImpl(String logger, JSONObject iteration, JSONObject results) {
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
    
    public EUIPOCancelServiceImpl() {
        this.states = DEFAULT_STATES;
    }
    
    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public List<String> check(String formatFilter) {
        getLogger().info(String.format(" Config for iteration -> iteration states %s; templates %s, %s; filters %s; nonparsable dates %s ", this.states.toString(), this.bkTemplate, this.seTemplate, this.filters, this.compiledPatterns));

        List<String> foundCandidates = new ArrayList<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + AbstractEUIPOService.FETCH_LIMIT);

        List<String> plusFilter = new ArrayList<>();
        plusFilter.add("id_euipo:* AND export:euipo");
        
        if (!this.states.isEmpty()) {

            String collected = states.stream().map(st-> {
                return DNTSTAV_FIELD+":"+st;
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
                    solrClient, reqMap, null, plusFilter, Arrays.asList(DNTSTAV_FIELD + ":A",
                            DNTSTAV_FIELD + ":NL", DNTSTAV_FIELD + ":PA", KURATORSTAV_FIELD + ":NPA"),
                    Arrays.asList(IDENTIFIER_FIELD), (rsp) -> {
                        Object identifier = rsp.getFieldValue("identifier");
                        foundCandidates.add(identifier.toString());
                    }, IDENTIFIER_FIELD);
            
            
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, e.getMessage(), e);
        }
        
        // nesmi byt v zadnem dalsim otevrenem exportu typu  
        return foundCandidates;
    }

    
    private boolean accept(Object dntstav, Object history) {
        if (dntstav.toString().equals(PublicItemState.D.name())) {
            List<String> schedulerComments = new ArrayList<>();

            JSONArray jsonArray = new JSONArray(history.toString());
            if (jsonArray.length() > 0) {
                //List<String> comments = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject lastObject = jsonArray.getJSONObject(i);
                    String comment = lastObject.optString("comment");
                    if (comment.startsWith("scheduler/"+Case.SKC_4a.name()) || comment.startsWith("scheduler/"+Case.SKC_4b.name())) {
                        schedulerComments.add(comment);
                    }
                }
                if (schedulerComments.size() > 0) {
                    String lastComment = schedulerComments.get(schedulerComments.size() - 1);
                    
                    if (lastComment.startsWith("scheduler/"+Case.SKC_4a.name()) || lastComment.startsWith("scheduler/"+Case.SKC_4b.name())) {
                        return true;
                    }

                }
            } 
            return false;
        } else return true;
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
                            "identifier, raw, date1, date2, date1_int, date2_int, controlfield_008, leader, fmt, type_of_date, historie_stavu, dntstav, id_euipo, id_euipo_export, id_euipo_export_canceled, ");

                    SolrDocumentList byids = solrClient.getById(DataCollections.catalog.name(), subList, params);
                    for (int j = 0; j < byids.size(); j++) {
                        SolrDocument doc = byids.get(j);
                        
                        Object date1int = doc.getFieldValue("date1_int");
                        Object ident = doc.getFieldValue("identifier");
                        Object euipo = doc.getFieldValue(MarcRecordFields.ID_EUIPO);
                        
                        Object export = doc.getFieldValue(MarcRecordFields.ID_EUIPO_EXPORT);

                        Object historieStavu = doc.getFirstValue(MarcRecordFields.HISTORIE_STAVU_FIELD);
                        Object dntStav = doc.getFirstValue(MarcRecordFields.DNTSTAV_FIELD);
                        
                        
                        Object raw = doc.getFieldValue("raw");
                        Object date1 = doc.getFieldValue("date1");
                        Object date2 = doc.getFieldValue("date2");
                        
                        Object leader = doc.getFieldValue(MarcRecordFields.LEADER_FIELD);
                        Object fmt = doc.getFieldValue(MarcRecordFields.FMT_FIELD);
                        Object typeOfDate = doc.getFieldValue(MarcRecordFields.TYPE_OF_DATE);

                        Object controlField008 = doc.getFirstValue("controlfield_008");

                        Object dntstav = doc.getFirstValue(MarcRecordFields.DNTSTAV_FIELD);


                        if (accept(dntStav, historieStavu)) {
                            
                            Map<String,List<String>> oneRecordValues = prepareDataRecordForExcel(raw, date1, date2, fmt, typeOfDate, controlField008);
                            
                            List<String> euipoids = new ArrayList<>();
                            ((Collection)euipo).forEach(id-> { euipoids.add(id.toString()); });


                            List<String> exports = new ArrayList<>();
                            ((Collection)export).forEach(id-> { exports.add(id.toString()); });

                            oneRecordValues.put("euipo", euipoids);
                            oneRecordValues.put("euipo_export", exports);
                            oneRecordValues.put("dntstav", Arrays.asList(dntstav.toString()));
                            oneRecordValues.put("identifier", Arrays.asList(ident.toString()));
                            oneRecordValues.put("historie_stavu", Arrays.asList(historieStavu.toString()));
                            
                            List<String> ccnb = generatecCCNB(new JSONObject(raw.toString()));
                            if (ccnb != null && !ccnb.isEmpty()) {
                                oneRecordValues.put("ccnb", ccnb);
                                
                            }

                            //marc_015a
                            
 
                            recordValues.put(ident.toString(), oneRecordValues);
                        } else {
                            toRemove.add(ident.toString());
                        }
                    }
                }
                
                
                docs.removeAll(toRemove);

                int numberOfExports = numberOfExports(docs);
                for (int export = 0; export < numberOfExports; export++) {
                    
                    List<String> exportPids = subList(docs, export, this.exportLimit);
                    SimpleDateFormat nameformat = new SimpleDateFormat("yyyy_MMMMM_dd_hh_mm_ss.SSS");
                    String exportName = String.format("euipo_uocp_%s_%s",format, nameformat.format(new Date()));
                    getLogger().info(String.format("Creating  export %s ", exportName));
                    this.createExport(exportName, exportPids.size());

                    int numberOfSpredsheets = numberOfSpreadsheets(exportPids);

                    for (int i = 0; i < numberOfSpredsheets; i++) {
                        int startIndex = i * this.spredsheetLimit;
                        int endIndex = (i + 1) * this.spredsheetLimit;
                        List<String> spreadSheetBatch = exportPids.subList(startIndex, Math.min(endIndex, exportPids.size()));

                        getLogger().info(String.format("Generating spreadsheet number %d and size is %d", i, spreadSheetBatch.size()));

                        try {
                            File nFile = generateSpreadSheet(format, exportName, i, spreadSheetBatch, recordValues);
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
     
                                UpdateRequest uReq = new UpdateRequest();
                                for (String id : updateBatch) {
                                    SolrInputDocument idoc = new SolrInputDocument();
                                    idoc.setField(IDENTIFIER_FIELD, id);
                                    
                                    // zrusim euipo identfikator, facet ze byl exportovan
                                    SolrJUtilities.atomicSetNull(idoc,  MarcRecordFields.ID_EUIPO);
                                    /** Must be set manually */
                                    // SolrJUtilities.atomicSetNull(idoc,  MarcRecordFields.EXPORT);

                                    SolrJUtilities.atomicAddDistinct(idoc, exportName, MarcRecordFields.ID_EUIPO_EXPORT);
                                    SolrJUtilities.atomicSet(idoc, exportName, MarcRecordFields.ID_EUIPO_EXPORT_ACTIVE);
                                    
                                    // nastavim id_euipo zrusene exporty 
                                    List<String> euipo = recordValues.get(id).get("euipo");
                                    SolrJUtilities.atomicAddDistinct(idoc, euipo, MarcRecordFields.ID_EUIPO_CANCELED);
                                    SolrJUtilities.atomicSet(idoc, euipo, MarcRecordFields.ID_EUIPO_LASTACTIVE);



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
        return retVal.get();
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
            doc.setField("export_type", "UOCP");

            UpdateRequest uReq = new UpdateRequest();
            uReq.add(doc);
            UpdateResponse response = uReq.process(solrClient, DataCollections.exports.name());
            if (response.getStatus() != 200) {
            }
            SolrJUtilities.quietCommit(solrClient, DataCollections.exports.name());
        }        
        
    }

    @Override
    public String getLastExportedDate() {
        // TODO Auto-generated method stub
        return null;
    }
    
    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }
    
    protected Options getOptions() {
        return Options.getInstance();
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
            templateStream = this.getClass().getResourceAsStream("EUIPO_seznam_zmen_empty.xlsx");
        }

        File parentFolder = new File(new File(this.outputFolder), exportid);
        parentFolder.mkdirs();

        SimpleDateFormat sdf = new SimpleDateFormat("hhMMss");

        File nFile = new File(parentFolder,
                String.format("%s_%s_%d_%s.proc", format, exportid, batchNumber, sdf.format(new Date())));
        nFile.createNewFile();

        try (XSSFWorkbook workbook = new XSSFWorkbook(templateStream)) {
            XSSFSheet recordSheet = workbook.getSheetAt(0);
            for (int j = 0; j < subList.size(); j++) {
                String skcIdent = subList.get(j);
                int rowIndex = j + 1;
                recordRows(recordValues, recordSheet, skcIdent, rowIndex);
            }
            FileOutputStream fos = new FileOutputStream(nFile);
            workbook.write(fos);
            fos.close();
        }
        return nFile;
    }

    
    // record row
    private void recordRows(Map<String, Map<String, List<String>>> recordValues, XSSFSheet recordSheet, String skcIdent,
            int rowIndex) {

        XSSFRow newRow = recordSheet.createRow(rowIndex);

        newRow.createCell(SpreadSheetIndexMapper.A).setCellValue(recordValues.get(skcIdent).get("dntstav").get(0));
        newRow.createCell(SpreadSheetIndexMapper.B).setCellValue(recordValues.get(skcIdent).get("euipo").stream().collect(Collectors.joining(", ")));
        newRow.createCell(SpreadSheetIndexMapper.C).setCellValue(recordValues.get(skcIdent).get("identifier").get(0));

        if (recordValues.get(skcIdent).containsKey("ccnb")) {
            newRow.createCell(SpreadSheetIndexMapper.D).setCellValue(recordValues.get(skcIdent).get("ccnb").get(0));
        }
        if (recordValues.get(skcIdent).containsKey("ISNType")) {
            newRow.createCell(SpreadSheetIndexMapper.E).setCellValue(recordValues.get(skcIdent).get("ISN").get(0));
        }

        if (recordValues.get(skcIdent).containsKey("AuthorPerfomer")) {
            List<String> authors = recordValues.get(skcIdent).get("AuthorPerfomer");
            newRow.createCell(SpreadSheetIndexMapper.F).setCellValue(authors.stream().collect(Collectors.joining(", ")));
        }

        
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
        
        newRow.createCell(SpreadSheetIndexMapper.G)
                .setCellValue(title);

        
        if (recordValues.get(skcIdent).containsKey("Publisher")) {
            String publisher = recordValues.get(skcIdent).get("Publisher").stream().collect(Collectors.joining(""));
            if (publisher.trim().endsWith(",")) {
                publisher = publisher.substring(0, publisher.length() - 1);
            }
            newRow.createCell(SpreadSheetIndexMapper.H).setCellValue(publisher);
        }

        if (recordValues.get(skcIdent).containsKey("PublisherDate")) {
            newRow.createCell(SpreadSheetIndexMapper.I)
                    .setCellValue(recordValues.get(skcIdent).get("PublisherDate").get(0));
        }

        if (recordValues.get(skcIdent).containsKey("historie_stavu")) {
            String history = recordValues.get(skcIdent).get("historie_stavu").get(0);

            JSONArray historyJSONArray = new JSONArray(history);
            if (historyJSONArray.length() > 0) {
                JSONObject last = historyJSONArray.getJSONObject(historyJSONArray.length()-1);
                String dateString = last.optString("date");
                if (dateString != null) {
                    try {
                        Date parse = HISTORY_DATE_INPUTFORMAT.parse(dateString);
                        newRow.createCell(SpreadSheetIndexMapper.J)
                            .setCellValue(HISTORY_DATE_OUTOUTFORMAT.format(parse));
                    } catch (Exception e) {
                        getLogger().warning("Cannot parse date "+dateString);
                    }

                    
                }
                
                String zadostString = last.optString("zadost");
                if (zadostString != null) {
                    newRow.createCell(SpreadSheetIndexMapper.K)
                        .setCellValue(zadostString);
                }

                String commentString = last.optString("comment");
                if (commentString != null && commentString.startsWith("scheduler/")) {
                    newRow.createCell(SpreadSheetIndexMapper.L)
                        .setCellValue(commentString);
                }
            }
        }
    }

    
}
