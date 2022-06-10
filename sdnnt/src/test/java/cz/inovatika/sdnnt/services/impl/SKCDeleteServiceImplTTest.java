package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateUtilsTTest;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.impl.DNTSKCPairServiceImplTTest.BuildSolrClientSupport;
import cz.inovatika.sdnnt.utils.SimplePOST;

public class SKCDeleteServiceImplTTest {
    
    public static final Logger LOGGER = Logger.getLogger(SKCDeleteServiceImplTTest.class.getName());
    
    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("zadost");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("zadost","catalog");
    }
    
    /** Test delete */
    @Test
    public void testDeleteNecessary() throws IOException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        InputStream skc = DuplicateUtilsTTest.class.getResourceAsStream("solrskc.xml");
        try {
            String solrHosts = SolrTestServer.TEST_URL;
            solrHosts = solrHosts + (solrHosts.endsWith("/") ? "" : "/") + DataCollections.catalog.name()+"/update";
            SimplePOST.post(solrHosts,IOUtils.toString(skc, "UTF-8"));
            SimplePOST.post(solrHosts,"<commit/>");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }

        String jonbConfig="{\"results\":{\"SKC_1\":\"state\"}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);
        
        SKCDeleteServiceImpl skcServie = EasyMock.createMockBuilder(SKCDeleteServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("results"),Arrays.asList("oai:aleph-nkp.cz:SKC01-000995692"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();
        
        EasyMock.expect(skcServie.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();
        
        EasyMock.replay(skcServie);
        skcServie.update();
    
        
        try (SolrClient client = SolrTestServer.getClient()) {
            
            SolrQuery query = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000995692\"")
                    .setRows(1)
                    .setStart(0);

            SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 0);
        }
    }
    

    /** Test update state */
    @Test
    public void testUpdateState() throws IOException, SolrServerException {
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

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }

        String jonbConfig="{\"results\":{\"SKC_1\":\"state\"}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);
        
        SKCDeleteServiceImpl skcServie = EasyMock.createMockBuilder(SKCDeleteServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("results"),Arrays.asList("oai:aleph-nkp.cz:DNT01-000102092"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();
        
        EasyMock.expect(skcServie.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();
        
        EasyMock.replay(skcServie);
        skcServie.update();
    
        
        try (SolrClient client = SolrTestServer.getClient()) {

            SolrQuery query = new SolrQuery("identifier:\"oai:aleph-nkp.cz:DNT01-000102092\"")
                    .setRows(1)
                    .setStart(0);

            SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
            
            MarcRecord dntdoc = MarcRecord.fromSolrDoc(docs.get(0));
            Assert.assertTrue(dntdoc.followers != null && dntdoc.followers.size() == 1);
            Assert.assertTrue(dntdoc.dntstav != null && dntdoc.dntstav.get(0).equals("D"));
            Assert.assertTrue(dntdoc.kuratorstav != null && dntdoc.kuratorstav.get(0).equals("D"));
            
            query = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000995692\"")
                    .setRows(1)
                    .setStart(0);

            docs = client.query(DataCollections.catalog.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
            MarcRecord skcdoc = MarcRecord.fromSolrDoc(docs.get(0));
            Assert.assertTrue(skcdoc.followers == null || skcdoc.followers.size() == 0);
            Assert.assertTrue(skcdoc.dntstav.get(0).equals("A"));
        }
    }

    
    

    /** Test update state */
    @Test
    public void testUpdateRequest() throws IOException, SolrServerException {
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

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }


        String jonbConfig="{\"results\":{\"SKC_1\":\"request\"}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        User user = testUser();

        ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl aService = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(appSupport, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appSupport.getUser()).andReturn(user).anyTimes();

        SKCDeleteServiceImpl skcService = EasyMock.createMockBuilder(SKCDeleteServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("results"),Arrays.asList("oai:aleph-nkp.cz:DNT01-000102092"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .addMockedMethod("buildAccountService")
                .createMock();
        
        EasyMock.expect(skcService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();
        
        EasyMock.expect(aService.buildClient()).andDelegateTo(
                new AccountServiceImplITTest.BuildSolrClientSupport()
        ).anyTimes();

        
        EasyMock.expect(skcService.buildAccountService()).andReturn(aService).anyTimes();

        EasyMock.replay(skcService,appSupport,bservice,aService);
        skcService.update();
    
        
        try (SolrClient client = SolrTestServer.getClient()) {
            SolrQuery query = new SolrQuery("identifier:\"oai:aleph-nkp.cz:DNT01-000102092\"")
                    .setRows(2)
                    .setStart(0);

            SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
            
            Assert.assertTrue(docs.get(0).getFieldValues("kuratorstav") != null && docs.get(0).getFieldValues("kuratorstav").size() ==1);
            Assert.assertTrue(docs.get(0).getFieldValues("kuratorstav").toString().equals("[DX]"));

            Assert.assertTrue(docs.get(0).getFieldValues("followers") != null && docs.get(0).getFieldValues("followers").size() ==1);
            Assert.assertTrue(docs.get(0).getFieldValues("followers").toString().equals("[oai:aleph-nkp.cz:SKC01-000995692]"));
            
            
            query = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000995692\"")
                    .setRows(2)
                    .setStart(0);

            docs = client.query(DataCollections.catalog.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
            Assert.assertTrue(docs.get(0).getFieldValues("kuratorstav") == null);

            query = new SolrQuery("*")
                    .setRows(2)
                    .setStart(0);

            docs = client.query(DataCollections.zadost.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
        }
    }

    
    protected User testUser() {
        User user = new User();
        user.setInstitution("NKP");
        user.setUsername( "dntskcpair");
        user.setJmeno( "dntskcpair" );
        user.setPrijmeni( "Prijmeni");
        return user;
    }

    private static class BuildSolrClientSupport extends SKCDeleteServiceImpl {

        public BuildSolrClientSupport() {
            super(null, new JSONObject(), new ArrayList<>());
            // TODO Auto-generated constructor stub
        }

        @Override
        public SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }
}
