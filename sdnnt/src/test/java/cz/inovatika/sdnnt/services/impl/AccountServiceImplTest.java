package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.ZadostProcess;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.model.workflow.DocumentWorkflow;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

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





    @Test
    public void testSaveZadost() throws IOException, SolrServerException, ConflictException {

        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }

        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(controler.getUser()).andReturn(user).anyTimes();
        EasyMock.expect(service.buildClient()).andDelegateTo(
            new BuildSolrClientSupport()
        ).anyTimes();

        EasyMock.replay(controler, service);


        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh(DocumentWorkflow.NZN.name());
        zadost.setTypeOfPeriod(Period.period_0.name());

        service.saveRequest(zadost.toJSON().toString(), user, null);

        Zadost fromService = Zadost.fromJSON(service.getRequest("pokusny11234").toString());

        Assert.assertTrue(fromService.getUser().equals("pokusny"));
        Assert.assertTrue(fromService.getInstitution().equals("NKP"));
        Assert.assertTrue(fromService.getNavrh().equals("NZN"));
    }


    @Test
    public void testSaveAndLoadVersionAndPeriods() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(controler.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh(DocumentWorkflow.NZN.name());

        Period period = Period.period_0;

        zadost.setTypeOfPeriod(period.name());
        zadost.setTypeOfDeadline(period.getDeadlineType().name());
        zadost.setDeadline(period.defineDeadline(new Date()));

        service.saveRequest(zadost.toJSON().toString(), user, null);


        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(savedZadost.getVersion() != null);
        savedZadost.setVersion(""+ (Long.parseLong(savedZadost.getVersion()) - 3));

        try {
            service.saveRequest(savedZadost.toJSON().toString(), user, null);
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


    @Test
    public void testZadostProcess() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("testZadostProcess is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(controler.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh(DocumentWorkflow.NZN.name());


        ZadostProcess zprocess = new ZadostProcess();
        zprocess.setState("approved");
        zprocess.setUser("pokusny");
        zprocess.setReason("Komentar");
        zprocess.setDate(new Date());
        zadost.addProcess("identifier-abc", zprocess);


        service.saveRequest(zadost.toJSON().toString(), user, null);

        String pokusny11234 = service.getRequest("pokusny11234").toString();

        Zadost zadostFromSolr = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Map<String, ZadostProcess> process = zadostFromSolr.getProcess();

        Assert.assertTrue(process != null);
        Assert.assertTrue(process.size() == 1);
        Assert.assertTrue(process.get("identifier-abc") != null);
        Assert.assertTrue(process.get("identifier-abc").getReason().equals("Komentar"));
        Assert.assertTrue(process.get("identifier-abc").getUser().equals("pokusny"));



    }


    @Test
    public void testSendZadostNZN() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(controler.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh(DocumentWorkflow.NZN.name());

        service.sendRequest(zadost.toJSON().toString());


        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(savedZadost.getVersion() !=null );


        Assert.assertTrue(savedZadost.getDesiredItemState().equals("NPA"));
        Assert.assertTrue(savedZadost.getTypeOfDeadline().equals("kurator"));


        Assert.assertNotNull(savedZadost.getDeadline());

        Calendar cal = Calendar.getInstance();
        cal.setTime(savedZadost.getDeadline());

        Assert.assertTrue(cal.get(Calendar.MONTH) == 1 || cal.get(Calendar.MONTH) == 7);
    }


    @Test
    public void testSendZadostVN() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(controler.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("VN");

        service.sendRequest(zadost.toJSON().toString());

        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(Long.parseLong(savedZadost.getVersion()) > 0);

        System.out.println(savedZadost.getDesiredItemState());
        System.out.println(savedZadost.getTypeOfDeadline());
        System.out.println(savedZadost.getDeadline());

//        Assert.assertTrue(savedZadost.getDesiredItemState().equals("NPA"));
//        Assert.assertTrue(savedZadost.getTypeOfDeadline().equals("kurator"));
//
//
//        Assert.assertNotNull(savedZadost.getDeadline());
//
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(savedZadost.getDeadline());
//
//        Assert.assertTrue(cal.get(Calendar.MONTH) == 1 || cal.get(Calendar.MONTH) == 7);
    }


    @Test
    public void testSendZadostVNZ() throws IOException, SolrServerException, ConflictException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(controler.getUser()).andReturn(user).anyTimes();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(controler, service);

        prepare.getClient().deleteByQuery("zadost", "*:*");
        prepare.getClient().commit("zadost");

        Zadost zadost = new Zadost("pokusny11234");
        zadost.setIdentifiers(Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930"));
        zadost.setInstitution("NKP");
        zadost.setUser("pokusny");
        zadost.setNavrh("VNZ");

        service.sendRequest(zadost.toJSON().toString());

        Zadost savedZadost = Zadost.fromJSON(service.getRequest("pokusny11234").toString());
        Assert.assertTrue(Long.parseLong(savedZadost.getVersion()) > 0);

        System.out.println(savedZadost.getDesiredItemState());
        System.out.println(savedZadost.getTypeOfDeadline());
        System.out.println(savedZadost.getDeadline());

//        Assert.assertTrue(savedZadost.getDesiredItemState().equals("NPA"));
//        Assert.assertTrue(savedZadost.getTypeOfDeadline().equals("kurator"));
//
//
//        Assert.assertNotNull(savedZadost.getDeadline());
//
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(savedZadost.getDeadline());
//
//        Assert.assertTrue(cal.get(Calendar.MONTH) == 1 || cal.get(Calendar.MONTH) == 7);
    }



    private User testUser() {
        User user = new User();
        user.institution="NKP";
        user.username = "pokusny";
        user.jmeno = "Jmeno";
        user.prijmeni = "Prijmeni";
        return user;
    }

    protected class BuildSolrClientSupport extends AccountServiceImpl {
        @Override
        SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }
}
