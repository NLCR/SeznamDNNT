package cz.inovatika.sdnnt.services.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.UserController;

public class SKCJoinServiceImplITTest {

    public static final Logger LOGGER = Logger.getLogger(SKCJoinServiceImplITTest.class.getName());

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("skc_join");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("notification_events");
    }

    @Test
    public void testDeliverSKCJoinNotificationsToQueue() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        SKCJoinServiceImpl service = EasyMock.createMockBuilder(SKCJoinServiceImpl.class)
                .withConstructor("test", new JSONObject())
                .addMockedMethod("notificationAdminEmailDelivery")
                .createMock();

        EasyMock.expect(service.notificationAdminEmailDelivery())
                .andReturn("queue")
                .anyTimes();

        EasyMock.replay(service);

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_1,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000011"), "SKC_4");
        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000002"), Case.SKC_2b,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000021"), "SKC_4b");

        try (SolrClient client = SolrTestServer.getClient()) {
            service.deliverSKC4SuccessorNotifications(client);
            client.commit("notification_events");
        }

        QueryResponse response = prepare.getClient().query("notification_events",
                new SolrQuery("*:*")
                        .addFilterQuery("event_type:skc_join_successor_found")
                        .addFilterQuery("status:ready")
                        .setSort("subject_id", SolrQuery.ORDER.asc));

        Assert.assertEquals(2, response.getResults().getNumFound());
        Assert.assertEquals("SKCJoinServiceImpl", response.getResults().get(0).getFieldValue("source"));

        JSONObject firstPayload = new JSONObject(response.getResults().get(0).getFieldValue("payload").toString());
        JSONObject secondPayload = new JSONObject(response.getResults().get(1).getFieldValue("payload").toString());
        Assert.assertEquals("SKC_1", firstPayload.getString("detected_case"));
        Assert.assertEquals("SKC_4", firstPayload.getString("previous_comment"));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000011", firstPayload.getJSONArray("followers").getString(0));
        Assert.assertEquals("SKC_2b", secondPayload.getString("detected_case"));
        Assert.assertEquals("SKC_4b", secondPayload.getString("previous_comment"));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000021", secondPayload.getJSONArray("followers").getString(0));

        EasyMock.verify(service);
    }

    @Test
    public void testQueuedSKCJoinNotificationContainsCuratorQueueData() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        SKCJoinServiceImpl service = EasyMock.createMockBuilder(SKCJoinServiceImpl.class)
                .withConstructor("test", new JSONObject())
                .addMockedMethod("notificationAdminEmailDelivery")
                .createMock();

        EasyMock.expect(service.notificationAdminEmailDelivery())
                .andReturn("queue")
                .anyTimes();

        EasyMock.replay(service);

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_2a,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000021", "oai:aleph-nkp.cz:DNT01-000000022"), "SKC_4b");

        try (SolrClient client = SolrTestServer.getClient()) {
            service.deliverSKC4SuccessorNotifications(client);
            client.commit("notification_events");
        }

        QueryResponse response = prepare.getClient().query("notification_events",
                new SolrQuery("*:*")
                        .addFilterQuery("event_type:skc_join_successor_found")
                        .addFilterQuery("status:ready"));

        Assert.assertEquals(1, response.getResults().getNumFound());
        Assert.assertEquals("curator", response.getResults().get(0).getFieldValue("channel"));
        Assert.assertEquals("SKCJoinServiceImpl", response.getResults().get(0).getFieldValue("source"));
        Assert.assertEquals("catalog", response.getResults().get(0).getFieldValue("subject_type"));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000001",
                response.getResults().get(0).getFieldValue("subject_id"));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000001",
                response.getResults().get(0).getFieldValue("origin_identifier"));

        JSONObject payload = new JSONObject(response.getResults().get(0).getFieldValue("payload").toString());
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000001", payload.getString("identifier"));
        Assert.assertEquals("SKC_2a", payload.getString("detected_case"));
        Assert.assertEquals("SKC_4b", payload.getString("previous_comment"));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000021", payload.getJSONArray("followers").getString(0));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000022", payload.getJSONArray("followers").getString(1));

        JSONObject recipientTarget = payload.getJSONObject("recipient_target");
        Assert.assertEquals("roles", recipientTarget.getString("type"));
        Assert.assertEquals("kurator", recipientTarget.getJSONArray("roles").getString(0));
        Assert.assertEquals("mainKurator", recipientTarget.getJSONArray("roles").getString(1));

        EasyMock.verify(service);
    }

    @Test
    public void testDeliverSKCJoinNotificationsToQueueKeepsSingleReadyEventForSameKey() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        SKCJoinServiceImpl service = EasyMock.createMockBuilder(SKCJoinServiceImpl.class)
                .withConstructor("test", new JSONObject())
                .addMockedMethod("notificationAdminEmailDelivery")
                .createMock();

        EasyMock.expect(service.notificationAdminEmailDelivery())
                .andReturn("queue")
                .anyTimes();

        EasyMock.replay(service);

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_1,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000011"), "SKC_4");

        try (SolrClient client = SolrTestServer.getClient()) {
            service.deliverSKC4SuccessorNotifications(client);
            client.commit("notification_events");
        }

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_1,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000011"), "SKC_4");

        try (SolrClient client = SolrTestServer.getClient()) {
            service.deliverSKC4SuccessorNotifications(client);
            client.commit("notification_events");
        }

        QueryResponse response = prepare.getClient().query("notification_events",
                new SolrQuery("*:*")
                        .addFilterQuery("event_type:skc_join_successor_found")
                        .addFilterQuery("status:ready")
                        .addFilterQuery("subject_id:\"oai:aleph-nkp.cz:DNT01-000000001\""));

        Assert.assertEquals(1, response.getResults().getNumFound());

        EasyMock.verify(service);
    }

    @Test
    public void testDeliverSKCJoinNotificationsDirectAggregatesToOneEmail() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        MailService mailService = EasyMock.createMock(MailService.class);
        mailService.sendMail(EasyMock.isA(Pair.class), EasyMock.isA(List.class), EasyMock.isA(String.class),
                EasyMock.isA(String.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            @Override
            public Object answer() {
                Pair<String, String> from = (Pair<String, String>) EasyMock.getCurrentArguments()[0];
                List<Pair<String, String>> recipients = (List<Pair<String, String>>) EasyMock.getCurrentArguments()[1];
                String subject = (String) EasyMock.getCurrentArguments()[2];
                String text = (String) EasyMock.getCurrentArguments()[3];

                Assert.assertEquals("from@testovic.cz", from.getLeft());
                Assert.assertEquals(1, recipients.size());
                Assert.assertEquals("kurator@testovic.cz", recipients.get(0).getLeft());
                Assert.assertEquals("SKCJoin test", subject);
                Assert.assertTrue(text.contains("Pocet zaznamu: 2"));
                Assert.assertTrue(text.contains("oai:aleph-nkp.cz:DNT01-000000001"));
                Assert.assertTrue(text.contains("oai:aleph-nkp.cz:DNT01-000000002"));
                Assert.assertTrue(text.contains("oai:aleph-nkp.cz:DNT01-000000022"));
                return null;
            }
        }).once();

        SKCJoinServiceImpl service = EasyMock.createMockBuilder(SKCJoinServiceImpl.class)
                .withConstructor("test", new JSONObject())
                .addMockedMethod("notificationAdminEmailDelivery")
                .addMockedMethod("buildMailService")
                .addMockedMethod("mailFrom")
                .addMockedMethod("skcJoinRecipients")
                .addMockedMethod("skcJoinSubject")
                .createMock();

        EasyMock.expect(service.notificationAdminEmailDelivery())
                .andReturn("direct")
                .anyTimes();
        EasyMock.expect(service.buildMailService())
                .andReturn(mailService)
                .anyTimes();
        EasyMock.expect(service.mailFrom())
                .andReturn(Pair.of("from@testovic.cz", "SDNNT"))
                .anyTimes();
        EasyMock.expect(service.skcJoinRecipients())
                .andReturn(Collections.singletonList(Pair.of("kurator@testovic.cz", "Kurator")))
                .anyTimes();
        EasyMock.expect(service.skcJoinSubject())
                .andReturn("SKCJoin test")
                .anyTimes();

        EasyMock.replay(mailService, service);

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_1,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000011"), "SKC_4");
        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000002"), Case.SKC_2a,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000021", "oai:aleph-nkp.cz:DNT01-000000022"), "SKC_4b");

        try (SolrClient client = SolrTestServer.getClient()) {
            service.deliverSKC4SuccessorNotifications(client);
            client.commit("notification_events");
        }

        QueryResponse response = prepare.getClient().query("notification_events", new SolrQuery("*:*").setRows(0));
        Assert.assertEquals(0, response.getResults().getNumFound());

        EasyMock.verify(mailService, service);
    }

    @Test
    public void testDeliverSKCJoinNotificationsDirectRespectsAdministratorNotificationSettings() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        MailService mailService = EasyMock.createMock(MailService.class);
        mailService.sendMail(EasyMock.isA(Pair.class), EasyMock.isA(List.class), EasyMock.isA(String.class),
                EasyMock.isA(String.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            @Override
            public Object answer() {
                List<Pair<String, String>> recipients = (List<Pair<String, String>>) EasyMock.getCurrentArguments()[1];
                String text = (String) EasyMock.getCurrentArguments()[3];

                Assert.assertEquals(2, recipients.size());
                Assert.assertEquals("kurator@testovic.cz", recipients.get(0).getLeft());
                Assert.assertEquals("main@testovic.cz", recipients.get(1).getLeft());
                Assert.assertTrue(text.contains("Pocet zaznamu: 2"));
                Assert.assertTrue(text.contains("oai:aleph-nkp.cz:DNT01-000000001"));
                Assert.assertTrue(text.contains("oai:aleph-nkp.cz:DNT01-000000002"));
                return null;
            }
        }).once();

        UserController users = EasyMock.createMock(UserController.class);
        UserController shibUsers = EasyMock.createMock(UserController.class);
        EasyMock.expect(users.findUsersByRole(Role.kurator))
                .andReturn(Arrays.asList(
                        user("kurator1", "kurator@testovic.cz", "Karel", "Kurator", true),
                        user("kurator2", "vypnuty@testovic.cz", "Vypnuty", "Kurator", false)))
                .once();
        EasyMock.expect(shibUsers.findUsersByRole(Role.kurator))
                .andReturn(Collections.<User>emptyList())
                .once();
        EasyMock.expect(users.findUsersByRole(Role.mainKurator))
                .andReturn(Arrays.asList(user("mainkurator", "main@testovic.cz", "Marta", "Kuratorova", true)))
                .once();
        EasyMock.expect(shibUsers.findUsersByRole(Role.mainKurator))
                .andReturn(Collections.<User>emptyList())
                .once();

        SKCJoinServiceImpl service = EasyMock.createMockBuilder(SKCJoinServiceImpl.class)
                .withConstructor("test", new JSONObject())
                .addMockedMethod("notificationAdminEmailDelivery")
                .addMockedMethod("buildMailService")
                .addMockedMethod("mailFrom")
                .addMockedMethod("skcJoinSubject")
                .addMockedMethod("buildUserController")
                .addMockedMethod("buildShibUsersController")
                .createMock();

        EasyMock.expect(service.notificationAdminEmailDelivery())
                .andReturn("direct")
                .anyTimes();
        EasyMock.expect(service.buildMailService())
                .andReturn(mailService)
                .anyTimes();
        EasyMock.expect(service.mailFrom())
                .andReturn(Pair.of("from@testovic.cz", "SDNNT"))
                .anyTimes();
        EasyMock.expect(service.skcJoinSubject())
                .andReturn("SKCJoin test")
                .anyTimes();
        EasyMock.expect(service.buildUserController())
                .andReturn(users)
                .anyTimes();
        EasyMock.expect(service.buildShibUsersController())
                .andReturn(shibUsers)
                .anyTimes();

        EasyMock.replay(mailService, users, shibUsers, service);

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_1,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000011"), "SKC_4");
        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000002"), Case.SKC_2a,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000021"), "SKC_4b");

        try (SolrClient client = SolrTestServer.getClient()) {
            service.deliverSKC4SuccessorNotifications(client);
            client.commit("notification_events");
        }

        EasyMock.verify(mailService, users, shibUsers, service);
    }

    @Test
    public void testSendRealSKCJoinNotificationEmailFromOptions() throws Exception {
//        Assume.assumeTrue("Manual SMTP test. Run with -Dsdnnt.test.sendMail=true",
//                Boolean.getBoolean("sdnnt.test.sendMail"));

        UserController users = EasyMock.createMock(UserController.class);
        UserController shibUsers = EasyMock.createMock(UserController.class);
        EasyMock.expect(users.findUsersByRole(Role.kurator))
                .andReturn(Collections.<User>emptyList())
                .anyTimes();
        EasyMock.expect(shibUsers.findUsersByRole(Role.kurator))
                .andReturn(Collections.<User>emptyList())
                .anyTimes();
        EasyMock.expect(users.findUsersByRole(Role.mainKurator))
                .andReturn(Collections.<User>emptyList())
                .anyTimes();
        EasyMock.expect(shibUsers.findUsersByRole(Role.mainKurator))
                .andReturn(Collections.<User>emptyList())
                .anyTimes();

        SKCJoinServiceImpl service = EasyMock.createMockBuilder(SKCJoinServiceImpl.class)
                .withConstructor("test", new JSONObject())
                .addMockedMethod("notificationAdminEmailDelivery")
                .addMockedMethod("buildUserController")
                .addMockedMethod("buildShibUsersController")
                .addMockedMethod("skcJoinRecipients")
                .createMock();

        EasyMock.expect(service.notificationAdminEmailDelivery())
                .andReturn("direct")
                .anyTimes();
        EasyMock.expect(service.buildUserController())
                .andReturn(users)
                .anyTimes();
        EasyMock.expect(service.buildShibUsersController())
                .andReturn(shibUsers)
                .anyTimes();
        EasyMock.expect(service.skcJoinRecipients())
                .andDelegateTo(new RealMailRecipientSKCJoinService())
                .anyTimes();

        EasyMock.replay(users, shibUsers, service);

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_1,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000011"), "SKC_4");
        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000002"), Case.SKC_2a,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000021", "oai:aleph-nkp.cz:DNT01-000000022"), "SKC_4b");

        service.deliverSKC4SuccessorNotifications(null);

        EasyMock.verify(users, shibUsers, service);
    }

    private MarcRecord record(String identifier) {
        MarcRecord record = new MarcRecord();
        record.identifier = identifier;
        record.fmt = "BK";
        record.license = "dnnto";
        record.dntstav = Arrays.asList("DX");
        record.kuratorstav = Arrays.asList("DX");
        return record;
    }

    private User user(String username, String email, String firstName, String lastName,
            boolean administratorskeNotifikace) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setJmeno(firstName);
        user.setPrijmeni(lastName);
        user.setAdministratorskeNotifikace(administratorskeNotifikace);
        return user;
    }

    private static class RealMailRecipientSKCJoinService extends SKCJoinServiceImpl {

        private RealMailRecipientSKCJoinService() {
            super("test", new JSONObject());
        }

        @Override
        protected List<Pair<String, String>> skcJoinRecipients() {
            String recipient = System.getProperty("sdnnt.test.mail.recipient");
            String recipientName = System.getProperty("sdnnt.test.mail.recipientName", "SDNNT test");
            if (recipient != null && !recipient.trim().isEmpty()) {
                return Collections.singletonList(Pair.of(recipient.trim(), recipientName));
            }
            return super.skcJoinRecipients();
        }

        @Override
        protected Pair<String, String> mailFrom() throws EmailException {
            JSONObject mail = Options.getInstance().getJSONObject("mail");
            if (mail == null) {
                throw new EmailException("mail configuration is missing");
            }
            String fromEmail = mail.getString("from.user");
            String fromName = mail.has("from.name") ? mail.getString("from.name") : fromEmail;
            return Pair.of(fromEmail, fromName);
        }

        @Override
        protected UserController buildUserController() {
            return null;
        }

        @Override
        protected UserController buildShibUsersController() {
            return null;
        }
    }
}
