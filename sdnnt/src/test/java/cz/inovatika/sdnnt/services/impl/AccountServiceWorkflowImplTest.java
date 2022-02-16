package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.easymock.EasyMock;
import org.junit.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Logger;

public class AccountServiceWorkflowImplTest {

    public static final Logger LOGGER = Logger.getLogger(AccountServiceWorkflowImplTest.class.getName());


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
    public void testNZNWorkflow() throws AccountException, IOException, ConflictException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning("TestSaveZadost is skipping");
            return;
        }

        CatalogSupport.inserNIdentifiers();

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

        EasyMock.replay(controler, service, appLogin);

        // vytvoreni zadosti
        Zadost zadost = new Zadost("01234");
        zadost.setIdentifiers(CatalogSupport.N_IDENTIFIERS);
        zadost.setState("open");
        zadost.setNavrh("NZN");
        service.saveRequest(zadost.toJSON().toString(), null);

        Zadost fromIndex = Zadost.fromJSON(service.getRequest("01234").toString());
        Assert.assertTrue(fromIndex.getNavrh().equals("NZN"));
        Assert.assertTrue(fromIndex.getIdentifiers().size() == CatalogSupport.N_IDENTIFIERS.size());
        Assert.assertNull(fromIndex.getDeadline());
        Assert.assertNull(fromIndex.getDesiredItemState());

        service.userCloseRequest(fromIndex.toJSON().toString());

        Zadost fromIndex2 = Zadost.fromJSON(service.getRequest("01234").toString());
        Assert.assertTrue(fromIndex2.getNavrh().equals("NZN"));
        Assert.assertTrue(fromIndex2.getIdentifiers().size() == CatalogSupport.N_IDENTIFIERS.size());
        Assert.assertNotNull(fromIndex2.getDeadline());
        Assert.assertNotNull(fromIndex2.getDesiredItemState());
        Assert.assertTrue(fromIndex2.getDesiredItemState().equals("NPA"));

        // schvaleni kuratorem
        service.curatorSwitchState(fromIndex.toJSON(), "oai:aleph-nkp.cz:DNT01-000157742", "Reason 1");
        service.curatorSwitchState(fromIndex.toJSON(), "oai:aleph-nkp.cz:DNT01-000157765", "Reason 2 " );

        // Posunuti zaznamu z duvodu spusteni scheduleru
        try (SolrClient client = SolrTestServer.getClient()) {

            MarcRecord oneRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157742");
            Assert.assertTrue(oneRecord.kuratorstav.size() == 1 && oneRecord.kuratorstav.get(0).equals("NPA"));
            moveMonths(client, oneRecord, -6, false);
            client.commit("catalog");

            MarcRecord secondRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157765");
            Assert.assertTrue(secondRecord.kuratorstav.size() == 1 && secondRecord.kuratorstav.get(0).equals("NPA"));
        }

        try (SolrClient client = SolrTestServer.getClient()) {

            MarcRecord oneRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157742");
            Assert.assertTrue(oneRecord.kuratorstav.get(0).equals("NPA"));

            MarcRecord secondRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157765");
            Assert.assertTrue(secondRecord.kuratorstav.get(0).equals("NPA"));
        }

        // NPA -> PA
        service.schedulerSwitchStates("01234");

        try (SolrClient client = SolrTestServer.getClient()) {

            MarcRecord oneRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157742");
            Assert.assertTrue(oneRecord.kuratorstav.get(0).equals("PA"));

            MarcRecord secondRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157765");
            Assert.assertTrue(secondRecord.kuratorstav.get(0).equals("NPA"));
        }



        try (SolrClient client = SolrTestServer.getClient()) {
            MarcRecord oneRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157742");
            // posunuju dokument za lhutu
            moveMonths(client, oneRecord, -12, true);

        }

        // PA -> A
        service.schedulerSwitchStates("01234");
        try (SolrClient client = SolrTestServer.getClient()) {

            MarcRecord oneRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157742");
            Assert.assertTrue(oneRecord.kuratorstav.get(0).equals("A"));

            MarcRecord secondRecord = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:DNT01-000157765");
            Assert.assertTrue(secondRecord.kuratorstav.get(0).equals("NPA"));
        }


    }

    private void moveMonths(SolrClient client, MarcRecord oneRecord, int months, boolean commit) throws SolrServerException, IOException {
        oneRecord.datum_krator_stavu = Date.from(LocalDate.now().plusMonths(months).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        LOGGER.info("Moving doccument '"+oneRecord.identifier+"' to "+oneRecord.datum_krator_stavu);
        SolrInputDocument sdoc = oneRecord.toSolrDoc();
        client.add("catalog", sdoc);
        if (commit) client.commit("catalog");
    }

    static void insertCatalogOneRecord(String states, MarcRecord mr) throws IOException, SolrServerException {
        try (SolrClient client = SolrTestServer.getClient()){
            client.add("catalog", mr.toSolrDoc());
        }

    }

    private User testUser() {
        User user = new User();
        user.setInstitution("NKP");
        user.setUsername( "pokusny");
        user.setJmeno( "Jmeno");
        user.setPrijmeni("Prijmeni");
        return user;
    }

    protected class BuildSolrClientSupport extends AccountServiceImpl {
        @Override
        SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }


}
