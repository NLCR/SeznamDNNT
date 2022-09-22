package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.*;
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

public class PXServiceImplITTest {

    public static final Logger LOGGER = Logger.getLogger(PXServiceImplITTest.class.getName());

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
    public void testCheck2() throws IOException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiersSKC();

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);
        System.out.println(optionsConfigJSONObject.toString(2));
        
        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 2020]\"},\"results\":{\"ctx\":true}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);
        System.out.println(jonbConfig);
        
        PXKrameriusServiceImpl pxService = EasyMock.createMockBuilder(PXKrameriusServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .addMockedMethod("simpleGET")
                .createMock();
        
        
        Options options = EasyMock.createMock(Options.class);
        EasyMock.expect(options.getJSONObject("check_kramerius")).andReturn(optionsConfigJSONObject.getJSONObject("check_kramerius")).anyTimes();

        EasyMock.expect(pxService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();
        EasyMock.expect(pxService.getOptions()).andReturn(options).anyTimes();

        InputStream resStream = this.getClass().getResourceAsStream("pxservice_kramerius_nkp.json");
        String nkp = IOUtils.toString(resStream, "UTF-8");

        resStream = this.getClass().getResourceAsStream("pxservice_kramerius_knav.json");
        String knav = IOUtils.toString(resStream, "UTF-8");

        resStream = this.getClass().getResourceAsStream("pxservice_kramerius_nkp2.json");
        String nkp2 = IOUtils.toString(resStream, "UTF-8");

        resStream = this.getClass().getResourceAsStream("pxservice_kramerius_svkhk.json");
        String svkhk = IOUtils.toString(resStream, "UTF-8");

        resStream = this.getClass().getResourceAsStream("pxservice_kramerius_mzk.json");
        String mzk = IOUtils.toString(resStream, "UTF-8");

        EasyMock.expect(pxService.simpleGET("http://krameriusndk.nkp.cz/search/api/v5.0/search?q=PID%3A%28uuid%5C%3Aaf9dec90-d5bb-11e3-b110-005056827e51+OR+uuid%5C%3A5498b7a0-d469-11e9-84ef-005056827e51+OR+uuid%5C%3A784ba2b3-17bc-42c2-982f-5ebe48e21905+OR+uuid%5C%3A5498b7a0-d469-11e9-84ef-005056827e51+OR+uuid%5C%3A023d4645-d892-40f1-8b1f-a2a5b933b0e6+OR+uuid%5C%3A113986c0-dcde-11e3-b110-005056827e51+OR+uuid%5C%3A41041210-c2d3-11e2-8b87-005056827e51%29&wt=json&rows=7&fl=PID+dostupnost"))
                                        //  "http://krameriusndk.nkp.cz/search/api/v5.0/search?q=PID%3A%28uuid%5C%3Aaf9dec90-d5bb-11e3-b110-005056827e51+OR+uuid%5C%3A5498b7a0-d469-11e9-84ef-005056827e51+OR+uuid%5C%3A113986c0-dcde-11e3-b110-005056827e51+OR+uuid%5C%3A41041210-c2d3-11e2-8b87-005056827e51%29&wt=json&rows=4&fl=PID+dostupnost"
                    .andReturn(nkp).anyTimes();
        
        EasyMock.expect(pxService.simpleGET("https://kramerius.lib.cas.cz/search/api/v5.0/search?q=PID%3A%28uuid%5C%3A784ba2b3-17bc-42c2-982f-5ebe48e21905%29&wt=json&rows=1&fl=PID+dostupnost"))
                .andReturn(knav).anyTimes();

        
        EasyMock.expect(pxService.simpleGET("http://krameriusndk.nkp.cz/search/api/v5.0/search?q=PID%3A%28uuid%5C%3Aaf9dec90-d5bb-11e3-b110-005056827e51+OR+uuid%5C%3A5498b7a0-d469-11e9-84ef-005056827e51+OR+uuid%5C%3A113986c0-dcde-11e3-b110-005056827e51+OR+uuid%5C%3A41041210-c2d3-11e2-8b87-005056827e51%29&wt=json&rows=4&fl=PID+dostupnost"))
            .andReturn(nkp2).anyTimes();

        EasyMock.expect(pxService.simpleGET("https://kramerius.svkhk.cz/search/api/v5.0/search?q=PID%3A%28uuid%5C%3A023d4645-d892-40f1-8b1f-a2a5b933b0e6%29&wt=json&rows=1&fl=PID+dostupnost"))
            .andReturn(svkhk).anyTimes();

        EasyMock.expect(pxService.simpleGET("https://kramerius.mzk.cz/search/api/v5.0/search?q=PID%3A%28uuid%5C%3A5498b7a0-d469-11e9-84ef-005056827e51%29&wt=json&rows=1&fl=PID+dostupnost"))
            .andReturn(mzk).anyTimes();

        
        EasyMock.replay(pxService, options);

        List<String> check = pxService.check();
        Assert.assertTrue(check.size() == 1);
        Assert.assertTrue(check.contains("oai:aleph-nkp.cz:SKC01-000688007"));
        
    }

    @Test
    public void testCheck() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException, AccountException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiers();

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 2020]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXKrameriusServiceImpl pxService = EasyMock.createMockBuilder(PXKrameriusServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .addMockedMethod("simpleGET")
                .createMock();

        Options options = EasyMock.createMock(Options.class);
        EasyMock.expect(options.getJSONObject("check_kramerius")).andReturn(optionsConfigJSONObject.getJSONObject("check_kramerius")).anyTimes();

        EasyMock.expect(pxService.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();
        EasyMock.expect(pxService.getOptions()).andReturn(options).anyTimes();

        InputStream resStream = this.getClass().getResourceAsStream("pxservice_kramerius.json");
        String s = IOUtils.toString(resStream, "UTF-8");

        EasyMock.expect(pxService.simpleGET("http://krameriusndk.nkp.cz/search/api/v5.0/search?q=PID%3A%28uuid%5C%3Aaf9dec90-d5bb-11e3-b110-005056827e51+OR+uuid%5C%3A113986c0-dcde-11e3-b110-005056827e51+OR+uuid%5C%3A41041210-c2d3-11e2-8b87-005056827e51%29&wt=json&rows=3&fl=PID+dostupnost"))
                .andReturn(s).anyTimes();

        EasyMock.replay(pxService, options);

        List<String> check = pxService.check();
        Assert.assertTrue(check.size() == 2);
        Assert.assertTrue(check.contains("oai:aleph-nkp.cz:DNT01-000008886"));
        Assert.assertTrue(check.contains("oai:aleph-nkp.cz:DNT01-000008874"));

    }

    @Test
    public void testUpdateState() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException, AccountException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiers();

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 1900]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXKrameriusServiceImpl pxService = EasyMock.createMockBuilder(PXKrameriusServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();


        Options options = EasyMock.createMock(Options.class);
        EasyMock.expect(options.getJSONObject("check_kramerius")).andReturn(optionsConfigJSONObject.getJSONObject("check_kramerius")).anyTimes();

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
                Object flag = document.getFieldValue(MarcRecordFields.FLAG_PUBLIC_IN_DL);
                Assert.assertNull(flag);
                Object cStav = document.getFieldValue(MarcRecordFields.KURATORSTAV_FIELD);
                Assert.assertNotNull(cStav);
                
                // History test 
                
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testUpdateContext() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException, AccountException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiers();

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 1900]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXKrameriusServiceImpl pxService = EasyMock.createMockBuilder(PXKrameriusServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();


        Options options = EasyMock.createMock(Options.class);
        EasyMock.expect(options.getJSONObject("check_kramerius")).andReturn(optionsConfigJSONObject.getJSONObject("check_kramerius")).anyTimes();

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
                Object flag = document.getFieldValue(MarcRecordFields.FLAG_PUBLIC_IN_DL);
                Assert.assertNotNull(flag);
                Object cStav = document.getFieldValue(MarcRecordFields.KURATORSTAV_FIELD);
                Assert.assertFalse(cStav.equals(CuratorItemState.PX));
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
        @Test
    public void testUpdateStateAndContext() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException, AccountException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        CatalogSupport.inserAIdentifiers();

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 1900]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXKrameriusServiceImpl pxService = EasyMock.createMockBuilder(PXKrameriusServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .createMock();


        Options options = EasyMock.createMock(Options.class);
        EasyMock.expect(options.getJSONObject("check_kramerius")).andReturn(optionsConfigJSONObject.getJSONObject("check_kramerius")).anyTimes();

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
                Object flag = document.getFieldValue(MarcRecordFields.FLAG_PUBLIC_IN_DL);
                Assert.assertNotNull(flag);
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

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 1900]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXKrameriusServiceImpl pxService = EasyMock.createMockBuilder(PXKrameriusServiceImpl.class)
                .withConstructor("test-logger",jobJSONObject.getJSONObject("iteration"),jobJSONObject.getJSONObject("results"))
                .addMockedMethod("getOptions")
                .addMockedMethod("buildClient")
                .addMockedMethod("buildAccountService")
                .createMock();

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


        Options options = EasyMock.createMock(Options.class);
        EasyMock.expect(options.getJSONObject("check_kramerius")).andReturn(optionsConfigJSONObject.getJSONObject("check_kramerius")).anyTimes();

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


    protected class BuildSolrClientSupport extends PXKrameriusServiceImpl {

        public BuildSolrClientSupport() {
            super(null, null, null);
        }

        @Override
        protected SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }

}
