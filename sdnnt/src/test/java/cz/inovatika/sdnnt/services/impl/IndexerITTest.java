package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.index.DntAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.DntAlephTestUtils.dntAlephStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.utils.SimplePOST;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class IndexerITTest {
    
    public static final Logger LOGGER = Logger.getLogger(IndexerITTest.class.getName());

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("catalog");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("catalog");
    }

    @Test
    public void testChangeKuratorState() throws JsonProcessingException, FactoryConfigurationError, XMLStreamException, IOException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        InputStream resourceAsStream = dntAlephStream("oai_SE_dnnt.xml");
        Assert.assertNotNull(resourceAsStream);
        alephImport(resourceAsStream,36);
        SolrJUtilities.quietCommit(prepare.getClient(), "catalog");
        
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("identifier:\"oai:aleph-nkp.cz:DNT01-000172209\"");
        QueryResponse catalog = prepare.getClient().query("catalog", solrQuery);
        long numFound = catalog.getResults().getNumFound();
        Assert.assertTrue(numFound == 1);
        // i granularita musi mit N 
        Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:DNT01-000172209", "N", License.dnnto.name(),"poznamka", new JSONArray(), "test");
        SolrJUtilities.quietCommit(prepare.getClient(), "catalog");

        solrQuery = new SolrQuery();
        solrQuery.setQuery("identifier:\"oai:aleph-nkp.cz:DNT01-000172209\"");
        catalog = prepare.getClient().query("catalog", solrQuery);
        Assert.assertTrue(catalog.getResults().getNumFound() == 1);
        
        MarcRecord changedMarcRercord = MarcRecord.fromIndex(prepare.getClient(), "oai:aleph-nkp.cz:DNT01-000172209");
        Assert.assertTrue(changedMarcRercord.dntstav != null);
        Assert.assertTrue(changedMarcRercord.dntstav.size() == 1);
        Assert.assertTrue(changedMarcRercord.dntstav.get(0).equals("N"));
        Assert.assertTrue(changedMarcRercord.license == null);
        Assert.assertTrue(changedMarcRercord.historie_kurator_stavu != null);
        Assert.assertTrue(changedMarcRercord.historie_kurator_stavu.length() == 3);

        JSONObject historieStavuJSON = changedMarcRercord.historie_stavu.getJSONObject(2);
        Assert.assertTrue(historieStavuJSON.has("stav"));
        Assert.assertTrue(historieStavuJSON.optString("stav") != null);
        Assert.assertTrue(historieStavuJSON.optString("stav").equals("N"));
        Assert.assertFalse(historieStavuJSON.has("license"));

        JSONObject historieKuratorStavuJSON = changedMarcRercord.historie_kurator_stavu.getJSONObject(2);
        Assert.assertTrue(historieKuratorStavuJSON.has("stav"));
        Assert.assertTrue(historieKuratorStavuJSON.optString("stav") != null);
        Assert.assertTrue(historieKuratorStavuJSON.optString("stav").equals("N"));
        Assert.assertFalse(historieKuratorStavuJSON.has("license"));

        Assert.assertTrue(changedMarcRercord.kuratorstav != null);
        Assert.assertTrue(changedMarcRercord.kuratorstav.size() == 1);
        Assert.assertTrue(changedMarcRercord.kuratorstav.get(0).equals("N"));
        
//        JSONArray granularity = changedMarcRercord.granularity;
//        for (int i = 0; i < granularity.length(); i++) {
//            JSONObject jsonObj = granularity.getJSONObject(i);
//            System.out.println(jsonObj.getString("state"));
//        }
        
    }

    @Test
    public void testChangeKuratorState_Granularity() throws JsonProcessingException, FactoryConfigurationError, XMLStreamException, IOException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        
        //SolrTestServer.TEST_URL;
        InputStream resourceAsStream = IndexerITTest.class.getResourceAsStream("oai_aleph-nkp.cz_SKC01-000083605.json");
        Assert.assertNotNull(resourceAsStream);
        String json = IOUtils.toString(resourceAsStream, "UTF-8");
        Assert.assertNotNull(json);
        
        String url = SolrTestServer.TEST_URL+"catalog/update/json/docs?commit=true";
        SimplePOST.postJSON(url, json);
        
        
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000083605\"");
        QueryResponse catalog = prepare.getClient().query("catalog", solrQuery);
        long numFound = catalog.getResults().getNumFound();
        Assert.assertTrue(numFound == 1);

        Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-000083605", "N", null,"poznamka", new JSONArray(), "test");
        SolrJUtilities.quietCommit(prepare.getClient(), "catalog");

        
        
    }
}
