package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.FOLLOWERS;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.HISTORIE_STAVU_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateSKCUtils;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.SKCDeleteService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.services.impl.shib.ShibUsersControllerImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class SKCJoinServiceImpl extends AbstractCheckDeleteService implements SKCDeleteService {

    protected Logger logger = Logger.getLogger(SKCTypeServiceImpl.class.getName());
    private final List<SKCJoinSuccessorNotification> skcJoinSuccessorNotifications = new ArrayList<>();

    public SKCJoinServiceImpl(String loggerName, JSONObject results) {
        super(loggerName, results);
        if (loggerName != null) {
            this.logger = Logger.getLogger(loggerName);
        }
    }

    @Override
    public void updateDeleteInfo(List<String> deleteInfo) {
        // TODO Auto-generated method stub
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }
    
    protected Options getOptions() {
        return Options.getInstance();
    }

    protected MailService buildMailService() {
        return new MailServiceImpl();
    }

    protected UserController buildUserController() {
        return new UserControlerImpl(null);
    }


    @Override
    protected Map<Case, List<Pair<String, List<String>>>> checkUpdate() throws IOException, SolrServerException {
        Map<Case, List<Pair<String, List<String>>>> retvals = new HashMap<>();
        skcJoinSuccessorNotifications.clear();
        try {
            CatalogIterationSupport support = new CatalogIterationSupport();
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "10000");
            try (final SolrClient solrClient = buildClient()) {
                noFollowers(retvals, support, reqMap, solrClient);
            } catch(IOException e) {
                getLogger().log(Level.SEVERE,e.getMessage(),e);
            }
        } catch (Exception  e) {
            getLogger().log(Level.SEVERE,e.getMessage(),e);
        }
        return retvals;
    }

    private void noFollowers(Map<Case, List<Pair<String, List<String>>>> retvals, CatalogIterationSupport support,
        Map<String, String> reqMap, final SolrClient solrClient) {
        List<String> plusFilter = Arrays.asList(
                KURATORSTAV_FIELD + ":DX"
        );
        
        List<String> minusFilter = Arrays.asList(
                FOLLOWERS + ":*"
        );
        
        support.iterate(solrClient, reqMap, null, plusFilter, minusFilter, 
                Arrays.asList(
                        MarcRecordFields.IDENTIFIER_FIELD,
                        MarcRecordFields.FMT_FIELD, 
                        "place_of_pub",
                        KURATORSTAV_FIELD,
                        HISTORIE_STAVU_FIELD,
                        HISTORIE_KURATORSTAVU_FIELD
                        ), (rsp) -> {

            String fieldValue = (String) rsp.getFirstValue(HISTORIE_KURATORSTAVU_FIELD);
            JSONArray jsonArray = new JSONArray(fieldValue);
            List<String> comments = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jObject = jsonArray.getJSONObject(i);
                String state = jObject.optString("stav");
                String comment = jObject.optString("comment");
                if (state.equals("DX")) {
                    if (comment != null) {
                        if (comment.startsWith("scheduler/")) {
                            comments.add(comment.substring("scheduler/".length()));
                        }
                    }
                }
            }
            // last is SKC_4b
            if (comments.size() >= 1 && (lastComment(comments).equals("SKC_4b") || lastComment(comments).equals("SKC_4"))) {
                try {
                    String identifier = (String) rsp.getFieldValue(MarcRecordFields.IDENTIFIER_FIELD);
                    SolrDocument document = solrClient.getById(DataCollections.catalog.name(),identifier);
                    MarcRecord fromIndex = MarcRecord.fromSolrDoc(document);
                    Pair<Case,List<String>> followers = DuplicateSKCUtils.findSKCFollowers(solrClient, fromIndex);
                    if (!followers.getKey().equals(Case.SKC_4a) && !followers.getKey().equals(Case.SKC_4b)) {
                        if (!retvals.containsKey(followers.getKey())) {
                            retvals.put(followers.getKey(),new ArrayList<>());
                        }
                        retvals.get(followers.getKey()).add(Pair.of(fromIndex.identifier, followers.getRight()));
                        collectSKC4SuccessorNotification(fromIndex, followers.getKey(), followers.getRight(),
                                lastComment(comments));
                    } else {
                        getLogger().log(Level.INFO, String.format(" SKC_4 again %s", identifier));
                    }
                } catch (SolrException | SolrServerException | IOException e) {
                    getLogger().log(Level.SEVERE, e.getMessage(),e);
                }
//            } else {
//                getLogger().log(Level.INFO, String.format(" TEST "));
            }
        }, IDENTIFIER_FIELD);
        
    }

    private String lastComment(List<String> comments) {
        return comments.get(comments.size() -1);
    }

    protected void collectSKC4SuccessorNotification(MarcRecord record, Case detectedCase, List<String> followers,
            String previousComment) {
        String eventKey = "skc_join_successor|" + detectedCase.name() + "|" + record.identifier + "|"
                + String.join(",", followers);
        skcJoinSuccessorNotifications.add(new SKCJoinSuccessorNotification(record, detectedCase, followers,
                previousComment, eventKey));
    }

    protected void deliverSKC4SuccessorNotifications(SolrClient solrClient)
            throws SolrServerException, IOException {
        if (skcJoinSuccessorNotifications.isEmpty()) {
            return;
        }
        if ("queue".equalsIgnoreCase(notificationAdminEmailDelivery())) {
            for (SKCJoinSuccessorNotification notification : skcJoinSuccessorNotifications) {
                queueSKC4SuccessorNotification(solrClient, notification);
            }
        } else {
            sendSKC4SuccessorNotificationEmail(skcJoinSuccessorNotifications);
        }
    }

    protected String notificationAdminEmailDelivery() {
        return getOptions().stringKey("notificationadminemail.delivery", "direct");
    }



    private boolean hasOpenSKC4SuccessorNotification(SolrClient solrClient, String eventKey)
            throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery("*:*")
                .addFilterQuery("event_type:skc_join_successor_found")
                .addFilterQuery("event_key:\"" + eventKey + "\"")
                .addFilterQuery("status:ready")
                .setRows(0);
        QueryResponse response = solrClient.query("notification_events", query);
        SolrDocumentList results = response.getResults();
        return results != null && results.getNumFound() > 0;
    }

    private SolrInputDocument skc4SuccessorNotificationEvent(MarcRecord record, Case detectedCase,
            List<String> followers, String previousComment, String eventKey) {
        Date now = new Date();
        JSONObject payload = new JSONObject();
        payload.put("identifier", record.identifier);
        payload.put("detected_case", detectedCase.name());
        payload.put("followers", new JSONArray(followers));
        payload.put("previous_comment", previousComment);
        payload.put("fmt", record.fmt);
        payload.put("dntstav", record.dntstav != null ? record.dntstav.get(0) : "");
        payload.put("kuratorstav", record.kuratorstav != null ? record.kuratorstav.get(0) : "");
        payload.put("license", record.license);
        if (record.historie_kurator_stavu != null) {
            payload.put("historie_kurator_stavu", record.historie_kurator_stavu);
        }
        payload.put("recipient_target", new JSONObject()
                .put("type", "roles")
                .put("roles", new JSONArray()
                        .put("kurator")
                        .put("mainKurator")));

        SolrInputDocument document = new SolrInputDocument();
        document.setField("id", "skc_join_" + UUID.randomUUID().toString());
        document.setField("channel", "curator");
        document.setField("event_type", "skc_join_successor_found");
        document.setField("event_key", eventKey);
        document.setField("event_hash", eventKey);
        document.setField("source", "SKCJoinServiceImpl");
        document.setField("status", "ready");
        document.setField("created_at", now);
        document.setField("updated_at", now);
        document.setField("subject_type", DataCollections.catalog.name());
        document.setField("subject_id", record.identifier);
        document.setField("origin_identifier", record.identifier);
        document.setField("title", "SKCJoin: nalezen následník");
        document.setField("summary", String.format("Záznam %s měl stav %s a nyní byl nalezen následník: %s.",
                record.identifier, previousComment, String.join(", ", followers)));
        document.setField("payload", payload.toString());
        return document;
    }

    private void queueSKC4SuccessorNotification(SolrClient solrClient, SKCJoinSuccessorNotification notification)
            throws SolrServerException, IOException {
        if (hasOpenSKC4SuccessorNotification(solrClient, notification.eventKey)) {
            getLogger().log(Level.INFO,
                    String.format("SKC_4 successor notification already queued for %s",
                            notification.record.identifier));
            return;
        }
        SolrInputDocument document = skc4SuccessorNotificationEvent(notification.record, notification.detectedCase,
                notification.followers, notification.previousComment, notification.eventKey);
        solrClient.add("notification_events", document);
    }

    private void sendSKC4SuccessorNotificationEmail(List<SKCJoinSuccessorNotification> notifications) {
        List<Pair<String, String>> recipients = skcJoinRecipients();
        if (recipients.isEmpty()) {
            getLogger().warning("Cannot send SKCJoin notification email. Missing recipient configuration.");
            return;
        }

        try {
            MailService mailService = buildMailService();
            Pair<String, String> mailFrom = mailFrom();
            String mailSubject = skcJoinSubject();
            String mailBody = skcJoinEmailBody(notifications);

            mailService.sendMail(mailFrom, recipients, mailSubject, mailBody);
            getLogger().info(String.format("Sent SKCJoin successor notification email with %d records",
                    notifications.size()));
        } catch (IOException | EmailException e) {
            getLogger().log(Level.WARNING,
                    String.format("Problem with sending SKCJoin notification email due %s", e.getMessage()), e);
        }
    }

    protected Pair<String, String> mailFrom() throws EmailException {
        JSONObject mail = getOptions().getJSONObject("mail");
        if (mail == null) {
            throw new EmailException("mail configuration is missing");
        }
        String fromEmail = mail.getString("from.user");
        String fromName = mail.has("from.name") ? mail.getString("from.name") : fromEmail;
        return Pair.of(fromEmail, fromName);
    }

    protected List<Pair<String, String>> skcJoinRecipients() {
        AdministratorRecipients administratorRecipients = administratorRecipients();
        if (administratorRecipients.roleUsersFound) {
            return administratorRecipients.recipients;
        }

        List<Pair<String, String>> recipients = new ArrayList<>();
        String recipient = getOptions().stringKey("notificationemail.skc_join_recipient", null);

        String recipientName = getOptions().stringKey("notificationemail.skc_join_recipient_name", "SDNNT");
        if (recipient !=  null) {
            for (String email : recipient.split("[,;]")) {
                String trimmed = email.trim();
                if (!trimmed.isEmpty()) {
                    recipients.add(Pair.of(trimmed, recipientName));
                }
            }
        }
        return recipients;
    }

    private AdministratorRecipients administratorRecipients() {
        Map<String, Pair<String, String>> recipients = new LinkedHashMap<>();
        boolean roleUsersFound = false;

        for (Role role : Arrays.asList(Role.kurator, Role.mainKurator)) {
            roleUsersFound = addAdministratorRecipients(recipients, buildUserController(), role) || roleUsersFound;
        }

        return new AdministratorRecipients(new ArrayList<>(recipients.values()), roleUsersFound);
    }

    private boolean addAdministratorRecipients(Map<String, Pair<String, String>> recipients, UserController controller,
            Role role) {
        if (controller == null) {
            return false;
        }
        try {
            List<User> users = controller.findUsersByRole(role);
            for (User user : users) {
                addAdministratorRecipient(recipients, user);
            }
            return !users.isEmpty();
        } catch (UserControlerException e) {
            getLogger().log(Level.WARNING,
                    String.format("Cannot resolve SKCJoin recipients for role '%s': %s", role.name(), e.getMessage()),
                    e);
            return false;
        }
    }

    private void addAdministratorRecipient(Map<String, Pair<String, String>> recipients, User user) {
        if (user == null || !user.isAdministratorskeNotifikace() || !StringUtils.isAnyString(user.getEmail())) {
            return;
        }
        String email = user.getEmail().trim();
        recipients.put(email.toLowerCase(), Pair.of(email, recipientName(user)));
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

    protected String skcJoinSubject() {
        JSONObject notificationEmail = getOptions().jsonObjKey("notificationemail");
        if (notificationEmail != null) {
            return notificationEmail.optString("skc_join_subject",
                    notificationEmail.optString("subject", "SKCJoin: nalezen naslednik"));
        }
        return "SKCJoin: nalezen naslednik";
    }

    private String skcJoinEmailBody(List<SKCJoinSuccessorNotification> notifications) {
        StringBuilder builder = new StringBuilder();
        builder.append("SKCJoin nasel nasledniky pro zaznamy ve stavu SKC_4/SKC_4b.\n\n");
        builder.append("Pocet zaznamu: ").append(notifications.size()).append("\n\n");

        for (SKCJoinSuccessorNotification notification : notifications) {
            builder.append("Identifier: ").append(notification.record.identifier).append("\n");
            builder.append("Detekovany pripad: ").append(notification.detectedCase.name()).append("\n");
            builder.append("Predchozi komentar: ").append(notification.previousComment).append("\n");
            builder.append("Followers: ").append(String.join(", ", notification.followers)).append("\n");
            /*
            if (notification.record.fmt != null) {
                builder.append("FMT: ").append(notification.record.fmt).append("\n");
            }
            if (notification.record.license != null) {
                builder.append("License: ").append(notification.record.license).append("\n");
            }*/
            builder.append("\n");
        }

        return builder.toString();
    }

    @Override
    protected List<String> checkDelete() throws IOException, SolrServerException {
        // TODO Auto-generated method stub
        return new ArrayList<>();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }
    
    
    public void updateFollowers() throws IOException {
        try {

            Map<Case,List<Pair<String,List<String>>>> cases = checkUpdate();
            getLogger().log(Level.INFO, String.format("Cases %s", cases.keySet().toString()));
            for (Case cs : cases.keySet()) {
                //String confProc = getProcess(cs);
                List<Pair<String, List<String>>> updates = cases.get(cs);
                getLogger().log(Level.INFO, String.format("Updating records, Case %s and number of records %d", cs.name(), updates.size()));
                switch (cs) {
                    case SKC_1:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());

                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                    case SKC_2a:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());
                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                    case SKC_2b:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());
                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                    case SKC_3:
                        try (SolrClient solrClient = buildClient()) {
                            for (int i = 0; i < updates.size(); i++) {
                                Pair<String, List<String>> pair = updates.get(i);
                                SolrDocument doc = solrClient.getById(DataCollections.catalog.name(), pair.getKey());
                                MarcRecord origin = MarcRecord.fromSolrDoc(doc);
                                origin.followers = pair.getRight();
                                // zmena stavu - nutno 
                                SolrInputDocument document = ChangeProcessStatesUtility.changeProcessState(CuratorItemState.DX.name(), origin,"scheduler", "scheduler/"+cs);
                                solrClient.add(DataCollections.catalog.name(), document);
                                getLogger().info("Updating id "+origin.identifier+" with followers "+origin.followers);
                            }
                        }
                        break;
                        
                    default:
                        break;
                }
            }
            //updateRecords(checkUpdate());
            //deleteRecords(checkDelete());
        } catch(Exception e) {
            getLogger().log(Level.SEVERE,e.getMessage(),e);
        } finally {
            try (SolrClient solrClient = buildClient()) {
                try {
                    deliverSKC4SuccessorNotifications(solrClient);
                } catch (SolrServerException e) {
                    getLogger().log(Level.WARNING, e.getMessage(), e);
                }
                SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
                SolrJUtilities.quietCommit(solrClient, DataCollections.zadost.name());
                SolrJUtilities.quietCommit(solrClient, "notification_events");
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        SKCJoinServiceImpl joinService = new SKCJoinServiceImpl("tt", null);
        joinService.updateFollowers();
    }

    private static class SKCJoinSuccessorNotification {
        private final MarcRecord record;
        private final Case detectedCase;
        private final List<String> followers;
        private final String previousComment;
        private final String eventKey;

        private SKCJoinSuccessorNotification(MarcRecord record, Case detectedCase, List<String> followers,
                String previousComment, String eventKey) {
            this.record = record;
            this.detectedCase = detectedCase;
            this.followers = new ArrayList<>(followers);
            this.previousComment = previousComment;
            this.eventKey = eventKey;
        }
    }

    private static class AdministratorRecipients {
        private final List<Pair<String, String>> recipients;
        private final boolean roleUsersFound;

        private AdministratorRecipients(List<Pair<String, String>> recipients, boolean roleUsersFound) {
            this.recipients = recipients;
            this.roleUsersFound = roleUsersFound;
        }
    }
    
    
}
