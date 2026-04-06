package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.utils.imports.ImporterUtils;
import cz.inovatika.sdnnt.services.CompareService;
import cz.inovatika.sdnnt.services.compare.DifferencesResult;
import cz.inovatika.sdnnt.services.compare.RecordFingerprint;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONArray;
import org.slf4j.LoggerFactory;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

public class CompareServiceImpl implements CompareService {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CompareServiceImpl.class);
    private Logger logger = Logger.getLogger(CompareServiceImpl.class.getName());

    public static final int LIMIT = 1000;

    private List<Pair<String,String>> differences = null;
    private Set<String> ignoredGranularity;
    private Set<String> ignoredMasterlinks;

    private String comparingSolr;

    public CompareServiceImpl(String logger, String comparingSolr, Set<String> ignoredGranularity, Set<String> ignoredMasterlinks) {
        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }
        this.comparingSolr = comparingSolr;
        this.ignoredGranularity = ignoredGranularity;
        this.ignoredMasterlinks = ignoredMasterlinks;
        getLogger().log(Level.INFO,"Ignored granularity fields "+this.ignoredGranularity);
        getLogger().log(Level.INFO,"Ignored master links fields "+this.ignoredMasterlinks);
    }

    public CompareServiceImpl(String logger, String comparingSolr, JSONArray ignoredGranularity, JSONArray ignoredMasterlinks) {
        this(logger, comparingSolr, setOfStrings(ignoredGranularity), setOfStrings(ignoredMasterlinks));
    }

    private static Set<String> setOfStrings(JSONArray input) {
        Set<String> outputSet = new HashSet<>();
        input.forEach(granularity->{outputSet.add(granularity.toString());});
        return outputSet;
    }

    public Logger getLogger() {
        return logger;
    }

    public DifferencesResult check() {
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + LIMIT);
        List<String> plusFilter = new ArrayList<>(Arrays.asList(
                "kuratorstav:*",
                "setSpec:SKC"
        ));

        Map<String, RecordFingerprint> processingFingerPrints = new HashMap<>();
        Map<String, Pair<RecordFingerprint, RecordFingerprint>> differences = new HashMap<>();
        Map<String, RecordFingerprint> missing = new HashMap<>();

        final AtomicInteger sourceCounter = new AtomicInteger(0);
        final AtomicInteger diffCounter = new AtomicInteger(0);

        logger.info("Iterating source solr "+getOptions().getString("solr.host")+"; Current iteration filter " + plusFilter);
        try (final SolrClient solrClient = buildSourceClient()) {
            support.iterate(solrClient, reqMap, null, plusFilter, new ArrayList<>(), Arrays.asList(
                    "*"
            ), (rsp) -> {

                Object identifier = rsp.getFieldValue(IDENTIFIER_FIELD);
                RecordFingerprint recordFingerprint = RecordFingerprint.loadFromSolrDocument(getLogger(), rsp, this.ignoredGranularity, this.ignoredMasterlinks);
                processingFingerPrints.put(identifier.toString(), recordFingerprint);
                sourceCounter.incrementAndGet();
                if (sourceCounter.get() % 1000 == 0) {
                    getLogger().log(Level.INFO, "Iterated so far "+sourceCounter.get());
                }

            }, IDENTIFIER_FIELD);
        } catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }


        logger.info("Iterating comparing solr "+this.comparingSolr +"; Current iteration filter " + plusFilter);
        try (final SolrClient solrClient = buildCompareClient(comparingSolr)) {
            support.iterate(solrClient, reqMap, null, plusFilter, new ArrayList<>(), Arrays.asList(
                    "*"
            ), (rsp) -> {
                Object identifier = rsp.getFieldValue(IDENTIFIER_FIELD);
                RecordFingerprint recordFingerprint = RecordFingerprint.loadFromSolrDocument(getLogger(),rsp, this.ignoredGranularity, this.ignoredMasterlinks);
                RecordFingerprint comparingFingerPrint = processingFingerPrints.get(identifier.toString());
                if (comparingFingerPrint == null) {
                    missing.put(identifier.toString(), recordFingerprint);
                } else {
                    if (!recordFingerprint.equals(comparingFingerPrint)) {
                        boolean eq = recordFingerprint.equals(comparingFingerPrint);
                        differences.put(identifier.toString(), Pair.of(comparingFingerPrint, recordFingerprint));
                    }
                }

                diffCounter.incrementAndGet();
                if (diffCounter.get() % 1000 == 0) {
                    getLogger().log(Level.INFO, "Iterated so far "+diffCounter.get()+"("+sourceCounter.get()+"), differences "+differences.size()+ " missing "+missing.size());
                }

            }, IDENTIFIER_FIELD);
        } catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return new DifferencesResult(differences, missing);
    }

    protected SolrClient buildCompareClient(String host) {
        return new HttpSolrClient.Builder(host).build();
    }

    protected SolrClient buildSourceClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    protected Options getOptions() {
        return Options.getInstance();
    }



}
