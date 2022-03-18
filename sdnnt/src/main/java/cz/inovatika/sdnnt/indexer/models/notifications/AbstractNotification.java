package cz.inovatika.sdnnt.indexer.models.notifications;

import java.util.Date;
import java.util.UUID;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;

public abstract class AbstractNotification {
    
    /**
     * Type of notification
     * @author happy
     *
     */
    public static enum TYPE {
        /** Simple, per document notification */
        simple, 
        /** Rule notification; set of rules*/
        rule;

        public static TYPE find(String str) {
            TYPE[] values = TYPE.values();
            for (TYPE t : values) {
                if (t.name().equals(str)) {
                    return t;
                }
            }
            return simple;
        }
    }

    public static final String USER_KEY = "user";
    public static final String PERIODICITY_KEY = "periodicity";
    public static final String ID_KEY = "id";
    public static final String TYPE_KEY = "type";

    protected String user;
    protected String periodicity;
    protected String id;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(String periodicity) {
        this.periodicity = periodicity;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    
    public abstract String getType();

    public abstract JSONObject toJSONObject();

    public abstract SolrInputDocument toSolrDocument();
    
    /**
     * Make sure if notification contains id, if not, it generates new
     */
    public void makeSureId() {
        // not thread safe
        if (getId() == null) {
            UUID randomUUID = UUID.randomUUID();
            this.setId(randomUUID.toString());
        }
    }

    
    
    protected static void enhanceNotificationFromJSON(AbstractNotification notification, JSONObject jsonObject) {
        if (jsonObject.has(ID_KEY)) {
            notification.setId(jsonObject.getString(ID_KEY));
        }
        if (jsonObject.has(USER_KEY)) {
            notification.setUser(jsonObject.getString(USER_KEY));
        }
        if (jsonObject.has(PERIODICITY_KEY)) {
            notification.setPeriodicity(jsonObject.getString(PERIODICITY_KEY));
        }
    }

    protected static void enhanceNotificationFromSolrDoc(AbstractNotification notification, SolrDocument sdoc) {
        if (sdoc.containsKey(ID_KEY)) {
            notification.setId(sdoc.getFieldValue(ID_KEY).toString());
        }
        if (sdoc.containsKey(USER_KEY)) {
            notification.setUser(sdoc.getFieldValue(USER_KEY).toString());
        }
        if (sdoc.containsKey(PERIODICITY_KEY)) {
            notification.setPeriodicity(sdoc.getFieldValue(PERIODICITY_KEY).toString());
        }
    }

    protected static void enhanceJSONObject(AbstractNotification notification, JSONObject jsonObject) {
        if (notification.getUser() != null) {
            jsonObject.put(USER_KEY, notification.getUser());
        }
        if (notification.getPeriodicity() != null) {
            jsonObject.put(PERIODICITY_KEY, notification.getPeriodicity());
        }
        if (notification.getId() != null) {
            jsonObject.put(ID_KEY, notification.getId());
        }
        if (notification.getType() != null) {
            jsonObject.put(TYPE_KEY, notification.getType());
        }
    }

    protected static void enhanceSolrInputDocument(AbstractNotification notification, SolrInputDocument docinput) {
        if (notification.getUser() != null) {
            docinput.setField(USER_KEY, notification.getUser());
        }
        if (notification.getPeriodicity() != null) {
            docinput.setField(PERIODICITY_KEY, notification.getPeriodicity());
        }
        if (notification.getId() != null) {
            docinput.setField(ID_KEY, notification.getId());
        }
        if (notification.getType() != null) {
            docinput.setField(TYPE_KEY, notification.getType());
        }
    }
    
}
