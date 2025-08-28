package cz.inovatika.sdnnt.indexer.models.notifications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.StringUtils;

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
            Object q = object.get(QUERY_KEY);
            if (q !=null ) {
                rnotification.setQuery(q.toString());
            }
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

        List<String> fi = new ArrayList<>();
        Map<String,String> filters = new HashMap<>(this.filters);
        String qFilter = queryFilter();
        if (qFilter!= null) {
            fi.add(qFilter);
        }
        
        if (filters.containsKey(MarcRecordFields.YEAR_OF_PUBLICATION)) {
            String removed = filters.remove(MarcRecordFields.YEAR_OF_PUBLICATION);
            fi.add(MarcRecordFields.YEAR_OF_PUBLICATION + ":[" + removed.replace(",", " TO ") + "]");
        }

        filters.keySet().forEach(key-> {
            fi.add(key+":\""+this.filters.get(key)+"\"");
        });
        builder.append(fi.stream().collect(Collectors.joining(" AND ")));
        return builder.toString();
    }

    private String queryFilter() {
        if (this.query != null && StringUtils.isAnyString(query)) {
            List<String> searchFields = Arrays.asList(
                "title",
                "id_pid",
                "id_all_identifiers",
                "id_all_identifiers_cuts",
                "fullText"
            );
            
            String collected = searchFields.stream().map(it -> {
                return it +":\""+query+"\"";
            }).collect(Collectors.joining(" OR "));
            return "("+collected+")";
        }
        return null;
        
    }
    
    public boolean accept(Map<String,String> doc) {
        if (this.filters.containsKey(MarcRecordFields.DNTSTAV_FIELD) || this.filters.containsKey(MarcRecordFields.LICENSE_FIELD)) {
            String string = doc.get(MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD);
            JSONArray jsonArray = new JSONArray(string);
            // check previous state
            if (jsonArray.length() >=2) {
                JSONObject object = jsonArray.getJSONObject(jsonArray.length()-2);
                if (this.filters.containsKey(MarcRecordFields.DNTSTAV_FIELD)) {
                   String historyStav = object.getString("stav");
                   String dntStavField = this.filters.get(MarcRecordFields.DNTSTAV_FIELD);
                   if (historyStav != null && dntStavField != null && !historyStav.equals(dntStavField)) {
                       return false;
                   }
                }
                if (this.filters.containsKey(MarcRecordFields.LICENSE_FIELD)) {
                    String historyLicense = object.getString("license");
                    String lisenceField = this.filters.get(MarcRecordFields.LICENSE_FIELD);
                    if (historyLicense != null && lisenceField != null && !historyLicense.equals(lisenceField)) {
                        return false;
                    }
                 }
            }
            return true;
        } else {
            return true;
        }
    }
    
    
    // dnstav, kuratorstav
    public String provideProcessQueryFilters() {
        
        Map<String,String> filters = new HashMap<>(this.filters);
        
        StringBuilder builder = new StringBuilder();
        List<String> fi = new ArrayList<>();
        String qFilter = queryFilter();
        if (qFilter!= null) {
            fi.add(qFilter);
        }

        if (filters.containsKey(MarcRecordFields.YEAR_OF_PUBLICATION)) {
            String removed = filters.remove(MarcRecordFields.YEAR_OF_PUBLICATION);
            fi.add(MarcRecordFields.YEAR_OF_PUBLICATION + ":[" + removed.replace(",", " TO ") + "]");
        }
        if (filters.containsKey(MarcRecordFields.DNTSTAV_FIELD)) {
            String removedDntStav = filters.remove(MarcRecordFields.DNTSTAV_FIELD);
            fi.add(MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD_CUT+":"+ removedDntStav);
            fi.add("NOT "+MarcRecordFields.DNTSTAV_FIELD+":"+removedDntStav);
        }
        
        if (filters.containsKey(MarcRecordFields.LICENSE_FIELD)) {
            String removedLicense = filters.remove(MarcRecordFields.LICENSE_FIELD);
            fi.add(MarcRecordFields.HISTORIE_KURATORSTAVU_FIELD_CUT+":"+ removedLicense);
            //fi.add("NOT "+MarcRecordFields.LICENSE_FIELD+":"+removedLicense);
        }
        filters.keySet().forEach(key-> {
            fi.add(key+":\""+this.filters.get(key)+"\"");
        });
        
        builder.append(fi.stream().collect(Collectors.joining(" AND ")));
        return builder.toString();
}
}
