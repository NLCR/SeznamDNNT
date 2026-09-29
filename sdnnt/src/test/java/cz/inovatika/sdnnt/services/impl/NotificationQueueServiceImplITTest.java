package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class NotificationQueueServiceImplITTest {

    public static final Logger LOGGER = Logger.getLogger(NotificationQueueServiceImplITTest.class.getName());

    private static final String COLLECTION = "notification_events";

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("notification_queue");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores(COLLECTION);
    }

    @Test
    public void testProcessUserNotificationEmail() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        addUserNotificationEvent("user_event_1", "test@testovic.cz", "Test User",
                new JSONArray()
                        .put(new JSONObject()
                                .put("identifier", "oai:aleph-nkp.cz:DNT01-000057932")
                                .put("nazev", "Test title")
                                .put("dntstav", "A")
                                .put("license", "dnnto")));

        MailService mailService = EasyMock.createMock(MailService.class);
        mailService.sendNotificationEmail(EasyMock.isA(Pair.class), EasyMock.isA(List.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            @Override
            public Object answer() {
                Pair<String, String> recipient = (Pair<String, String>) EasyMock.getCurrentArguments()[0];
                List<Map<String, String>> documents = (List<Map<String, String>>) EasyMock.getCurrentArguments()[1];
                Assert.assertEquals("test@testovic.cz", recipient.getLeft());
                Assert.assertEquals("Test User", recipient.getRight());
                Assert.assertEquals(1, documents.size());
                Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000057932", documents.get(0).get("identifier"));
                Assert.assertEquals("dnnto", documents.get(0).get("license"));
                return null;
            }
        }).once();

        NotificationQueueServiceImpl service = queueService(mailService);
        EasyMock.replay(mailService, service);

        service.processQueue(100);

        Assert.assertEquals(1, countByStatus("sent"));
        Assert.assertEquals(0, countByStatus("ready"));
        Assert.assertNotNull(event("user_event_1").getFieldValue("resolved_at"));

        EasyMock.verify(mailService, service);
    }

    @Test
    public void testProcessSKCJoinNotificationsInOneEmail() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        addSKCJoinEvent("skc_event_1", "oai:aleph-nkp.cz:DNT01-000000001",
                new JSONArray().put("oai:aleph-nkp.cz:DNT01-000000011"));
        addSKCJoinEvent("skc_event_2", "oai:aleph-nkp.cz:DNT01-000000002",
                new JSONArray().put("oai:aleph-nkp.cz:DNT01-000000021").put("oai:aleph-nkp.cz:DNT01-000000022"));

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

        NotificationQueueServiceImpl service = queueService(mailService);
        EasyMock.replay(mailService, service);

        service.processQueue(100);

        Assert.assertEquals(2, countByStatus("sent"));
        Assert.assertEquals(0, countByStatus("ready"));

        EasyMock.verify(mailService, service);
    }

    @Test
    public void testProcessSKCJoinNotificationsResolvesCuratorRoles() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        addSKCJoinEventWithRoleTarget("skc_event_roles", "oai:aleph-nkp.cz:DNT01-000000001",
                new JSONArray().put("oai:aleph-nkp.cz:DNT01-000000011"));

        UserController users = EasyMock.createMock(UserController.class);
        UserController shibUsers = EasyMock.createMock(UserController.class);
        EasyMock.expect(users.findUsersByRole(Role.kurator))
                .andReturn(Arrays.asList(user("kurator1", "kurator@testovic.cz", "Karel", "Kurator")))
                .once();
        EasyMock.expect(shibUsers.findUsersByRole(Role.kurator))
                .andReturn(Arrays.asList(user("shibkurator", "kurator@testovic.cz", "Karel", "Kurator")))
                .once();
        EasyMock.expect(users.findUsersByRole(Role.mainKurator))
                .andReturn(Arrays.asList(user("mainkurator", "main@testovic.cz", "Marta", "Kuratorova")))
                .once();
        EasyMock.expect(shibUsers.findUsersByRole(Role.mainKurator))
                .andReturn(Collections.<User>emptyList())
                .once();

        MailService mailService = EasyMock.createMock(MailService.class);
        mailService.sendMail(EasyMock.isA(Pair.class), EasyMock.isA(List.class), EasyMock.isA(String.class),
                EasyMock.isA(String.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            @Override
            public Object answer() {
                List<Pair<String, String>> recipients = (List<Pair<String, String>>) EasyMock.getCurrentArguments()[1];
                Assert.assertEquals(2, recipients.size());
                Assert.assertEquals("kurator@testovic.cz", recipients.get(0).getLeft());
                Assert.assertEquals("Karel Kurator", recipients.get(0).getRight());
                Assert.assertEquals("main@testovic.cz", recipients.get(1).getLeft());
                Assert.assertEquals("Marta Kuratorova", recipients.get(1).getRight());
                return null;
            }
        }).once();

        NotificationQueueServiceImpl service = queueService(mailService, users, shibUsers);
        EasyMock.replay(mailService, users, shibUsers, service);

        service.processQueue(100);

        Assert.assertEquals(1, countByStatus("sent"));
        Assert.assertEquals(0, countByStatus("ready"));

        EasyMock.verify(mailService, users, shibUsers, service);
    }

    @Test
    public void testFailedUserNotificationEmailMarksEventAsError() throws Exception {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }

        addUserNotificationEvent("user_event_error", "test@testovic.cz", "Test User",
                new JSONArray()
                        .put(new JSONObject()
                                .put("identifier", "oai:aleph-nkp.cz:DNT01-000057932")));

        MailService mailService = EasyMock.createMock(MailService.class);
        mailService.sendNotificationEmail(EasyMock.isA(Pair.class), EasyMock.isA(List.class));
        EasyMock.expectLastCall().andThrow(new EmailException("smtp failure")).once();

        NotificationQueueServiceImpl service = queueService(mailService);
        EasyMock.replay(mailService, service);

        service.processQueue(100);

        Assert.assertEquals(1, countByStatus("error"));
        Assert.assertEquals(0, countByStatus("ready"));
        Assert.assertNull(event("user_event_error").getFieldValue("resolved_at"));

        EasyMock.verify(mailService, service);
    }

    private NotificationQueueServiceImpl queueService(MailService mailService) {
        NotificationQueueServiceImpl service = EasyMock.createMockBuilder(NotificationQueueServiceImpl.class)
                .addMockedMethod("buildClient")
                .addMockedMethod("buildMailService")
                .addMockedMethod("mailFrom")
                .addMockedMethod("skcJoinRecipients")
                .addMockedMethod("skcJoinSubject")
                .createMock();

        EasyMock.expect(service.buildClient())
                .andDelegateTo(new BuildSolrClientSupport())
                .anyTimes();
        EasyMock.expect(service.buildMailService())
                .andReturn(mailService)
                .anyTimes();
        try {
            EasyMock.expect(service.mailFrom())
                    .andReturn(Pair.of("from@testovic.cz", "SDNNT"))
                    .anyTimes();
        } catch (EmailException e) {
            throw new IllegalStateException(e);
        }
        EasyMock.expect(service.skcJoinRecipients())
                .andReturn(Collections.singletonList(Pair.of("kurator@testovic.cz", "Kurator")))
                .anyTimes();
        EasyMock.expect(service.skcJoinSubject())
                .andReturn("SKCJoin test")
                .anyTimes();
        return service;
    }

    private NotificationQueueServiceImpl queueService(MailService mailService, UserController users,
            UserController shibUsers) {
        NotificationQueueServiceImpl service = EasyMock.createMockBuilder(NotificationQueueServiceImpl.class)
                .addMockedMethod("buildClient")
                .addMockedMethod("buildMailService")
                .addMockedMethod("mailFrom")
                .addMockedMethod("skcJoinRecipients")
                .addMockedMethod("skcJoinSubject")
                .addMockedMethod("buildUserController")
                .addMockedMethod("buildShibUsersController")
                .createMock();

        EasyMock.expect(service.buildClient())
                .andDelegateTo(new BuildSolrClientSupport())
                .anyTimes();
        EasyMock.expect(service.buildMailService())
                .andReturn(mailService)
                .anyTimes();
        try {
            EasyMock.expect(service.mailFrom())
                    .andReturn(Pair.of("from@testovic.cz", "SDNNT"))
                    .anyTimes();
        } catch (EmailException e) {
            throw new IllegalStateException(e);
        }
        EasyMock.expect(service.skcJoinSubject())
                .andReturn("SKCJoin test")
                .anyTimes();
        EasyMock.expect(service.buildUserController())
                .andReturn(users)
                .anyTimes();
        EasyMock.expect(service.buildShibUsersController())
                .andReturn(shibUsers)
                .anyTimes();
        return service;
    }

    private void addUserNotificationEvent(String id, String recipient, String recipientName, JSONArray documents)
            throws SolrServerException, IOException {
        JSONObject payload = new JSONObject()
                .put("recipient", recipient)
                .put("recipient_name", recipientName)
                .put("username", "test1")
                .put("interval", "den")
                .put("documents", documents);

        addEvent(id, "email", "user_notification_email", "NotificationServiceImpl", "user", "test1", payload);
    }

    private void addSKCJoinEvent(String id, String identifier, JSONArray followers)
            throws SolrServerException, IOException {
        JSONObject payload = new JSONObject()
                .put("identifier", identifier)
                .put("detected_case", "SKC_1")
                .put("followers", followers)
                .put("previous_comment", "SKC_4")
                .put("fmt", "BK")
                .put("license", "dnnto");

        addEvent(id, "curator", "skc_join_successor_found", "SKCJoinServiceImpl", "catalog", identifier, payload);
    }

    private void addSKCJoinEventWithRoleTarget(String id, String identifier, JSONArray followers)
            throws SolrServerException, IOException {
        JSONObject payload = new JSONObject()
                .put("identifier", identifier)
                .put("detected_case", "SKC_1")
                .put("followers", followers)
                .put("previous_comment", "SKC_4")
                .put("fmt", "BK")
                .put("license", "dnnto")
                .put("recipient_target", new JSONObject()
                        .put("type", "roles")
                        .put("roles", new JSONArray()
                                .put("kurator")
                                .put("mainKurator")));

        addEvent(id, "curator", "skc_join_successor_found", "SKCJoinServiceImpl", "catalog", identifier, payload);
    }

    private void addEvent(String id, String channel, String eventType, String source, String subjectType,
            String subjectId, JSONObject payload) throws SolrServerException, IOException {
        SolrInputDocument document = new SolrInputDocument();
        document.setField("id", id);
        document.setField("channel", channel);
        document.setField("event_type", eventType);
        document.setField("event_key", id + "_key");
        document.setField("event_hash", id + "_hash");
        document.setField("source", source);
        document.setField("status", "ready");
        document.setField("created_at", new Date());
        document.setField("updated_at", new Date());
        document.setField("subject_type", subjectType);
        document.setField("subject_id", subjectId);
        document.setField("origin_identifier", subjectId);
        document.setField("title", id);
        document.setField("summary", id);
        document.setField("payload", payload.toString());

        prepare.getClient().add(COLLECTION, document);
        SolrJUtilities.quietCommit(prepare.getClient(), COLLECTION);
    }

    private long countByStatus(String status) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery("*:*")
                .addFilterQuery("status:" + status)
                .setRows(0);
        QueryResponse response = prepare.getClient().query(COLLECTION, query);
        return response.getResults().getNumFound();
    }

    private SolrDocument event(String id) throws SolrServerException, IOException {
        return prepare.getClient().getById(COLLECTION, id);
    }

    private User user(String username, String email, String firstName, String lastName) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setJmeno(firstName);
        user.setPrijmeni(lastName);
        return user;
    }

    protected class BuildSolrClientSupport extends NotificationQueueServiceImpl {

        @Override
        protected SolrClient buildClient() {
            return SolrTestServer.getClient();
        }
    }
}
