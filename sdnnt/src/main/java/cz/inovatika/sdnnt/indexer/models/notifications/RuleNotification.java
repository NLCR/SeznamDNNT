package cz.inovatika.sdnnt.indexer.models.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.utils.MarcRecordFields;

/**
 * Represents one notification filter
 * 
 * @author happy
 */
public class RuleNotification extends AbstractNotification {

    public static final String NAME_KEY = "name";
    public static final String QUERY_KEY = "query";
    public static final String FILTERS_KEY = "filters";
    
    private String name;
    private String query;

    private Map<String, String> filters = new HashMap<>();

    protected RuleNotification() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public String getType() {
        return TYPE.rule.name();
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> map) {
        this.filters = map;
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        enhanceJSONObject(this, jsonObject);
        if (getName() != null) {
            jsonObject.put(NAME_KEY, getName());
        }
        if (getQuery() != null) {
            jsonObject.put(QUERY_KEY, getQuery());
        }

        if (getFilters() != null && !getFilters().isEmpty()) {
            JSONObject serializedFilters = serializeFilters(getFilters());
            jsonObject.put(FILTERS_KEY, serializedFilters.toString());
        }
        return jsonObject;
    }

    private JSONObject serializeFilters(Map<String, String> filters2) {
        JSONObject serialized = new JSONObject(filters2);
        return serialized;
    }

    private static Map<String, String> deserializeFilters(String obj) {
        return deserializeFilters(new JSONObject(obj));
    }

    private static Map<String, String> deserializeFilters(JSONObject obj) {
        Map<String, String> retvals = new HashMap<>();
        obj.toMap().entrySet().forEach(entry -> {
            retvals.put(entry.getKey(), entry.getValue().toString());
        });
        return retvals;
    }

    @Override
    public SolrInputDocument toSolrDocument() {
        SolrInputDocument sdoc = new SolrInputDocument();
        enhanceSolrInputDocument(this, sdoc);
        if (getName() != null) {
            sdoc.setField(NAME_KEY, getName());
        }
        if (getQuery() != null) {
            sdoc.setField(QUERY_KEY, getQuery());
        }
        if (getFilters() != null && !getFilters().isEmpty()) {
            JSONObject serializedFilters = serializeFilters(getFilters());
            sdoc.setField(FILTERS_KEY, serializedFilters.toString());
        }
        return sdoc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filters, name, query);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RuleNotification other = (RuleNotification) obj;
        return Objects.equals(filters, other.filters) && Objects.equals(name, other.name)
                && Objects.equals(query, other.query);
    }

    public static RuleNotification fromJSON(JSONObject object) {
        RuleNotification rnotification = new RuleNotification();
        enhanceNotificationFromJSON(rnotification, object);
        if (object.has(NAME_KEY)) {
            rnotification.setName(object.getString(NAME_KEY));
        }
        if (object.has(QUERY_KEY)) {
            rnotification.setQuery(object.getString(QUERY_KEY));
        }
        if (object.has(FILTERS_KEY)) {
            rnotification.setFilters(deserializeFilters(object.getString(FILTERS_KEY)));
        }
        return rnotification;
    }

    public static RuleNotification fromJSON(String json) {
        JSONObject object = new JSONObject(json);
        return fromJSON(object);
    }

    public static RuleNotification fromSolrDoc(SolrDocument sdoc) {
        RuleNotification rnotification = new RuleNotification();
        enhanceNotificationFromSolrDoc(rnotification, sdoc);
        if (sdoc.containsKey(NAME_KEY)) {
            rnotification.setName(sdoc.getFieldValue(NAME_KEY).toString());
        }
        if (sdoc.containsKey(QUERY_KEY)) {
            rnotification.setQuery(sdoc.getFieldValue(QUERY_KEY).toString());
        }
        if (sdoc.containsKey(FILTERS_KEY)) {
            String json = sdoc.getFieldValue(FILTERS_KEY).toString();
            rnotification.setFilters(deserializeFilters(json));
        }
        return rnotification;
    }
    
    /**
     * Method returns query filters
     * @return
     */
    public String provideSearchQueryFilters() {
        // dnt stav a licence musi byt jinak
        StringBuilder builder = new StringBuilder();
        
        if (this.filters.containsKey(MarcRecordFields.YEAR_OF_PUBLICATION)) {
            String removed = this.filters.remove(MarcRecordFields.YEAR_OF_PUBLICATION);
            // dntstavy - musi byt predchozi 
            // licence - musi byt predchozi
            
        }

        List<String> fi = new ArrayList<>();
        this.filters.keySet().forEach(key-> {
            fi.add(key+":"+this.filters.get(key));
        });
        
        builder.append(fi.stream().collect(Collectors.joining(" AND ")));
        return builder.toString();
    }
    
    // dnstav, kuratorstav
    public String provideProcessQueryFilters() {
        // dnt stav a licence musi byt jinak
        StringBuilder builder = new StringBuilder();
        if (this.filters.containsKey(MarcRecordFields.YEAR_OF_PUBLICATION)) {
            String removed = this.filters.remove(MarcRecordFields.YEAR_OF_PUBLICATION);

            if (this.filters.containsKey(MarcRecordFields.DNTSTAV_FIELD)) {
                String removedDntStav = this.filters.remove(MarcRecordFields.DNTSTAV_FIELD);
                
            }
            
            if (this.filters.containsKey(MarcRecordFields.LICENSE_FIELD)) {
                String removedLicense = this.filters.remove(MarcRecordFields.DNTSTAV_FIELD);
            }
        }
        return builder.toString();
}
}
