package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class PXYearServiceTest {

    public static final Logger LOGGER = Logger.getLogger(PXServiceImplTest.class.getName());

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

    @Test
    public void testCheck() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException, AccountException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiers();

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 1976]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXYearServiceImpl pxService = EasyMock.createMockBuilder(PXYearServiceImpl.class)
                .withConstructor(jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();

        Options options = EasyMock.createMock(Options.class);

        EasyMock.expect(pxService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();
        EasyMock.expect(pxService.getOptions()).andReturn(options).anyTimes();

        InputStream resStream = this.getClass().getResourceAsStream("pxservice_kramerius.json");
        String s = IOUtils.toString(resStream, "UTF-8");

        EasyMock.replay(pxService, options);

        List<String> check = pxService.check();
        Assert.assertTrue(check.size() == 2);
        Assert.assertTrue(check.contains("oai:aleph-nkp.cz:DNT01-000008874"));
        Assert.assertTrue(check.contains("oai:aleph-nkp.cz:DNT01-000008884"));
    }



    @Test
    public void testUpdate() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException, AccountException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiers();

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 2010]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXYearServiceImpl pxService = EasyMock.createMockBuilder(PXYearServiceImpl.class)
                .withConstructor(jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();


        Options options = EasyMock.createMock(Options.class);

        EasyMock.expect(pxService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();
        EasyMock.expect(pxService.getOptions()).andReturn(options).anyTimes();

        EasyMock.replay(pxService, options);

        pxService.update(CatalogSupport.A_IDENTIFIERS);

        final SolrClient client = SolrTestServer.getClient();
        CatalogSupport.A_IDENTIFIERS.stream().forEach(identifier-> {

            SolrQuery query = new SolrQuery("identifier:\"" + identifier + "\"")
                    .setRows(1)
                    .setStart(0)
                    .setFields(String.format("%s, %s", MarcRecordFields.FLAG_PUBLIC_IN_DL, MarcRecordFields.KURATORSTAV_FIELD));
            try {
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                Assert.assertTrue(docs.getNumFound() == 1);
                SolrDocument document = docs.get(0);
                Object cStav = document.getFieldValue(MarcRecordFields.KURATORSTAV_FIELD);
                Assert.assertNotNull(cStav);
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Test
    public void testRequest() throws AccountException, ConflictException, SolrServerException, IOException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiers();

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 1900]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        AbstractPXService pxService = EasyMock.createMockBuilder(PXYearServiceImpl.class)
                .withConstructor(jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .addMockedMethod("buildAccountService")
                .createMock();

        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);
        ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl aService = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(appSupport, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appSupport.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(aService.buildClient()).andDelegateTo(
                new AccountServiceImplTest.BuildSolrClientSupport()
        ).anyTimes();


        Options options = EasyMock.createMock(Options.class);

        EasyMock.expect(pxService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();

        EasyMock.expect(pxService.getOptions()).andReturn(options).anyTimes();
        EasyMock.expect(pxService.buildAccountService()).andReturn(aService).anyTimes();
        EasyMock.replay(pxService, options,controler, aService,appSupport);

        pxService.request(CatalogSupport.A_IDENTIFIERS);

        final SolrClient client = SolrTestServer.getClient();
        SolrQuery query = new SolrQuery("*")
                .setRows(1)
                .setStart(0);
        try {
            SolrDocumentList docs = client.query(DataCollections.zadost.name(), query).getResults();
            Assert.assertTrue(docs.getNumFound() == 1);
            SolrDocument document = docs.get(0);
            Collection<Object> identifiers = document.getFieldValues("identifiers");
            Assert.assertTrue(CatalogSupport.A_IDENTIFIERS.size() == identifiers.size());
            Assert.assertTrue( document.getFieldValue("navrh").equals("PXN"));
            Assert.assertTrue( document.getFieldValue("state").equals("waiting"));
            Assert.assertTrue( document.getFieldValue("type_of_request").equals("scheduler"));

        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected User testUser() {
        User user = new User();
        user.setInstitution("NKP");
        user.setUsername( "pokusny");
        user.setJmeno( "Jmeno" );
        user.setPrijmeni( "Prijmeni");
        return user;
    }

    protected class BuildSolrClientSupport extends PXYearServiceImpl {

        public BuildSolrClientSupport() {
            super(null, null);
        }

        @Override
        protected SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }


}
