package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.skcAlephStream;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.tuple.Pair;
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
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.index.exceptions.MaximumIterationExceedException;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.impl.AccountServiceImplITTest.BuildSolrClientSupport;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public class SKCTypeServiceTTest {
    
    public static Logger LOGGER = Logger.getLogger(SKCTypeServiceImpl.class.getName());
    
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

    /** Smazano a nema stav
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws MaximumIterationExceedException */
    @Test
    public void testTypeSurvive() throws FactoryConfigurationError, XMLStreamException, SolrServerException, MaximumIterationExceedException, ParserConfigurationException, SAXException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        try {
            alephImport(prepare.getClient(), skcAlephStream("skc/update/oai_skc1.xml"),31, true, true);
            
            Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-001579067", "A", "A", License.dnnto.name(),"poznamka", "test");
            
            SKCTypeServiceImpl skcDeleteService = EasyMock.createMockBuilder(SKCTypeServiceImpl.class)
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
            

            ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
            ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

            AccountServiceImpl aService = EasyMock.createMockBuilder(AccountServiceImpl.class)
                    .withConstructor(appSupport, bservice)
                    .addMockedMethod("buildClient").createMock();
            
            EasyMock.expect(skcDeleteService.buildAccountService()).andReturn(aService).anyTimes();

            EasyMock.expect(aService.buildClient()).andDelegateTo(
                    new AccountServiceImplITTest.BuildSolrClientSupport()
            ).anyTimes();

            
            EasyMock.replay(skcDeleteService,appSupport,bservice,aService /*,oaiCheck*/);
            skcDeleteService.update();
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery catalogQuery = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-001579067\"")
                        .setRows(1000);
                SolrDocumentList catalogDocs = client.query(DataCollections.catalog.name(), catalogQuery).getResults();
                Assert.assertTrue(catalogDocs.size() == 1);
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void testTypeNotSurvive() throws FactoryConfigurationError, XMLStreamException, SolrServerException, MaximumIterationExceedException, ParserConfigurationException, SAXException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        try {
            alephImport(prepare.getClient(), skcAlephStream("skc/update/oai_skc1.xml"),31, true, true);
            
            Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-001579067", "A","A", License.dnnto.name(),"poznamka",  "test");
            
            SKCTypeServiceImpl skcDeleteService = EasyMock.createMockBuilder(SKCTypeServiceImpl.class)
                    .withConstructor("test-logger",new JSONObject())
                    .addMockedMethod("getOptions")
                    .addMockedMethod("buildClient")
                    .addMockedMethod("buildAccountService")
                    //.addMockedMethod("buildCheckOAISKC")
                    .createMock();
            
            
            Options options = EasyMock.createMock(Options.class);
            EasyMock.expect(skcDeleteService.buildClient()).andDelegateTo(
                    new BuildSolrClientSupport()
            ).anyTimes();
            EasyMock.expect(skcDeleteService.getOptions()).andReturn(options).anyTimes();

            

            ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
            ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

            AccountServiceImpl aService = EasyMock.createMockBuilder(AccountServiceImpl.class)
                    .withConstructor(appSupport, bservice)
                    .addMockedMethod("buildClient").createMock();
            

            EasyMock.expect(aService.buildClient()).andDelegateTo(
                    new AccountServiceImplITTest.BuildSolrClientSupport()
                ).anyTimes();

            EasyMock.expect(skcDeleteService.buildAccountService()).andReturn(aService).anyTimes();

            EasyMock.replay(skcDeleteService,appSupport,bservice,aService);
            skcDeleteService.update();
            
            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery catalogQuery = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-001579067\"")
                        .setRows(1000);
                SolrDocumentList catalogDocs = client.query(DataCollections.catalog.name(), catalogQuery).getResults();
                Assert.assertTrue(catalogDocs.size() == 1);
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
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

    
    protected class BuildSolrClientSupport extends SKCTypeServiceImpl{

        public BuildSolrClientSupport() {
            super(null, null);
        }
        @Override
        public SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    
    }

    
    
}
