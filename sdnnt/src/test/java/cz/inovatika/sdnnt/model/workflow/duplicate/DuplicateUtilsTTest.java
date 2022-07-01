package cz.inovatika.sdnnt.model.workflow.duplicate;

import static cz.inovatika.sdnnt.index.DntAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.DntAlephTestUtils.dntAlephStream;
import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.skcAlephStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.DntAlephImporterITTest;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.utils.SimplePOST;
import cz.inovatika.sdnnt.utils.XMLUtils;

public class DuplicateUtilsTTest {

    public static final Logger LOGGER = Logger.getLogger(DntAlephImporterITTest.class.getName());

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("dntaleph");
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
    public void testFindDuplicateDNT() throws XMLStreamException, SolrServerException, ParserConfigurationException, SAXException, IOException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        
        InputStream dnt = DuplicateUtilsTTest.class.getResourceAsStream("solrdnt.xml");
        InputStream skc = DuplicateUtilsTTest.class.getResourceAsStream("solrskc.xml");
        
        try {
            
            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update";
            SimplePOST.post(solrHosts,IOUtils.toString(dnt, "UTF-8"));
            SimplePOST.post(solrHosts,IOUtils.toString(skc, "UTF-8"));
            SimplePOST.post(solrHosts,"<commit/>");
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*").setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 2);

                SolrQuery id = new SolrQuery("identifier:\"oai:aleph-nkp.cz:DNT01-000102092\"").setRows(1);
                docs = client.query(DataCollections.catalog.name(), query).getResults();
                    
                MarcRecord fromDoc = MarcRecord.fromDocDep(docs.get(0));
                Assert.assertNotNull(fromDoc);
                
                Pair<Case,List<String>> findDNTFollowers = DuplicateDNTUtils.findDNTFollowers(client, fromDoc);
                Assert.assertTrue(findDNTFollowers.getRight() != null );
                Assert.assertTrue(findDNTFollowers.getLeft().equals(Case.DNT_1));
                
            }
        } catch (IOException e) {
              Assert.fail(e.getMessage());
        }
    }

    /** Test nalezeni dnt 
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException */
    @Test
    public void testFindDuplicateSKC_ActiveCCNB() throws XMLStreamException, SolrServerException, ParserConfigurationException, SAXException, IOException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        
        //InputStream dnt = DuplicateUtilsTTest.class.getResourceAsStream("oaidnt.xml");
        InputStream skc = DuplicateUtilsTTest.class.getResourceAsStream("solrskc.xml");
        InputStream skcActiveccnb = DuplicateUtilsTTest.class.getResourceAsStream("solrskc-active-ccnb.xml");
        
        try {

            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update";
            SimplePOST.post(solrHosts,IOUtils.toString(skc, "UTF-8"));
            SimplePOST.post(solrHosts,IOUtils.toString(skcActiveccnb, "UTF-8"));

            SimplePOST.post(solrHosts,"<commit/>");
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*").setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 2);

                SolrQuery id = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000995692\"").setRows(1);
                docs = client.query(DataCollections.catalog.name(), query).getResults();
                    
                MarcRecord fromDoc = MarcRecord.fromDocDep(docs.get(0));
                Assert.assertNotNull(fromDoc);
                Assert.assertTrue(fromDoc.identifier.equals("oai:aleph-nkp.cz:SKC01-000995692"));
             
                Pair<Case,List<String>> findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, fromDoc);
                Assert.assertTrue(findSKCFollowers.getKey().equals(Case.SKC_1));
                Assert.assertTrue(findSKCFollowers.getValue().size() == 1);
                Assert.assertTrue(findSKCFollowers.getValue().get(0).equals("oai:aleph-nkp.cz:SKC01-000999999"));
            }
        } catch (IOException e) {
              Assert.fail(e.getMessage());
        }
    }

    /** Test nalezeni dnt 
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException */
    @Test
    public void testFindDuplicateSKC_CanceledCCNB() throws XMLStreamException, SolrServerException, ParserConfigurationException, SAXException, IOException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        
        InputStream skc = DuplicateUtilsTTest.class.getResourceAsStream("solrskc.xml");
        InputStream skcActiveccnb = DuplicateUtilsTTest.class.getResourceAsStream("solrskc-canceled-ccnb.xml");
        
        try {

            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update";
            SimplePOST.post(solrHosts,IOUtils.toString(skc, "UTF-8"));
            SimplePOST.post(solrHosts,IOUtils.toString(skcActiveccnb, "UTF-8"));

            SimplePOST.post(solrHosts,"<commit/>");
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*").setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 2);

                SolrQuery id = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000995692\"").setRows(1);
                docs = client.query(DataCollections.catalog.name(), query).getResults();
                    
                MarcRecord fromDoc = MarcRecord.fromDocDep(docs.get(0));
                Assert.assertNotNull(fromDoc);
                Assert.assertTrue(fromDoc.identifier.equals("oai:aleph-nkp.cz:SKC01-000995692"));
                
                Pair<Case,List<String>> findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, fromDoc);
                System.out.println(findSKCFollowers);
                Assert.assertTrue(findSKCFollowers.getValue().size() == 1);
                Assert.assertTrue(findSKCFollowers.getValue().get(0).equals("oai:aleph-nkp.cz:SKC01-00099999c"));
                Assert.assertTrue(findSKCFollowers.getKey().equals(Case.SKC_2a));

            }
        } catch (IOException e) {
              Assert.fail(e.getMessage());
        }
    }

    
    /**
     * Test 910a
     */
    // DELETE
    @Test
    public void testFindDuplicateSKC_910() throws XMLStreamException, SolrServerException, ParserConfigurationException, SAXException, IOException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        InputStream skc = DuplicateUtilsTTest.class.getResourceAsStream("solrskc.xml");
        InputStream skcActiveccnb = DuplicateUtilsTTest.class.getResourceAsStream("solrskc-910.xml");
        try {

            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update";
            SimplePOST.post(solrHosts,IOUtils.toString(skc, "UTF-8"));
            SimplePOST.post(solrHosts,IOUtils.toString(skcActiveccnb, "UTF-8"));

            SimplePOST.post(solrHosts,"<commit/>");
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*").setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 2);

                SolrQuery id = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000995692\"").setRows(1);
                docs = client.query(DataCollections.catalog.name(), query).getResults();
                    
                MarcRecord fromDoc = MarcRecord.fromDocDep(docs.get(0));
                Assert.assertNotNull(fromDoc);
                Assert.assertTrue(fromDoc.identifier.equals("oai:aleph-nkp.cz:SKC01-000995692"));
                
                Pair<Case,List<String>> findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, fromDoc);
                Assert.assertTrue(findSKCFollowers.getValue().size() == 1);
                Assert.assertTrue(findSKCFollowers.getValue().get(0).equals("oai:aleph-nkp.cz:SKC01-00099910a"));
                System.out.println(findSKCFollowers.getKey());
                //Assert.assertTrue(findSKCFollowers.getKey().equals(Case.SKC_2a));

            }
        } catch (IOException e) {
              Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFindDuplicateSKC_910_2() throws XMLStreamException, SolrServerException, ParserConfigurationException, SAXException, IOException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        InputStream skc = DuplicateUtilsTTest.class.getResourceAsStream("solrskc_910_2_orig_2.xml");
        InputStream follower = DuplicateUtilsTTest.class.getResourceAsStream("solrskc-910_2_follower.xml");

        alephImport(prepare.getClient(), follower,29, true, true);
        String string = IOUtils.toString(skc, "UTF-8").trim();
        try {

            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update";
            SimplePOST.post(solrHosts,string);

            SimplePOST.post(solrHosts,"<commit/>");
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*").setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 30);

                SolrQuery id = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-008768253\"").setRows(1);
                docs = client.query(DataCollections.catalog.name(), id).getResults();
                
                MarcRecord fromDoc = MarcRecord.fromDocDep(docs.get(0));
                Assert.assertNotNull(fromDoc);
                Assert.assertTrue(fromDoc.identifier.equals("oai:aleph-nkp.cz:SKC01-008768253"));
                
                Pair<Case,List<String>> findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, fromDoc);
                Assert.assertTrue(findSKCFollowers.getValue().size() == 1);
                Assert.assertTrue(findSKCFollowers.getValue().get(0).equals("oai:aleph-nkp.cz:SKC01-000890002"));

            }
        } catch (IOException e) {
              Assert.fail(e.getMessage());
        }
    }
}
