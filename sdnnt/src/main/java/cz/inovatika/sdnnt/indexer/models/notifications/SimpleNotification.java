package cz.inovatika.sdnnt.indexer.models.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.model.User;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.*;

/**
 * Simple notification
 * 
 * @author happy
 */
public class SimpleNotification extends AbstractNotification {

    // public static final String TYPE = "simple";
    public static final String IDENTIFIER_KEY = "identifier";

    private String identifier;

    protected SimpleNotification() {
        super();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getType() {
        return TYPE.simple.name();
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        enhanceJSONObject(this, jsonObject);
        if (getIdentifier() != null) {
            jsonObject.put(IDENTIFIER_KEY, getIdentifier());
        }
        return jsonObject;
    }

    @Override
    public SolrInputDocument toSolrDocument() {
        SolrInputDocument sdoc = new SolrInputDocument();
        enhanceSolrInputDocument(this, sdoc);
        if (getIdentifier() != null) {
            sdoc.setField(IDENTIFIER_KEY, getIdentifier());
        }
        return sdoc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SimpleNotification that = (SimpleNotification) o;
        return Objects.equals(id, that.id) && Objects.equals(identifier, that.identifier)
                && Objects.equals(user, that.user) && Objects.equals(periodicity, that.periodicity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, identifier, user, periodicity);
    }

    public static SimpleNotification fromJSON(JSONObject object) {
        SimpleNotification snotification = new SimpleNotification();
        enhanceNotificationFromJSON(snotification, object);
        if (object.has(IDENTIFIER_KEY)) {
            snotification.setIdentifier(object.getString(IDENTIFIER_KEY));
        }
        return snotification;
    }

    public static SimpleNotification fromJSON(String json) {
        JSONObject object = new JSONObject(json);
        return fromJSON(object);
    }

    public static SimpleNotification fromSolrDoc(SolrDocument sdoc) {
        SimpleNotification snotification = new SimpleNotification();
        enhanceNotificationFromSolrDoc(snotification, sdoc);
        if (sdoc.containsKey(IDENTIFIER_KEY)) {
            snotification.setIdentifier(sdoc.getFieldValue(IDENTIFIER_KEY).toString());
        }
        return snotification;
    }

}
