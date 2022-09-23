package cz.inovatika.sdnnt.model.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateDNTUtils;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateUtilsTTest;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.AccountServiceImplITTest.BuildSolrClientSupport;
import cz.inovatika.sdnnt.utils.SimplePOST;

public class Production1TT {
    
    public static Logger LOGGER = Logger.getLogger(Production1TT.class.getName());
    
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
    public void testDocA_DNNT() throws XMLStreamException, SolrServerException, ParserConfigurationException, SAXException, IOException, ConflictException, AccountException {

        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        InputStream doc = Production1TT.class.getResourceAsStream("prod/004292483_PA_dnntt.json");
        InputStream zadost = Production1TT.class.getResourceAsStream("prod/zadost.json");
        try {
            
            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update/json/docs?commit=true";
            SimplePOST.post(solrHosts,IOUtils.toString(doc, "UTF-8"));

            solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.zadost.name()+"/update/json/docs?commit=true";
            SimplePOST.post(solrHosts,IOUtils.toString(zadost, "UTF-8"));

            User user = testUser();
            UserController controler  = EasyMock.createMock(UserController.class);
            ApplicationUserLoginSupport appLogin = EasyMock.createMock(ApplicationUserLoginSupport.class);
            ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

            AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                    .withConstructor( appLogin, bservice)
                    .addMockedMethod("buildClient").createMock();

            EasyMock.expect(appLogin.getUser()).andReturn(user).anyTimes();
            EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
            ).anyTimes();

            EasyMock.replay(controler, service ,bservice, appLogin);

            service.schedulerSwitchStates("22859a86-ce88-4961-8f10-1d4439a983c8");

            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*").setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 1);

                SolrQuery id = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-004292483\"").setRows(1);
                docs = client.query(DataCollections.catalog.name(), query).getResults();
                    
                MarcRecord fromDoc = MarcRecord.fromDocDep(docs.get(0));
//                Assert.assertNotNull(fromDoc);
//                Assert.assertNotNull(fromDoc.dntstav);
//                Assert.assertFalse(fromDoc.dntstav.isEmpty());
//                String dntstav = fromDoc.dntstav.get(0);
//                Assert.assertTrue(dntstav.equals("PA"));
            }
            
        } catch (IOException e) {
              Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testDocPA() throws XMLStreamException, SolrServerException, ParserConfigurationException, SAXException, IOException, ConflictException, AccountException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        InputStream doc = Production1TT.class.getResourceAsStream("prod/004292483.json");
        InputStream zadost = Production1TT.class.getResourceAsStream("prod/zadost.json");
        try {
            
            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update/json/docs?commit=true";
            SimplePOST.post(solrHosts,IOUtils.toString(doc, "UTF-8"));

            solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.zadost.name()+"/update/json/docs?commit=true";
            SimplePOST.post(solrHosts,IOUtils.toString(zadost, "UTF-8"));

            User user = testUser();
            UserController controler  = EasyMock.createMock(UserController.class);
            ApplicationUserLoginSupport appLogin = EasyMock.createMock(ApplicationUserLoginSupport.class);
            ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

            AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                    .withConstructor( appLogin, bservice)
                    .addMockedMethod("buildClient").createMock();

            EasyMock.expect(appLogin.getUser()).andReturn(user).anyTimes();
            EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
            ).anyTimes();

            EasyMock.replay(controler, service ,bservice, appLogin);

            service.schedulerSwitchStates("22859a86-ce88-4961-8f10-1d4439a983c8");
            

            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*").setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 1);

                SolrQuery id = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-004292483\"").setRows(1);
                docs = client.query(DataCollections.catalog.name(), query).getResults();
                    
                MarcRecord fromDoc = MarcRecord.fromDocDep(docs.get(0));
                Assert.assertNotNull(fromDoc);
                
                Assert.assertNotNull(fromDoc.dntstav);
                Assert.assertFalse(fromDoc.dntstav.isEmpty());
                String dntstav = fromDoc.dntstav.get(0);
                Assert.assertTrue(dntstav.equals("PA"));
            }
            
        } catch (IOException e) {
              Assert.fail(e.getMessage());
        }
    }
    
    private User testUser() {
        User user = new User();
        user.setInstitution("NKP");
        user.setUsername( "pokusny");
        user.setJmeno( "Jmeno" );
        user.setPrijmeni( "Prijmeni");
        return user;
    }


}
