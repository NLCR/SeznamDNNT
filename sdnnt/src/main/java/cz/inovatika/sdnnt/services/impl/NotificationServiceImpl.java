package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.indexer.models.Notification;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationServiceImpl implements NotificationsService  {


    static final Logger LOGGER = Logger.getLogger(NotificationServiceImpl.class.getName());

    private UserControler userControler;
    private MailService mailService;

    public NotificationServiceImpl(UserControler userControler, MailService mailService) {
        this.userControler = userControler;
        this.mailService = mailService;
    }


    @Override
    public List<Notification> findNotificationsByUser(String username) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                List<Notification> notifications = new ArrayList<>();
                SolrQuery q = new SolrQuery("user:"+username)
                        .setSort("id", SolrQuery.ORDER.desc)
                        .setRows(1000);


                iteration(client, "notifications",  CursorMarkParams.CURSOR_MARK_START, q, (doc)->{
                    notifications.add(fromSolrDoc(doc));
                    return doc;
                });

                return notifications;
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE,e.getMessage());
            throw new NotificationsException(e);
        }
    }

    private Notification fromSolrDoc(SolrDocument doc) {
        Notification notif = new Notification();
        notif.setId((String) doc.getFieldValue("id"));
        notif.setIdentifier((String) doc.getFieldValue("identifier"));
        notif.setUser((String) doc.getFieldValue("user"));
        notif.setPeriodicity((String) doc.getFieldValue("periodicity"));
        notif.setIndextime((Date) doc.getFieldValue("indextime"));
        return notif;
    }

    @Override
    public List<Notification> findNotificationsByInterval(NotificationInterval interval) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                List<Notification> notifications = new ArrayList<>();
                SolrQuery q = new SolrQuery("periodicity:"+interval.name())
                        .setSort("id", SolrQuery.ORDER.desc).setRows(1000);

                iteration(client, "notifications",  CursorMarkParams.CURSOR_MARK_START, q, (doc)->{
                    notifications.add(fromSolrDoc(doc));
                    return doc;
                });

                return notifications;
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE,e.getMessage());
            throw new NotificationsException(e);
        }
    }

    @Override
    public void saveNotification(Notification notification) throws NotificationsException {
        try (SolrClient client = buildClient()) {
            try {
                client.addBean("notifications", notification);
            } finally {
                SolrJUtilities.quietCommit(client, "notifications");
            }
        } catch (IOException | SolrServerException e) {
            LOGGER.log(Level.SEVERE,e.getMessage());
        }
    }

    @Override
    public void processNotifications(NotificationInterval interval) throws NotificationsException, UserControlerException {
        try (SolrClient client = buildClient()){
            List<User> users = userControler.findUsersByNotificationInterval(interval.name());
            users.stream().forEach(user-> {
                String fqCatalog;
                String fqJoin = "{!join fromIndex=notifications from=identifier to=identifier} user:"+user.getUsername();
                switch(interval) {
                    case den:
                        fqCatalog = "datum_stavu:[NOW/DAY-1DAY TO NOW]";
                        break;
                    case tyden:
                        fqCatalog = "datum_stavu:[NOW/DAY-7DAYS TO NOW]";
                        break;
                    default:
                        fqCatalog = "datum_stavu:[NOW/MONTH-1MONTH TO NOW]";
                }
                try {
                    final List<Map<String,String>> documents = new ArrayList<>();
                    SolrQuery q = new SolrQuery("*").setRows(1000)
                            .setSort("identifier", SolrQuery.ORDER.desc)
                            .addFilterQuery(fqCatalog)
                            .addFilterQuery(fqJoin)
                            .setFields("identifier,datum_stavu,nazev,dntstav");
                    iteration(client,"catalog",  CursorMarkParams.CURSOR_MARK_START, q, (doc) ->{

                        Collection<Object> dntstav = doc.getFieldValues("dntstav");
                        Map<String,String> map = new HashMap<>();
                        map.put("nazev", (String) doc.getFirstValue("nazev"));
                        map.put("dntstav", dntstav.size() == 1 ? (String)new ArrayList<>(dntstav).get(0): dntstav.toString());
                        map.put("identifier", doc.getFieldValue("identifier").toString());
                        documents.add(map);

                        return doc;
                    });
                    LOGGER.log(Level.INFO, "checkNotifications finished");

                    if (!documents.isEmpty()) {
                        LOGGER.info(String.format("Processing notification '%s', for user '%s' with email '%s'. Number of documents %d", interval.name(), user.getJmeno()+" "+user.getPrijmeni(), user.getEmail(), documents.size()));
                        Pair<String, String> pair = Pair.of(user.getEmail(), user.getJmeno() + " " + user.getPrijmeni());
                        try {
                            mailService.sendNotificationEmail(pair, documents);
                        } catch (IOException | EmailException e) {
                            LOGGER.log(Level.WARNING, String.format("Problem with sending email to %s due %s", e.getMessage(), user.getEmail()));
                            LOGGER.throwing(this.getClass().getName(), "processNotifications",e);
                        }
                    } else {
                        LOGGER.info(String.format("No changed documents for user  %s and interval %s", user ,interval));
                    }
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    //throw new NotificationsException(ex);
                }

            });
        } catch (IOException e) {
            throw new NotificationsException(e);
        }

    }

    private void iteration(SolrClient client, String collection, String cursorMark, SolrQuery q, Function<SolrDocument, SolrDocument> function) throws SolrServerException, IOException {
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

}
