package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.beans.Field;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {


  @Field
  public String username;
  
  @Field
  public String pwd;
  
  @Field
  public String role;
  
  @Field
  public String state;
  
  @Field
  public boolean isActive;

  @Field
  public String typ;

  @Field
  public String titul;
  
  @Field
  public String jmeno;
  
  @Field
  public String prijmeni;

  @Field
  public String ico;

  @Field
  public String adresa;

  @Field
  public String psc;

  @Field
  public String mesto;

  @Field
  public String telefon;

  @Field
  public String email;

  @Field
  public String kontaktni;

  @Field
  public List<String> nositel; // Nositel autorských práv k dílu: 

  @Field
  public String poznamka;

  @Field
  public String apikey;

  @Field
  public String resetPwdToken;
  
  @Field
  public Date resetPwdExpiration;

  @Field
  public String notifikace_interval = NotificationInterval.none.name();


  public JSONObject toJSONObject() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return new JSONObject(mapper.writeValueAsString(this));
    } catch (JsonProcessingException ex) {
      Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static User fromJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    User o = objectMapper.readValue(json, User.class);
    return o;
  }




  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(username, user.username) &&
            Objects.equals(pwd, user.pwd);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, pwd);
  }
}
