package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.skcAlephStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

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

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserController;

public class OAIHarvestDeleteServiceTTest {

    public static final Logger LOGGER = Logger.getLogger(OAIHarvestDeleteServiceTTest.class.getName());

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("oaiharvest");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("catalog");
        prepare.deleteCores("history");
        prepare.deleteCores("zadost");
    }

    /** Smazano a nema stav*/
    @Test
    public void testDeleteHasNoState() throws FactoryConfigurationError, XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        try {
            alephImport(prepare.getClient(), skcAlephStream("skc/update/oai_skc1.xml"),31, true, true);
            //Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-001579067", "A", License.dnnto.name(),"poznamka", new JSONArray(), "test");
            SKCDeleteServiceImpl skcDeleteService = EasyMock.createMockBuilder(SKCDeleteServiceImpl.class)
                    .withConstructor("test-logger",new JSONObject())
                    .addMockedMethod("getOptions")
                    .addMockedMethod("buildClient")
                    .addMockedMethod("buildAccountService")
                    .createMock();

            Options options = EasyMock.createMock(Options.class);
            EasyMock.expect(skcDeleteService.buildClient()).andDelegateTo(
                    new BuildSolrClientSupport()
            ).anyTimes();
            EasyMock.expect(skcDeleteService.getOptions()).andReturn(options).anyTimes();


            User user = testUser();
            UserController controler  = EasyMock.createMock(UserController.class);
            ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
            ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

            AccountServiceImpl aService = EasyMock.createMockBuilder(AccountServiceImpl.class)
                    .withConstructor(appSupport, bservice)
                    .addMockedMethod("buildClient").createMock();

            EasyMock.expect(appSupport.getUser()).andReturn(user).anyTimes();

            EasyMock.expect(aService.buildClient()).andDelegateTo(
                    new AccountServiceImplITTest.BuildSolrClientSupport()
            ).anyTimes();
            
            EasyMock.expect(skcDeleteService.buildAccountService()).andReturn(aService).anyTimes();
            EasyMock.replay(skcDeleteService, options, controler, appSupport, bservice, aService);

            alephImport(prepare.getClient(), skcAlephStream("skc/delete/oai_skc1_deleted.xml"),30, true, true, skcDeleteService);
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery catalogQuery = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-001579067\"")
                        .setRows(1000);
                SolrDocumentList catalogDocs = client.query(DataCollections.catalog.name(), catalogQuery).getResults();
                Assert.assertTrue(catalogDocs.size() == 0);
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    
    
    /** Smazano a nema stav*/
    @Test
    public void testDeleteHasStateAndNotFound() throws FactoryConfigurationError, XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        try {
            alephImport(prepare.getClient(), skcAlephStream("skc/update/oai_skc1.xml"),31, true, true);
            Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-001579067", "A", License.dnnto.name(),"poznamka", new JSONArray(), "test");
            SKCDeleteServiceImpl skcDeleteService = EasyMock.createMockBuilder(SKCDeleteServiceImpl.class)
                    .withConstructor("test-logger",new JSONObject())
                    .addMockedMethod("getOptions")
                    .addMockedMethod("buildClient")
                    .addMockedMethod("buildAccountService")
                    .createMock();

            Options options = EasyMock.createMock(Options.class);
            EasyMock.expect(skcDeleteService.buildClient()).andDelegateTo(
                    new BuildSolrClientSupport()
            ).anyTimes();
            EasyMock.expect(skcDeleteService.getOptions()).andReturn(options).anyTimes();


            User user = testUser();
            UserController controler  = EasyMock.createMock(UserController.class);
            ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
            ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

            AccountServiceImpl aService = EasyMock.createMockBuilder(AccountServiceImpl.class)
                    .withConstructor(appSupport, bservice)
                    .addMockedMethod("buildClient").createMock();

            EasyMock.expect(appSupport.getUser()).andReturn(user).anyTimes();

            EasyMock.expect(aService.buildClient()).andDelegateTo(
                    new AccountServiceImplITTest.BuildSolrClientSupport()
            ).anyTimes();
            
            EasyMock.expect(skcDeleteService.buildAccountService()).andReturn(aService).anyTimes();
            EasyMock.replay(skcDeleteService, options, controler, appSupport, bservice, aService);
            // vede na zadost
            alephImport(prepare.getClient(), skcAlephStream("skc/delete/oai_skc1_deleted.xml"),30, true, true, skcDeleteService);
            // zadost vytvorena, nemela by se objevit
            alephImport(prepare.getClient(), skcAlephStream("skc/delete/oai_skc1_deleted.xml"),30, true, true, skcDeleteService);

            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery catalogQuery = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-001579067\"")
                        .setRows(1000);
                SolrDocumentList catalogDocs = client.query(DataCollections.catalog.name(), catalogQuery).getResults();
                Assert.assertTrue(catalogDocs.size() == 1);
                
                MarcRecord fDoc = MarcRecord.fromDocDep(catalogDocs.get(0));

                Assert.assertNotNull(fDoc.dntstav);
                Assert.assertTrue(fDoc.dntstav.equals(Arrays.asList("A")));

                Assert.assertNotNull(fDoc.kuratorstav);
                Assert.assertTrue(fDoc.kuratorstav.equals(Arrays.asList("DX")));
                
                Assert.assertNotNull(fDoc.license);
                Assert.assertTrue(fDoc.license.equals("dnnto"));

                SolrQuery zadostQuery = new SolrQuery("*")
                        .setRows(1000);
                SolrDocumentList results = client.query(DataCollections.zadost.name(), zadostQuery).getResults();
                Assert.assertTrue(results.getNumFound() == 1);
                
                
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    
    
    /** Smazano a nema stav*/
    @Test
    public void testDeleteHasStateAndSameCCNB() throws FactoryConfigurationError, XMLStreamException, SolrServerException {
    }
    
    protected User testUser() {
        User user = new User();
        user.setInstitution("NKP");
        user.setUsername( "pokusny");
        user.setJmeno( "Jmeno" );
        user.setPrijmeni( "Prijmeni");
        return user;
    }

    
    static class BuildSolrClientSupport extends SKCDeleteServiceImpl {

        public BuildSolrClientSupport() {
            super(null, new JSONObject());
        }

        @Override
        public SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
        
    }

}
