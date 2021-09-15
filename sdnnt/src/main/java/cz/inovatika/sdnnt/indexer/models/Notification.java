package cz.inovatika.sdnnt.indexer.models;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.json.JSONObject;

import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Notification {

    private String id;
    private String identifier;
    private String user;
    private String periodicity;
    private Date indextime;

    public Notification() {
    }

    public String getUser() {
        return user;
    }

    @Field
    public void setUser(String user) {
        this.user = user;
    }

    public Date getIndextime() {
        return indextime;
    }

    @Field
    public void setIndextime(Date indextime) {
        this.indextime = indextime;
    }

    @Field
    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Field
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }


    public String getPeriodicity() {
        return periodicity;
    }

    @Field
    public void setPeriodicity(String periodicity) {
        this.periodicity = periodicity;
    }

    public static Notification fromJSON(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Notification o = objectMapper.readValue(json, Notification.class);
        return o;
    }




    public JSONObject toJSONObject() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return new JSONObject(mapper.writeValueAsString(this));
        } catch (JsonProcessingException ex) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            return new JSONObject().put("error", ex);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(user, that.user) &&
                Objects.equals(periodicity, that.periodicity) &&
                Objects.equals(indextime, that.indextime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, identifier, user, periodicity, indextime);
    }
}
