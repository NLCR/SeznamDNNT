package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;
import cz.inovatika.sdnnt.indexer.models.notifications.NotificationFactory;
import cz.inovatika.sdnnt.indexer.models.notifications.RuleNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.SimpleNotification;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class NotificationServiceImpl implements NotificationsService {

    static final Logger LOGGER = Logger.getLogger(NotificationServiceImpl.class.getName());

    private UserControler userControler;
    private MailService mailService;

    public NotificationServiceImpl(UserControler userControler, MailService mailService) {
        this.userControler = userControler;
        this.mailService = mailService;
    }

    @Override
    public List<AbstractNotification> findNotificationsByUser(String username) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                return iterateNotification(client, filterUserName(username));
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new NotificationsException(e);
        }
    }

    
    @Override
    public AbstractNotification findNotificationByUserAndId(String username, String id) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                List<AbstractNotification> iterateNotification = iterateNotification(client, filterUserNameAndId(username, id));
                if (!iterateNotification.isEmpty()) {
                    return iterateNotification.get(0);
                } else return null;
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new NotificationsException(e);
        }
    }

    @Override
    public List<AbstractNotification> findNotificationByInterval(NotificationInterval interval, TYPE type)
            throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                return iterateNotification(client, filterIntervalAndType(interval, type));
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new NotificationsException(e);
        }
    }

    @Override
    public List<AbstractNotification> findNotificationsByUserAndInterval(String username, NotificationInterval interval)
            throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                return iterateNotification(client, filterUserAndInterval(username, interval));
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new NotificationsException(e);
        }
    }

    @Override
    public List<AbstractNotification> findNotificationsByUserAndInterval(String username, NotificationInterval interval,
            TYPE type) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                List<AbstractNotification> iterateNotification = iterateNotification(client, filterUserIntervalAndType(username, interval, type));
                if (type.equals(TYPE.simple)) {
                    iterateNotification.addAll(iterateNotification(client, filterUserIntervalAndNotType(username, interval)));
                }
                return iterateNotification;
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new NotificationsException(e);
        }
    }

    @Override
    public List<AbstractNotification> findNotificationsByUser(String username, TYPE type)
            throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                List<AbstractNotification> iterateNotification = iterateNotification(client, filterUserNameAndType(username, type));
                if (type.equals(TYPE.simple)) {
                    iterateNotification.addAll(iterateNotification(client, filterUserAndNotType(username)));
                }
                return iterateNotification;
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new NotificationsException(e);
        }
    }

    private List<AbstractNotification> iterateNotification(SolrClient client, String filter)
            throws SolrServerException, IOException {

        List<AbstractNotification> notifications = new ArrayList<>();
        SolrQuery q = new SolrQuery(filter).setSort("id", SolrQuery.ORDER.desc).setRows(1000);

        iteration(client, "notifications", CursorMarkParams.CURSOR_MARK_START, q, (doc) -> {
            AbstractNotification notif = NotificationFactory.fromSolrDoc(doc);
            notifications.add(notif);
            return doc;
        });

        return notifications;
    }
    private String filterUserNameAndId(String username, String id) {
        return "user:" + username + " AND id:" + id;
    }

    private String filterUserName(String username) {
        return "user:" + username;
    }

    private String filterUserAndNotType(String username) {
        return "user:" + username + " AND -type:*";
    }
    private String filterUserNameAndType(String username, TYPE type) {
        return "user:" + username + " AND type:" + type.name();
    }

    private String filterIntervalAndType(NotificationInterval interval, TYPE type) {
        return "periodicity:" + interval + " AND type:" + type.name();
    }
    
    private String filterUserIntervalAndNotType(String username, NotificationInterval interval) {
        return "user:" + username + " AND "+ "periodicity:" + interval + " AND -type:*";
    }
    
    private String filterUserIntervalAndType(String username, NotificationInterval interval, TYPE type) {
        return "user:" + username + " AND "+ "periodicity:" + interval + " AND type:" + type.name();
    }

    private String filterUserAndInterval(String username, NotificationInterval interval) {
        return "user:" + username + " AND "+ "periodicity:" + interval;
    }
    
    @Override
    public List<AbstractNotification> findNotificationsByInterval(NotificationInterval interval)
            throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                List<AbstractNotification> notifications = new ArrayList<>();
                SolrQuery q = new SolrQuery("periodicity:" + interval.name()).setSort("id", SolrQuery.ORDER.desc)
                        .setRows(1000);

                iteration(client, "notifications", CursorMarkParams.CURSOR_MARK_START, q, (doc) -> {
                    AbstractNotification notif = NotificationFactory.fromSolrDoc(doc);
                    notifications.add(notif);
                    return doc;
                });
                return notifications;
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            throw new NotificationsException(e);
        }
    }

    @Override
    public SimpleNotification saveSimpleNotification(SimpleNotification notification) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                notification.makeSureId();
                client.add("notifications", notification.toSolrDocument());
                //client.commit();
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
            
            SolrDocument sDoc = client.getById("notifications",notification.getId());
            return (SimpleNotification) NotificationFactory.fromSolrDoc(sDoc);
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return null;
    }


    @Override
    public void processNotifications(NotificationInterval interval)
            throws NotificationsException, UserControlerException {
        // notifikace ?? 
        // vzit vsechny uzivatele, kteri maji nastaveny interval 
        List<User> users = userControler.findUsersByNotificationInterval(interval.name());
        // prochazim uzivatele; beru jednoduche notifikace
        users.stream().forEach(user -> {
            final List<Map<String, String>> documents = new ArrayList<>();
            try {
                List<String> docIdents = new ArrayList<>();

                List<Map<String,String>> simpleNotifications = processSimpleNotification(user, interval);
                simpleNotifications.stream().forEach(doc-> {
                    String ident = doc.get(MarcRecordFields.IDENTIFIER_FIELD);
                    if (!docIdents.contains(ident)) {
                        docIdents.add(ident);
                        documents.add(doc);
                    }
                });

                List<Map<String, String>> ruleNotifications = processRuleBasedNotification(user, interval);
                ruleNotifications.stream().forEach(doc-> {
                    String ident = doc.get(MarcRecordFields.IDENTIFIER_FIELD);
                    if (!docIdents.contains(ident)) {
                        docIdents.add(ident);
                        documents.add(doc);
                    }
                });
                if (!documents.isEmpty()) {
                    sendEmail(interval, user, new ArrayList<>(documents));
                }
            } catch (UserControlerException | NotificationsException | IOException e) {
               LOGGER.log(Level.SEVERE, e.getMessage(),e);
            }
        });
    }

    protected List<Map<String, String>> processRuleBasedNotification(User user, NotificationInterval interval) throws NotificationsException, IOException {
        // vlastni notifikace - join
        List<AbstractNotification> notificationsByInterval = this.findNotificationsByUserAndInterval(user.getUsername(), interval, TYPE.rule);
        String  fqCatalog = fCatalogFilter(interval);
        final List<Map<String, String>> documents = new ArrayList<>();
        try (SolrClient client = buildClient()) {
            
            notificationsByInterval.stream().forEach(notif-> {
                RuleNotification ruleNotification = (RuleNotification) notif;
                SolrQuery q = new SolrQuery("*").setRows(1000).setSort("identifier", SolrQuery.ORDER.desc)
                        .addFilterQuery(fqCatalog)
                        .addFilterQuery(ruleNotification.provideProcessQueryFilters())
                        .setFields("identifier,datum_stavu,nazev,dntstav,license,historie_stavu");

                try {
                    iteration(client, "catalog", CursorMarkParams.CURSOR_MARK_START, q, (doc) -> {

                        Collection<Object> dntstav = doc.getFieldValues("dntstav");
                        Collection<Object> license = doc.getFieldValues("license");
                        String historieStavu = doc.containsKey("historie_stavu") ?  (String) doc.getFieldValue("historie_stavu") : null;
                                               
                        
                        Map<String, String> map = new HashMap<>();
                        map.put("nazev", (String) doc.getFirstValue("nazev"));
                        map.put("dntstav",
                                dntstav.size() == 1 ? (String) new ArrayList<>(dntstav).get(0) : dntstav.toString());
                        if ( license != null) {
                            map.put("license",
                                    license.size() == 1 ? (String) new ArrayList<>(license).get(0) : license.toString());
                            
                        }
                        map.put("identifier", doc.getFieldValue("identifier").toString());
                        if (historieStavu != null) {
                            map.put("historie_stavu", historieStavu);
                        }
                        if (ruleNotification.accept(map)) {
                            documents.add(map);
                        }

                        return doc;
                    });
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
        }
        return documents;
    }
    
    
    protected List<Map<String,String>> processSimpleNotification(User user, NotificationInterval interval)
            throws UserControlerException, NotificationsException {
        final List<Map<String, String>> documents = new ArrayList<>();
        try (SolrClient client = buildClient()) {

            //String fqCatalog;
            String fqJoin = "{!join fromIndex=notifications from=identifier to=identifier} user:"
                    + user.getUsername();
            String  fqCatalog = fCatalogFilter(interval);
            try {
                SolrQuery q = new SolrQuery("*").setRows(1000).setSort("identifier", SolrQuery.ORDER.desc)
                        .addFilterQuery(fqCatalog).addFilterQuery(fqJoin)
                        .setFields("identifier,datum_stavu,nazev,dntstav");
                iteration(client, "catalog", CursorMarkParams.CURSOR_MARK_START, q, (doc) -> {

                    Collection<Object> dntstav = doc.getFieldValues("dntstav");
                    Collection<Object> license = doc.getFieldValues("license");

                    Map<String, String> map = new HashMap<>();
                    map.put("nazev", (String) doc.getFirstValue("nazev"));
                    map.put("dntstav",
                            dntstav.size() == 1 ? (String) new ArrayList<>(dntstav).get(0) : dntstav.toString());
                    if ( license != null) {
                        map.put("license",
                                license.size() == 1 ? (String) new ArrayList<>(license).get(0) : license.toString());
                        
                    }

                    map.put("identifier", doc.getFieldValue("identifier").toString());
                    documents.add(map);

                    return doc;
                });
                LOGGER.log(Level.INFO, "checkNotifications finished");
            } catch (SolrServerException | IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } catch (IOException e) {
            throw new NotificationsException(e);
        }
        return documents;
    }

    private String fCatalogFilter(NotificationInterval interval) {
        String fqCatalog;
        switch (interval) {
        case den:
            fqCatalog = "datum_stavu:[NOW/DAY-1DAY TO NOW]";
            break;
        case tyden:
            fqCatalog = "datum_stavu:[NOW/DAY-7DAYS TO NOW]";
            break;
        default:
            fqCatalog = "datum_stavu:[NOW/MONTH-1MONTH TO NOW]";
        }
        return fqCatalog;
    }

    private void sendEmail(NotificationInterval interval, User user, final List<Map<String, String>> documents) {
        if (!documents.isEmpty()) {
            LOGGER.info(String.format(
                    "Processing notification '%s', for user '%s' with email '%s'. Number of documents %d",
                    interval.name(), user.getJmeno() + " " + user.getPrijmeni(), user.getEmail(),
                    documents.size()));
            Pair<String, String> pair = Pair.of(user.getEmail(),
                    user.getJmeno() + " " + user.getPrijmeni());
            try {
                mailService.sendNotificationEmail(pair, documents);
            } catch (IOException | EmailException e) {
                LOGGER.log(Level.WARNING, String.format("Problem with sending email to %s due %s",
                        e.getMessage(), user.getEmail()));
                LOGGER.throwing(this.getClass().getName(), "processNotifications", e);
            }
        } else {
            LOGGER.info(String.format("No changed documents for user  %s and interval %s", user, interval));
        }
    }

    private void iteration(SolrClient client, String collection, String cursorMark, SolrQuery q,
            Function<SolrDocument, SolrDocument> function) throws SolrServerException, IOException {
        boolean done = false;
        while (!done) {
            q.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse qr = client.query(collection, q);
            String nextCursorMark = qr.getNextCursorMark();
            SolrDocumentList docs = qr.getResults();

            for (SolrDocument doc : docs) {
                function.apply(doc);
            }

            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            }
            cursorMark = nextCursorMark;
        }
    }

    SolrClient buildClient() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

    @Override
    public RuleNotification saveNotificationRule(RuleNotification notificationRule) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                notificationRule.makeSureId();
                client.add("notifications", notificationRule.toSolrDocument());
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
            SolrDocument sDoc = client.getById("notifications",notificationRule.getId());
            return (RuleNotification) NotificationFactory.fromSolrDoc(sDoc);

        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return null;
        }

    }

    @Override
    public void deleteRuleNotifications(List<RuleNotification> ruleNotifications) throws NotificationsException {
        List<String> ids = ruleNotifications.stream().map(RuleNotification::getId).collect(Collectors.toList());
        try (SolrClient client = buildClient()) {
            client.deleteById("notifications", ids,100);
        } catch (IOException | SolrServerException  e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        //public UpdateResponse deleteById(String collection, List<String> ids, int commitWithinMs) throws SolrServerException, IOException {

    }
    
    
}
