package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.indexer.models.*;
import cz.inovatika.sdnnt.it.EmbeddedServerPrepare;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.utils.MarcModelTestsUtils;
import cz.inovatika.sdnnt.utils.SolrUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.*;

import java.io.IOException;
import java.util.*;

public class NotificationServiceImplTest {

    public static EmbeddedServerPrepare prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new EmbeddedServerPrepare();
        prepare.setupBeforeClass();
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
    public void testNotificationDen() throws IOException, SolrServerException, NotificationsException, UserControlerException, EmailException {

        MarcRecord marcRecord1 = catalogDoc("notifications/oai:aleph-nkp.cz:DNT01-000057932");
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DAY_OF_WEEK, -1);
        marcRecord1.datum_stavu = calendar1.getTime();

        Assert.assertNotNull(marcRecord1);

        MarcRecord marcRecord2 = catalogDoc("notifications/oai:aleph-nkp.cz:DNT01-000057930");
        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.DAY_OF_WEEK, -1);
        marcRecord2.datum_stavu = calendar2.getTime();


        prepare.getClient().add(  "catalog", marcRecord1.toSolrDoc());
        prepare.getClient().add(  "catalog", marcRecord2.toSolrDoc());

        SolrUtils.quietCommit(prepare.getClient(), "catalog");

        // catalog prepared

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        QueryResponse catalog = prepare.getClient().query("catalog", solrQuery);
        long numFound = catalog.getResults().getNumFound();
        Assert.assertTrue(numFound == 2);
        // saved marc record
        MarcRecord.fromDoc(catalog.getResults().get(0));
        MarcRecord.fromDoc(catalog.getResults().get(1));

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
                Assert.assertTrue(documents.size()  == 2);

                return null;
            }
        }).times(1);

        EasyMock.expect(service.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(mailService, controler, service);
        service.saveNotification(notification("test1", "oai:aleph-nkp.cz:DNT01-000057930"));
        service.saveNotification(notification("test1", "oai:aleph-nkp.cz:DNT01-000057932"));

        List<Notification> notificationsByInterval = service.findNotificationsByInterval(NotificationInterval.den);
        Assert.assertTrue(notificationsByInterval.size() == 2);

        service.processNotifications(NotificationInterval.den);
    }

    private Notification notification(String user, String identifier) {
        Notification notification = new Notification();
        notification.setId(String.format( "%s_%s", user, identifier ));
        notification.setIndextime(new Date());
        notification.setPeriodicity(NotificationInterval.den.name());
        notification.setUser(user);
        notification.setIdentifier(identifier);
        return notification;
    }

    private MarcRecord catalogDoc(String ident ) throws IOException {
        SolrDocument document = MarcModelTestsUtils.prepareResultDocument(ident.replaceAll("\\:","_"));
        Assert.assertNotNull(document);
        return MarcRecord.fromDoc(document);
    }

    private List<User> createNotificationUsers() {
        User user = new User();
        user.username = "test1";
        user.jmeno = "Test_1_jmeno";
        user.prijmeni = "Test_1_prijmeni";
        user.email = "test@testovic.cz";
        return new ArrayList<>(Arrays.asList(user));
    }
}
