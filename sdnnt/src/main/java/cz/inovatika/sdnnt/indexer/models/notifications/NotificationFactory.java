package cz.inovatika.sdnnt.indexer.models.notifications;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;

public class NotificationFactory {

    private NotificationFactory() {
    }

    public static AbstractNotification fromSolrDoc(SolrDocument solrDoc) {
        if (solrDoc.containsKey(AbstractNotification.TYPE_KEY)) {
            Object fieldValue = solrDoc.getFieldValue(AbstractNotification.TYPE_KEY);
            if (fieldValue != null) {
                TYPE vv = AbstractNotification.TYPE.find(fieldValue.toString());
                switch (vv) {
                case rule:
                    return RuleNotification.fromSolrDoc(solrDoc);
                case simple:
                    return SimpleNotification.fromSolrDoc(solrDoc);
                default:
                    return SimpleNotification.fromSolrDoc(solrDoc);
                }

            } else
                return SimpleNotification.fromSolrDoc(solrDoc);
        }
        return SimpleNotification.fromSolrDoc(solrDoc);
    }

    public static AbstractNotification fromJSON(String object) {
        return fromJSON(new JSONObject(object));
    }

    public static AbstractNotification fromJSON(JSONObject object) {
        if (object.has(AbstractNotification.TYPE_KEY)) {
            String type = object.getString(AbstractNotification.TYPE_KEY);
            TYPE vv = AbstractNotification.TYPE.find(type);
            switch (vv) {
            case rule:
                return RuleNotification.fromJSON(object);
            case simple:
                return SimpleNotification.fromJSON(object);
            default:
                return SimpleNotification.fromJSON(object);
            }
        } else {
            return SimpleNotification.fromJSON(object);
        }
    }
}
