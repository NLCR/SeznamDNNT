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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.services.MailService;

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
                .addMockedMethod("notificationEmailDelivery")
                .createMock();

        EasyMock.expect(service.notificationEmailDelivery())
                .andReturn("queue")
                .anyTimes();

        EasyMock.replay(service);

        service.collectSKC4SuccessorNotification(record("oai:aleph-nkp.cz:DNT01-000000001"), Case.SKC_1,
                Arrays.asList("oai:aleph-nkp.cz:DNT01-000000011"), "SKC_4");

        try (SolrClient client = SolrTestServer.getClient()) {
            service.deliverSKC4SuccessorNotifications(client);
            client.commit("notification_events");
        }

        QueryResponse response = prepare.getClient().query("notification_events",
                new SolrQuery("*:*")
                        .addFilterQuery("event_type:skc_join_successor_found")
                        .addFilterQuery("status:ready"));

        Assert.assertEquals(1, response.getResults().getNumFound());
        Assert.assertEquals("SKCJoinServiceImpl", response.getResults().get(0).getFieldValue("source"));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000001",
                response.getResults().get(0).getFieldValue("subject_id"));

        JSONObject payload = new JSONObject(response.getResults().get(0).getFieldValue("payload").toString());
        Assert.assertEquals("SKC_1", payload.getString("detected_case"));
        Assert.assertEquals("SKC_4", payload.getString("previous_comment"));
        Assert.assertEquals("oai:aleph-nkp.cz:DNT01-000000011", payload.getJSONArray("followers").getString(0));

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
                .addMockedMethod("notificationEmailDelivery")
                .createMock();

        EasyMock.expect(service.notificationEmailDelivery())
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
                .addMockedMethod("notificationEmailDelivery")
                .createMock();

        EasyMock.expect(service.notificationEmailDelivery())
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
                .addMockedMethod("notificationEmailDelivery")
                .addMockedMethod("buildMailService")
                .addMockedMethod("mailFrom")
                .addMockedMethod("skcJoinRecipients")
                .addMockedMethod("skcJoinSubject")
                .createMock();

        EasyMock.expect(service.notificationEmailDelivery())
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

    private MarcRecord record(String identifier) {
        MarcRecord record = new MarcRecord();
        record.identifier = identifier;
        record.fmt = "BK";
        record.license = "dnnto";
        record.dntstav = Arrays.asList("DX");
        record.kuratorstav = Arrays.asList("DX");
        return record;
    }
}
