package cz.inovatika.sdnnt.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.exceptions.MaximumIterationExceedException;
import cz.inovatika.sdnnt.index.utils.HarvestUtils;
import cz.inovatika.sdnnt.index.utils.OAIXMLHeadersReader;
import cz.inovatika.sdnnt.index.utils.OAIXMLRecordsReader;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class OAICheckSKC {
    
    public static Logger LOGGER  = Logger.getLogger(OAICheckSKC.class.getName());
    
    public static final int DEFAULT_CONNECT_TIMEOUT = 5;
    public static final String CONNECTION_TIMEOUT_KEY = "connectionTimeout";
    public static final String CONNECTION_REQUEST_TIMEOUT_KEY = "connectionRequestTimeout";
    public static final String SOCKET_TIMEOUT_KEY = "socketTimeout";
    public static final String START_URL ="https://aleph.nkp.cz/OAI?verb=ListIdentifiers&metadataPrefix=marc21&set=SKC-DNNT";
    
    private Logger logger = LOGGER;
    
    
    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Pair<Set<String>, List<String>> iterate() throws IOException, MaximumIterationExceedException, XMLStreamException, ParserConfigurationException, SAXException {
        Set<String> records = new HashSet<>();
        List<String> deleted = new ArrayList<>();
        
        int recCounter = 0;
        int delCounter = 0;
        int indexCount = 0;
        try(CloseableHttpClient client = buildOAIClient()) {
            String resumptionToken = firstCheck(client);
            while(resumptionToken != null) {
                String url = "http://aleph.nkp.cz/OAI?verb=ListIdentifiers&resumptionToken=" + resumptionToken;
                File dFile = HarvestUtils.throttle(client, "skc_dnt", url);
                InputStream dStream = new FileInputStream(dFile);
                OAIXMLHeadersReader xmlReader = new OAIXMLHeadersReader(dStream);
                resumptionToken = xmlReader.readFromXML();
                records.addAll(xmlReader.getRecords());
                records.addAll(xmlReader.getToDelete());
                indexCount++;
                if (indexCount % 100 == 0) {
                    logger.info(String.format("Iteration %d,  Number of records %d", indexCount, records.size()));
                }
                deletePaths(dFile);
            }
        }
        logger.info(String.format("Iteration finished. Records: %d", records.size()));
        return Pair.of(records, deleted);
    }
    
    private void deletePaths(File dFile) {
        try {
            Files.delete(dFile.toPath());
            Files.delete(dFile.getParentFile().toPath());
          } catch (IOException e) {
            LOGGER.warning("Exception during deleting file");
          }
    }

    
    private String firstCheck(CloseableHttpClient client) throws IOException, MaximumIterationExceedException, XMLStreamException {
        File dFile = HarvestUtils.throttle(client, "skc_dnt", START_URL);
        InputStream dStream = new FileInputStream(dFile);
        OAIXMLRecordsReader xmlReader = new OAIXMLRecordsReader(dStream);
        String resToken = xmlReader.readFromXML();
        LOGGER.info("Records "+xmlReader.getRecords().size());
        LOGGER.info("Deleted records "+xmlReader.getToDelete().size());
        return resToken;
    }

    private CloseableHttpClient buildOAIClient() {
        JSONObject harvest = Options.getInstance().getJSONObject("OAIHavest");
        int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        if (harvest.has(CONNECTION_TIMEOUT_KEY)) {
            connectTimeout = harvest.getInt(CONNECTION_TIMEOUT_KEY);
        }

        int connectionRequestTimeout = DEFAULT_CONNECT_TIMEOUT;
        if (harvest.has(CONNECTION_REQUEST_TIMEOUT_KEY)) {
            connectionRequestTimeout = harvest.getInt(CONNECTION_REQUEST_TIMEOUT_KEY);
        }

        int socketTimeout = DEFAULT_CONNECT_TIMEOUT;
        if (harvest.has(SOCKET_TIMEOUT_KEY)) {
            socketTimeout = harvest.getInt(SOCKET_TIMEOUT_KEY);
        }


        int ct = connectTimeout * 1000;
        int crt = connectionRequestTimeout * 1000;
        int st = socketTimeout * 1000;
        LOGGER.info(String.format("Creating client with (connectionTimeout=%d, connectionRequestTimeout=%d, socketTimeout=%d", ct, crt, st));
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(ct)
            .setConnectionRequestTimeout(crt)
            .setSocketTimeout(st).build();

        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

}
