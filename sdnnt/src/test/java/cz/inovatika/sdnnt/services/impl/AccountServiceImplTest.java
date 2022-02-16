package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.AccountServlet;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.ZadostProcess;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.ServletsSupport;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AccountServiceImplTest {

    public static final Logger LOGGER = Logger.getLogger(AccountServiceImplTest.class.getName());

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
        if(SolrTestServer.TEST_SERVER_IS_RUNNING) {
            prepare.deleteCores("users","zadost","catalog");
        }
    }




    // Plain save
    @Test
    public void testSaveZadost() throws IOException, SolrServerException, ConflictException, AccountException {

        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }

        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);
        ApplicationUserLoginSupport appLogin = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler, appLogin, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appLogin.getUser()).andReturn(user).anyTimes();
        EasyMock.expect(service.buildClient()).andDelegateTo(
            new BuildSolrClientSupport()
        ).anyTimes();

        EasyMock.replay(controler, service ,bservice, appLogin);


        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setState("open");
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("NZN");
        zadost.setTypeOfPeriod(Period.period_nzn_1_12_18.name());
        zadost.setPozadavek("Pozadavek ABC");
        zadost.setPoznamka("Poznamka ABC");

        service.saveRequest(zadost.toJSON().toString(), null);

        Zadost fromService = Zadost.fromJSON(service.getRequest("pokusny11234").toString());

        Assert.assertTrue(fromService.getUser().equals("pokusny"));
        Assert.assertTrue(fromService.getInstitution().equals("NKP"));
        Assert.assertTrue(fromService.getNavrh().equals("NZN"));

        Assert.assertNotNull(fromService.getPozadavek());
        Assert.assertNotNull(fromService.getPoznamka());

    }


    // Plain saave version and periods
    @Test
    public void testSaveAndLoadVersionAndPeriods() throws IOException, SolrServerException, ConflictException, AccountException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        ApplicationUserLoginSupport appLogin = EasyMock.createMock(ApplicationUserLoginSupport.class);
        UserControler controler  = EasyMock.createMock(UserControler.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler, appLogin, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appLogin.getUser()).andReturn(user).anyTimes();
        EasyMock.expect(bservice.getBundle()).andReturn(null).anyTimes();


        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service, appLogin,bservice);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setState("open");
        zadost.setNavrh("NZN");

        Period period = Period.period_nzn_1_12_18;

        zadost.setTypeOfPeriod(period.name());
        zadost.setTransitionType(period.getTransitionType().name());
        zadost.setDeadline(period.defineDeadline(new Date()));

        service.saveRequest(zadost.toJSON().toString(), null);


        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(savedZadost.getVersion() != null);
        savedZadost.setVersion(""+ (Long.parseLong(savedZadost.getVersion()) - 3));

        try {
            service.saveRequest(savedZadost.toJSON().toString(), null);
            Assert.fail("Excpecting versions conflict");
        } catch (SolrServerException e) {
            Assert.fail("exception "+e.getMessage());
        } catch (IOException e) {
            Assert.fail("exception "+e.getMessage());
        } catch (ConflictException e) {
            // ok. must be conflict
            System.out.println("TEstik ");
        }
    }


    // Test zadost save process
    @Test
    public void testZadostKuratorProcess() throws IOException, SolrServerException, ConflictException, AccountException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("testZadostProcess is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        ApplicationUserLoginSupport appLogin = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler,appLogin, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appLogin.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service, appLogin);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setState("open");
        zadost.setNavrh("NZN");


        ZadostProcess zprocess = new ZadostProcess();
        zprocess.setState("approved");
        zprocess.setUser("pokusny");
        zprocess.setReason("Komentar");
        zprocess.setDate(new Date());
        zadost.addProcess("identifier-abc", zprocess);


        service.saveRequest(zadost.toJSON().toString(), null);

        String pokusny11234 = service.getRequest("pokusny11234").toString();

        Zadost zadostFromSolr = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Map<String, ZadostProcess> process = zadostFromSolr.getProcess();

        Assert.assertTrue(process != null);
        Assert.assertTrue(process.size() == 1);
        Assert.assertTrue(process.get("identifier-abc") != null);
        Assert.assertTrue(process.get("identifier-abc").getReason().equals("Komentar"));
        Assert.assertTrue(process.get("identifier-abc").getUser().equals("pokusny"));
    }

    // Test send zadost
    @Test
    public void testSendZadostNZN() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);
        ApplicationUserLoginSupport appLogin = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler, appLogin, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appLogin.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service, appLogin);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("NZN");

        service.userCloseRequest(zadost.toJSON().toString());


        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(savedZadost.getVersion() !=null );


        Assert.assertTrue(savedZadost.getDesiredItemState().equals("NPA"));
        Assert.assertTrue(savedZadost.getTransitionType().equals("kurator"));


        Assert.assertNotNull(savedZadost.getDeadline());


        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.ofInstant(savedZadost.getDeadline().toInstant(), ZoneId.systemDefault());

        // debug configuration !
//        int days = java.time.Period.between(date1, date2).getDays();
//        Assert.assertTrue(days >= 5);
    }


    @Test
    public void testSendZadostVN() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);
        ApplicationUserLoginSupport appLogin =  EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);


        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler,appLogin, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appLogin.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service,appLogin);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("VN");

        service.userCloseRequest(zadost.toJSON().toString());

        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(Long.parseLong(savedZadost.getVersion()) > 0);


        Assert.assertTrue(savedZadost.getDesiredItemState().equals("N"));
        Assert.assertNotNull(savedZadost.getTransitionType() != null);
        Assert.assertNotNull(savedZadost.getDeadline() != null);
    }


    @Test
    public void testSendZadostVNZ() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);
        ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);


        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler, appSupport, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appSupport.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service,appSupport);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("VNZ");

        service.userCloseRequest(zadost.toJSON().toString());

        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(Long.parseLong(savedZadost.getVersion()) > 0);
    }

    // Po zavreni kuratorem ceka na uatomaticke zpracovani
    @Test
    public void testZadostProcessedApproveNZN_WaitingState() throws IOException, SolrServerException, ConflictException, AccountException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        AccountServiceImpl service = zadostProcessPrepare();
        CatalogSupport.inserNIdentifiers();
        // Zadost
        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(CatalogSupport. N_IDENTIFIERS);
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("NZN");
        zadost.setDatumZadani(new Date());

        service.commit("catalog","zadost","history");


        JSONObject zadostJSON = service.userCloseRequest(zadost.toJSON().toString());
        for (String identifier : CatalogSupport.N_IDENTIFIERS) {
            try {
                zadostJSON = service.curatorSwitchState(zadostJSON, identifier, "Test reason");
                // committing
                service.commit("catalog","zadost","history");
            } catch (AccountException e) {
                Assert.fail(e.getMessage());
            }
        }

        JSONObject nZadost = service.getRequest(zadost.getId());
        // kuratorske zavreni zadosti
        service.curatorCloseRequest(nZadost.toString());
        // po schvaleni, waiting for automatic process
        Zadost approvedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertNotNull(approvedZadost.getState());
        Assert.assertEquals(approvedZadost.getState(), "waiting_for_automatic_process");
    }

    // Po zavreni kuratorem je ve stavu processed
    @Test
    public void testZadostProcessedRejectNZN() throws IOException, SolrServerException, ConflictException, AccountException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        AccountServiceImpl service = zadostProcessPrepare();
        CatalogSupport.inserNIdentifiers();

        // Zadost
        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(CatalogSupport. N_IDENTIFIERS);
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("NZN");
        zadost.setDatumZadani(new Date());

        service.commit("catalog","zadost","history");

        JSONObject zadostJSON = service.userCloseRequest(zadost.toJSON().toString());
        for (String identifier : CatalogSupport.N_IDENTIFIERS) {
            try {
                zadostJSON = service.curatorRejectSwitchState(zadostJSON, identifier, "Test reason");
                // committing
                service.commit("catalog","zadost","history");
            } catch (AccountException e) {
                Assert.fail(e.getMessage());
            }
        }

        JSONObject nZadost = service.getRequest(zadost.getId());
        // kuratorske zavreni zadosti
        service.curatorCloseRequest(nZadost.toString());
        // po schvaleni, waiting for automatic process
        Zadost rejectedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertNotNull(rejectedZadost.getState());
        System.out.println(rejectedZadost.getState());
        Assert.assertEquals(rejectedZadost.getState(), "processed");
    }


    private AccountServiceImpl zadostProcessPrepare() throws SolrServerException, IOException {
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);
        ApplicationUserLoginSupport appSupport = EasyMock.createMock(ApplicationUserLoginSupport.class);
        ResourceServiceService bservice = EasyMock.createMock(ResourceServiceService.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(appSupport, bservice)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(appSupport.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service,appSupport);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        prepare.getClient().deleteByQuery("catalog", "*:*");
        prepare.getClient().commit("catalog");
        return service;
    }


    @Test
    public void testZadostProcessedApprove() throws IOException, SolrServerException, ConflictException, AccountException {
    }

    private User testUser() {
        User user = new User();
        user.setInstitution("NKP");
        user.setUsername( "pokusny");
        user.setJmeno( "Jmeno" );
        user.setPrijmeni( "Prijmeni");
        return user;
    }

    protected class BuildSolrClientSupport extends AccountServiceImpl {
        @Override
        SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }
}
