package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.easymock.EasyMock;
import org.json.JSONArray;
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
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateUtilsTTest;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.utils.SimplePOST;

public class DNTSKCPairServiceImplTTest {

    public static final Logger LOGGER = Logger.getLogger(DNTSKCPairServiceImplTTest.class.getName());

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

    /** Test update stavu */
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

        //String jonbConfig="{\"results\":{\"state\":true}}";
        String jonbConfig="{\"results\":{}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);
        
        AbstractCheckDeleteService dntSkcService = EasyMock.createMockBuilder(DNTSKCPairServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();
        
        EasyMock.expect(dntSkcService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();

        EasyMock.replay(dntSkcService);
        dntSkcService.update();
        
        try (SolrClient client = SolrTestServer.getClient()) {
            // vyrazeny zaznam
            SolrQuery query = new SolrQuery("identifier:\"oai:aleph-nkp.cz:DNT01-000102092\"")
                    .setRows(1)
                    .setStart(0);
            SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
            MarcRecord origin = MarcRecord.fromDocDep(docs.get(0));
            Assert.assertTrue(origin.dntstav != null);
            Assert.assertTrue(origin.kuratorstav != null);
            Assert.assertTrue(origin.dntstav.get(0).equals("D"));
            Assert.assertTrue(origin.kuratorstav.get(0).equals("D"));
            Assert.assertTrue(origin.kuratorstav.get(0).equals("D"));
            Assert.assertTrue(origin.followers != null);
            Assert.assertTrue(origin.followers.equals(Arrays.asList("oai:aleph-nkp.cz:SKC01-000995692")));
            

            // naslednicky zaznam
            query = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-000995692\"")
                    .setRows(1)
                    .setStart(0);
            docs = client.query(DataCollections.catalog.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
            MarcRecord follower = MarcRecord.fromDocDep(docs.get(0));
            Assert.assertTrue(follower.dntstav != null);
            Assert.assertTrue(follower.kuratorstav != null);
            Assert.assertTrue(follower.dntstav.get(0).equals("A"));
            Assert.assertTrue(follower.kuratorstav.get(0).equals("A"));
            Assert.assertTrue(follower.license.equals("dnnto"));
        }
    }

    
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

        String jonbConfig="{\"results\":{\"DNT_1\":\"request\"}}}";

        JSONObject jobJSONObject = new JSONObject(jonbConfig);
        
        AbstractCheckDeleteService dntSkcService = EasyMock.createMockBuilder(DNTSKCPairServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .addMockedMethod("buildAccountService")
                .createMock();
        
        User user = testUser();

        ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl aService = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(appSupport, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appSupport.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(dntSkcService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();

        EasyMock.expect(aService.buildClient()).andDelegateTo(
                new AccountServiceImplITTest.BuildSolrClientSupport()
        ).anyTimes();

        EasyMock.expect(dntSkcService.buildAccountService()).andReturn(aService).anyTimes();

        EasyMock.replay(dntSkcService,appSupport,bservice,aService);
        dntSkcService.update();
        
        JSONObject results = aService.search("*", null, null, null, null, null, null, null, 0, 1);
        JSONArray jArray = results.getJSONObject("response").getJSONArray("docs");
        Zadost zadost = Zadost.fromJSON(jArray.getJSONObject(0).toString());
        Assert.assertNotNull(zadost);
        Assert.assertTrue(zadost.getNavrh() != null);
        Assert.assertTrue(zadost.getNavrh().equals("DXN"));

        String desiredItemState = zadost.getDesiredItemState();
        Assert.assertTrue(desiredItemState != null);
        Assert.assertTrue(desiredItemState.equals("D"));
    }
    
    
    protected User testUser() {
        User user = new User();
        user.setInstitution("NKP");
        user.setUsername( "dntskcpair");
        user.setJmeno( "dntskcpair" );
        user.setPrijmeni( "Prijmeni");
        return user;
    }

    
    protected class BuildSolrClientSupport extends DNTSKCPairServiceImpl {

        public BuildSolrClientSupport() {
            super(null, null);
        }

        @Override
        protected SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }

}
