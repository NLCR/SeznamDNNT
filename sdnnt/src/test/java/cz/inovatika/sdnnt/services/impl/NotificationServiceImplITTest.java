package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.indexer.models.*;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;
import cz.inovatika.sdnnt.indexer.models.notifications.NotificationFactory;
import cz.inovatika.sdnnt.indexer.models.notifications.RuleNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.SimpleNotification;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.utils.MarcModelTestsUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.json.JSONArray;
import org.junit.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import static cz.inovatika.sdnnt.index.DntAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.DntAlephTestUtils.dntAlephStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

public class NotificationServiceImplITTest {

    public static final Logger LOGGER = Logger.getLogger(NotificationServiceImplITTest.class.getName());

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("notifikace");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("notifications","catalog");
    }


    
    
    @Test
    public void testSaveNotifications() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException {

        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        UserControler controler = EasyMock.createMock(UserControler.class);

        EasyMock.expect(controler.findUsersByNotificationInterval(NotificationInterval.den.name()))
                .andReturn(createNotificationUsers())
                .anyTimes();

        MailServiceImpl mailService = EasyMock.createMockBuilder(MailServiceImpl.class)
                .addMockedMethod("sendNotificationEmail")
                .createMock();

        NotificationServiceImpl service = EasyMock.createMockBuilder(NotificationServiceImpl.class)
                .withConstructor(controler, mailService)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();

        EasyMock.replay(mailService, controler, service);

        service.saveSimpleNotification(simpleNotification("test1", "notification_knihovna_oai_aleph-nkp.cz_SKC01-000057930.json"));
        service.saveSimpleNotification(simpleNotification("test1", "notification_knihovna_oai_aleph-nkp.cz_SKC01-000057932.json"));
        service.saveNotificationRule(ruleNotification("knihovna", "notification_knihovna_rulebased.json"));

        //service.saveNotificationRule(ruleNotification("knihovna", "notification_knihovna_rulebased3.json"));

        Assert.assertTrue(service.findNotificationsByUser("test1").size() == 2);
        Assert.assertTrue(service.findNotificationsByUser("test1", TYPE.simple).size() == 2);
        Assert.assertTrue(service.findNotificationsByUser("test1", TYPE.rule).size() == 0);

        Assert.assertTrue( service.findNotificationsByUser("knihovna").size() == 1);
        Assert.assertTrue( service.findNotificationsByUser("knihovna", TYPE.rule).size() == 1);
        Assert.assertTrue( service.findNotificationsByUser("knihovna", TYPE.simple).size() == 0);
    }    

    
    
    
    @Test
    public void testSendNotifications_SIMPLE_RULE() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException {

        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        MarcRecord marcRecord1 = catalogDoc("notifications/oai:aleph-nkp.cz:DNT01-000057932");
        Assert.assertNotNull(marcRecord1);

        MarcRecord marcRecord2 = catalogDoc("notifications/oai:aleph-nkp.cz:DNT01-000057930");
        Assert.assertNotNull(marcRecord2);

        prepare.getClient().add(  "catalog", marcRecord1.toSolrDoc());
        prepare.getClient().add(  "catalog", marcRecord2.toSolrDoc());

        SolrJUtilities.quietCommit(prepare.getClient(), "catalog");
        

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        QueryResponse catalog = prepare.getClient().query("catalog", solrQuery);
        long numFound = catalog.getResults().getNumFound();
        Assert.assertTrue(numFound == 2);
        // saved marc record
        MarcRecord solrMarc1 = MarcRecord.fromDoc(catalog.getResults().get(0));
        MarcRecord solrMarc2 = MarcRecord.fromDoc(catalog.getResults().get(1));
        
        
        solrMarc1.setKuratorStav("X", "X", null, "testuser", "poznamka", null);
        solrMarc2.setKuratorStav("X", "X", null, "testuser", "poznamka", null);

        Calendar calendar1 = Calendar.getInstance();
        //calendar1.add(Calendar.DAY_OF_WEEK, -1);
        solrMarc1.datum_stavu = calendar1.getTime();


        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.DAY_OF_WEEK, -1);
        solrMarc2.datum_stavu = calendar2.getTime();
 
        // save and commit
        prepare.getClient().add(  "catalog", solrMarc1.toSolrDoc());
        prepare.getClient().add(  "catalog", solrMarc2.toSolrDoc());
        SolrJUtilities.quietCommit(prepare.getClient(), "catalog");

        MailServiceImpl mailService = EasyMock.createMockBuilder(MailServiceImpl.class)
                .addMockedMethod("sendNotificationEmail")
                .createMock();

        UserControler controler = EasyMock.createMock(UserControler.class);

        EasyMock.expect(controler.findUsersByNotificationInterval(NotificationInterval.den.name()))
                .andReturn(createNotificationUsers())
                .anyTimes();

        NotificationServiceImpl service = EasyMock.createMockBuilder(NotificationServiceImpl.class)
                .withConstructor(controler, mailService)
                .addMockedMethod("buildClient").createMock();



        mailService.sendNotificationEmail(
                EasyMock.isA(Pair.class),
                EasyMock.isA(List.class)
        );

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            @Override
            public Object answer() throws Throwable {
                Pair<String,String> pair = (Pair<String, String>) EasyMock.getCurrentArguments()[0];
                List<Map<String,String>> documents = (List<Map<String, String>>) EasyMock.getCurrentArguments()[1];
                Assert.assertTrue(pair.getLeft().equals("test@testovic.cz"));
                System.out.println("Document size "+ documents.size());
                Assert.assertTrue(documents.size()  == 2);
                return null;
            }
        }).times(1);


        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(mailService, controler, service);

        service.saveSimpleNotification(simpleNotification("test1", "notification_knihovna_oai_aleph-nkp.cz_SKC01-000057930.json"));
        service.saveSimpleNotification(simpleNotification("test1", "notification_knihovna_oai_aleph-nkp.cz_SKC01-000057932.json"));
        service.saveNotificationRule(ruleNotification("test1", "notification_knihovna_rulebased_dntstavA.json"));

        List<AbstractNotification> notificationsByInterval = service.findNotificationsByInterval(NotificationInterval.den);
        Assert.assertTrue(notificationsByInterval.size() == 3);
        
        service.processNotifications(NotificationInterval.den);
    }


    
    @Test
    public void testSendNotifications_QUERY() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException, FactoryConfigurationError, XMLStreamException {

        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        
        InputStream resourceAsStream = dntAlephStream("oai_SE_dnnt.xml");
        Assert.assertNotNull(resourceAsStream);
        alephImport(resourceAsStream,36);
        SolrJUtilities.quietCommit(prepare.getClient(), "catalog");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("identifier:\"oai:aleph-nkp.cz:DNT01-000172209\"");
        QueryResponse catalog = prepare.getClient().query("catalog", solrQuery);
        long numFound = catalog.getResults().getNumFound();
        Assert.assertTrue(numFound == 1);
        // saved marc record
        MarcRecord solrMarc1 = MarcRecord.fromDoc(catalog.getResults().get(0));
        
        
        solrMarc1.setKuratorStav("X", "X", null, "testuser", "poznamka", null);

        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DAY_OF_WEEK, -1);
        solrMarc1.datum_stavu = calendar1.getTime();

 
        // save and commit
        prepare.getClient().add(  "catalog", solrMarc1.toSolrDoc());
        SolrJUtilities.quietCommit(prepare.getClient(), "catalog");

        MailServiceImpl mailService = EasyMock.createMockBuilder(MailServiceImpl.class)
                .addMockedMethod("sendNotificationEmail")
                .createMock();

        UserControler controler = EasyMock.createMock(UserControler.class);

        EasyMock.expect(controler.findUsersByNotificationInterval(NotificationInterval.den.name()))
                .andReturn(createNotificationUsers())
                .anyTimes();

        NotificationServiceImpl service = EasyMock.createMockBuilder(NotificationServiceImpl.class)
                .withConstructor(controler, mailService)
                .addMockedMethod("buildClient").createMock();



        mailService.sendNotificationEmail(
                EasyMock.isA(Pair.class),
                EasyMock.isA(List.class)
        );

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            @Override
            public Object answer() throws Throwable {
                Pair<String,String> pair = (Pair<String, String>) EasyMock.getCurrentArguments()[0];
                List<Map<String,String>> documents = (List<Map<String, String>>) EasyMock.getCurrentArguments()[1];
                Assert.assertTrue(pair.getLeft().equals("test@testovic.cz"));
                System.out.println("Document size "+ documents.size());
                Assert.assertTrue(documents.size()  == 1);
                return null;
            }
        }).times(1);


        EasyMock.expect(service.buildClient()).andDelegateTo(
                new BuildSolrClientSupport()
        ).anyTimes();


        EasyMock.replay(mailService, controler, service);

        service.saveNotificationRule(ruleNotification("test1", "notification_knihovna_rulebased_query.json"));

        List<AbstractNotification> notificationsByInterval = service.findNotificationsByInterval(NotificationInterval.den);
        Assert.assertTrue(notificationsByInterval.size() == 1);
        
        service.processNotifications(NotificationInterval.den);

            
    }

    static SimpleNotification simpleNotification(String user, String identifier) throws IOException {
        InputStream resourceAsStream = NotificationServiceImplITTest.class.getClassLoader()
                .getResourceAsStream("cz/inovatika/sdnnt/indexer/models/notifications/" + identifier);
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        AbstractNotification notification = NotificationFactory.fromJSON(jsonString);
        notification.setUser(user);
        return (SimpleNotification) notification;
    }

    static RuleNotification ruleNotification(String user, String identifier) throws IOException {
        InputStream resourceAsStream = NotificationServiceImplITTest.class.getClassLoader()
                .getResourceAsStream("cz/inovatika/sdnnt/indexer/models/notifications/" + identifier);
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        AbstractNotification notification = NotificationFactory.fromJSON(jsonString);
        notification.setUser(user);
        return (RuleNotification) notification;
    }

    private MarcRecord catalogDoc(String ident ) throws IOException {
        SolrDocument document = MarcModelTestsUtils.prepareResultDocument(ident.replaceAll("\\:","_"));
        Assert.assertNotNull(document);
        return MarcRecord.fromDoc(document);
    }

    private List<User> createNotificationUsers() {
        User user = new User();
        user.setUsername( "test1");
        user.setJmeno( "Test_1_jmeno");
        user.setPrijmeni( "Test_1_prijmeni");
        user.setEmail( "test@testovic.cz");
        return new ArrayList<>(Arrays.asList(user));
    }


    protected class BuildSolrClientSupport extends NotificationServiceImpl {

        public BuildSolrClientSupport() {
            super(null, null);
        }

        @Override
        SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }

}