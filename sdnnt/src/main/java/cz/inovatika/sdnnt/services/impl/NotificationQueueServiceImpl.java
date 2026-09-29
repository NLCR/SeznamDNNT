package cz.inovatika.sdnnt.services.impl;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.shib.ShibUsersControllerImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class NotificationQueueServiceImpl {

    public static final Logger LOGGER = Logger.getLogger(NotificationQueueServiceImpl.class.getName());

    private static final String COLLECTION = "notification_events";
    private static final String STATUS_READY = "ready";
    private static final String STATUS_SENT = "sent";
    private static final String STATUS_ERROR = "error";
    private static final String USER_NOTIFICATION_EMAIL = "user_notification_email";
    private static final String SKC_JOIN_SUCCESSOR_FOUND = "skc_join_successor_found";

    public void processQueue(int limit) throws IOException, SolrServerException {
        int rows = limit > 0 ? limit : 100;
        try (SolrClient client = buildClient()) {
            SolrDocumentList events = readReadyEvents(client, rows);
            List<SolrDocument> skcJoinEvents = new ArrayList<>();

            for (SolrDocument event : events) {
                String eventType = stringField(event, "event_type");
                if (USER_NOTIFICATION_EMAIL.equals(eventType)) {
                    processUserNotificationEmail(client, event);
                } else if (SKC_JOIN_SUCCESSOR_FOUND.equals(eventType)) {
                    skcJoinEvents.add(event);
                } else {
                    LOGGER.warning(String.format("Unsupported notification event type '%s' for event '%s'",
                            eventType, stringField(event, "id")));
                }
            }

            if (!skcJoinEvents.isEmpty()) {
                processSKCJoinNotificationEmails(client, skcJoinEvents);
            }

            SolrJUtilities.quietCommit(client, COLLECTION);
        }
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

    protected MailService buildMailService() {
        return new MailServiceImpl();
    }

    protected UserController buildUserController() {
        return new UserControlerImpl(null);
    }

    protected UserController buildShibUsersController() {
        return new ShibUsersControllerImpl();
    }

    private SolrDocumentList readReadyEvents(SolrClient client, int rows) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery("*:*")
                .addFilterQuery("status:" + STATUS_READY)
                .addFilterQuery("event_type:(" + USER_NOTIFICATION_EMAIL + " OR " + SKC_JOIN_SUCCESSOR_FOUND + ")")
                .setFields("id,event_type,payload,subject_id,origin_identifier,summary,created_at")
                .setSort("created_at", SolrQuery.ORDER.asc)
                .setRows(rows);
        QueryResponse response = client.query(COLLECTION, query);
        return response.getResults();
    }

    private void processUserNotificationEmail(SolrClient client, SolrDocument event) throws SolrServerException, IOException {
        String eventId = stringField(event, "id");
        try {
            JSONObject payload = payload(event);
            Pair<String, String> recipient = Pair.of(payload.getString("recipient"),
                    payload.optString("recipient_name", payload.getString("recipient")));
            buildMailService().sendNotificationEmail(recipient, documents(payload.optJSONArray("documents")));
            markEvent(client, eventId, STATUS_SENT);
            LOGGER.info(String.format("Sent queued user notification email for event '%s'", eventId));
        } catch (IOException | EmailException | RuntimeException e) {
            markEvent(client, eventId, STATUS_ERROR);
            LOGGER.log(Level.WARNING,
                    String.format("Problem with queued user notification email event '%s': %s", eventId, e.getMessage()),
                    e);
        }
    }

    private void processSKCJoinNotificationEmails(SolrClient client, List<SolrDocument> events)
            throws SolrServerException, IOException {
        List<Pair<String, String>> recipients = recipients(events);
        if (recipients.isEmpty()) {
            LOGGER.warning("Cannot send queued SKCJoin notification email. Missing recipient configuration.");
            for (SolrDocument event : events) {
                markEvent(client, stringField(event, "id"), STATUS_ERROR);
            }
            return;
        }

        try {
            buildMailService().sendMail(mailFrom(), recipients, skcJoinSubject(), skcJoinEmailBody(events));
            for (SolrDocument event : events) {
                markEvent(client, stringField(event, "id"), STATUS_SENT);
            }
            LOGGER.info(String.format("Sent queued SKCJoin notification email with %d events", events.size()));
        } catch (ServerException | EmailException | RuntimeException e) {
            for (SolrDocument event : events) {
                markEvent(client, stringField(event, "id"), STATUS_ERROR);
            }
            LOGGER.log(Level.WARNING,
                    String.format("Problem with queued SKCJoin notification email: %s", e.getMessage()), e);
        }
    }

    private List<Pair<String, String>> recipients(List<SolrDocument> events) {
        for (SolrDocument event : events) {
            JSONObject payload = payload(event);
            if (payload.has("recipient_target")) {
                return recipients(payload.getJSONObject("recipient_target"));
            }
        }
        return skcJoinRecipients();
    }

    private List<Pair<String, String>> recipients(JSONObject recipientTarget) {
        String type = recipientTarget.optString("type");
        if ("roles".equals(type)) {
            return roleRecipients(recipientTarget.optJSONArray("roles"));
        }
        LOGGER.warning(String.format("Unsupported recipient target type '%s'", type));
        return new ArrayList<>();
    }

    private List<Pair<String, String>> roleRecipients(JSONArray roles) {
        Map<String, Pair<String, String>> recipients = new LinkedHashMap<>();
        if (roles == null) {
            return new ArrayList<>();
        }

        UserController users = buildUserController();
        UserController shibUsers = buildShibUsersController();
        for (int i = 0; i < roles.length(); i++) {
            String roleName = roles.optString(i, null);
            if (!StringUtils.isAnyString(roleName)) {
                continue;
            }
            try {
                Role role = Role.valueOf(roleName);
                addRoleRecipients(recipients, users, role);
                addRoleRecipients(recipients, shibUsers, role);
            } catch (IllegalArgumentException e) {
                LOGGER.warning(String.format("Unsupported recipient role '%s'", roleName));
            } catch (UserControlerException e) {
                LOGGER.log(Level.WARNING,
                        String.format("Cannot resolve recipients for role '%s': %s", roleName, e.getMessage()), e);
            }
        }
        return new ArrayList<>(recipients.values());
    }

    private void addRoleRecipients(Map<String, Pair<String, String>> recipients, UserController controller, Role role)
            throws UserControlerException {
        if (controller == null) {
            return;
        }
        for (User user : controller.findUsersByRole(role)) {
            addRecipient(recipients, user);
        }
    }

    private void addRecipient(Map<String, Pair<String, String>> recipients, User user) {
        if (user == null || !user.isAdministratorskeNotifikace() || !StringUtils.isAnyString(user.getEmail())) {
            return;
        }
        String email = user.getEmail().trim();
        String name = recipientName(user);
        recipients.put(email.toLowerCase(), Pair.of(email, name));
    }

    private String recipientName(User user) {
        StringBuilder name = new StringBuilder();
        if (StringUtils.isAnyString(user.getJmeno())) {
            name.append(user.getJmeno().trim());
        }
        if (StringUtils.isAnyString(user.getPrijmeni())) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(user.getPrijmeni().trim());
        }
        if (name.length() > 0) {
            return name.toString();
        }
        return user.getEmail();
    }

    private void markEvent(SolrClient client, String eventId, String status) throws SolrServerException, IOException {
        Date now = new Date();
        SolrInputDocument update = new SolrInputDocument();
        update.setField("id", eventId);
        update.setField("status", atomicSet(status));
        update.setField("updated_at", atomicSet(now));
        if (STATUS_SENT.equals(status)) {
            update.setField("resolved_at", atomicSet(now));
        }
        client.add(COLLECTION, update);
    }

    private Map<String, Object> atomicSet(Object value) {
        Map<String, Object> atomic = new HashMap<>();
        atomic.put("set", value);
        return atomic;
    }

    private JSONObject payload(SolrDocument event) {
        return new JSONObject(stringField(event, "payload"));
    }

    private List<Map<String, String>> documents(JSONArray documents) {
        List<Map<String, String>> result = new ArrayList<>();
        if (documents == null) {
            return result;
        }
        for (int i = 0; i < documents.length(); i++) {
            JSONObject item = documents.getJSONObject(i);
            Map<String, String> map = new HashMap<>();
            for (String key : item.keySet()) {
                Object value = item.opt(key);
                if (value != null) {
                    map.put(key, value.toString());
                }
            }
            result.add(map);
        }
        return result;
    }

    protected Pair<String, String> mailFrom() throws EmailException {
        JSONObject mail = Options.getInstance().getJSONObject("mail");
        if (mail == null) {
            throw new EmailException("mail configuration is missing");
        }
        String fromEmail = mail.getString("from.user");
        String fromName = mail.has("from.name") ? mail.getString("from.name") : fromEmail;
        return Pair.of(fromEmail, fromName);
    }

    protected List<Pair<String, String>> skcJoinRecipients() {
        List<Pair<String, String>> recipients = new ArrayList<>();
        String recipient = Options.getInstance().stringKey("notificationemail.skc_join_recipient", null);
        if (recipient == null || recipient.trim().isEmpty()) {
            recipient = Options.getInstance().stringKey("OAI.adminEmail", null);
        }
        if (recipient == null || recipient.trim().isEmpty()) {
            return recipients;
        }

        String recipientName = Options.getInstance().stringKey("notificationemail.skc_join_recipient_name", "SDNNT");
        for (String email : recipient.split("[,;]")) {
            String trimmed = email.trim();
            if (!trimmed.isEmpty()) {
                recipients.add(Pair.of(trimmed, recipientName));
            }
        }
        return recipients;
    }

    protected String skcJoinSubject() {
        JSONObject notificationEmail = Options.getInstance().jsonObjKey("notificationemail");
        if (notificationEmail != null) {
            return notificationEmail.optString("skc_join_subject",
                    notificationEmail.optString("subject", "SKCJoin: nalezen naslednik"));
        }
        return "SKCJoin: nalezen naslednik";
    }

    private String skcJoinEmailBody(List<SolrDocument> events) {
        StringBuilder builder = new StringBuilder();
        builder.append("SKCJoin nasel nasledniky pro zaznamy ve stavu SKC_4/SKC_4b.\n\n");
        builder.append("Pocet zaznamu: ").append(events.size()).append("\n\n");

        for (SolrDocument event : events) {
            JSONObject payload = payload(event);
            builder.append("Identifier: ").append(payload.optString("identifier", stringField(event, "subject_id"))).append("\n");
            builder.append("Detekovany pripad: ").append(payload.optString("detected_case")).append("\n");
            builder.append("Predchozi komentar: ").append(payload.optString("previous_comment")).append("\n");
            builder.append("Followers: ").append(join(payload.optJSONArray("followers"))).append("\n");
            if (payload.has("fmt")) {
                builder.append("FMT: ").append(payload.optString("fmt")).append("\n");
            }
            if (payload.has("license")) {
                builder.append("License: ").append(payload.optString("license")).append("\n");
            }
            builder.append("\n");
        }

        return builder.toString();
    }

    private String join(JSONArray array) {
        if (array == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            values.add(array.optString(i));
        }
        return String.join(", ", values);
    }

    private String stringField(SolrDocument document, String fieldName) {
        Object value = document.getFieldValue(fieldName);
        return value != null ? value.toString() : "";
    }
}
