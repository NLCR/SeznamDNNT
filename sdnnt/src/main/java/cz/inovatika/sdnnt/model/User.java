package cz.inovatika.sdnnt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
//@JsonIgnoreProperties(ignoreUnknown = true)
public class User {


  public static final String USERNAME_KEY  = "username";
  public static final String PWD_KEY  = "pwd";
  public static final String APIKEY_KEY  = "apikey";
  public static final String ROLE_KEY  = "role";
  public static final String STATE_KEY  = "state";
  public static final String TYP_KEY  = "typ";
  public static final String NOTIFIKACE_INTERVAL_KEY  = "notifikace_interval";

  public static final String JMENO_KEY  = "jmeno";
  public static final String PRIJMENI_KEY  = "prijmeni";
  public static final String TITUL_KEY  = "titul";
  public static final String ICO_KEY  = "ico";
  public static final String ADRESA_KEY  = "adresa";

  public static final String PSC_KEY  = "psc";
  public static final String MESTO_KEY  = "mesto";

  public static final String ULICE_KEY  = "ulice";
  public static final String CISLO_KEY  = "cislo";

  public static final String TELEFON_KEY  = "telefon";
  public static final String EMAIL_KEY  = "email";

  public static final String KONTAKTNI_KEY  = "kontaktni";
  public static final String NOSITEL_KEY  = "nositel";

  public static final String POZNAMKA_KEY  = "poznamka";

  public static final String RESET_PWD_KEY  = "resetPwdToken";
  public static final String RESET_PWD_EXPIRATION_KEY  = "resetPwdExpiration";

  public static final String INSTITUTION_KEY  = "institution";

  public static final String THIRD_PARTY_USER_KEY  = "thirdpartyuser";


  private String username;
  private String pwd;
  private String role;
  private String state;
  private boolean isActive = true;
  private String typ;
  private String titul;
  private String jmeno;
  private String prijmeni;
  private String ico;
  private String adresa;
  private String psc;
  private String mesto;
  private String ulice;
  private String cislo;
  private String telefon;
  private String email;
  private String kontaktni;
  private List<String> nositel; // Nositel autorských práv k dílu:
  private String poznamka;
  private String apikey;
  private String resetPwdToken;
  private Date resetPwdExpiration;
  private String institution;
  private String notifikace_interval = NotificationInterval.none.name();

  private boolean thirdPartyUser = false;

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getTyp() {
    return typ;
  }

  public void setTyp(String typ) {
    this.typ = typ;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getAdresa() {
    return adresa;
  }

  public void setAdresa(String adresa) {
    this.adresa = adresa;
  }

  public String getCislo() {
    return cislo;
  }

  public void setCislo(String cislo) {
    this.cislo = cislo;
  }


  public String getPoznamka() {
    return poznamka;
  }

  public void setPoznamka(String poznamka) {
    this.poznamka = poznamka;
  }

  public String getIco() {
    return ico;
  }

  public void setIco(String ico) {
    this.ico = ico;
  }

  public String getJmeno() {
    return jmeno;
  }

  public void setJmeno(String jmeno) {
    this.jmeno = jmeno;
  }

  public String getMesto() {
    return mesto;
  }

  public void setMesto(String mesto) {
    this.mesto = mesto;
  }

  public String getPrijmeni() {
    return prijmeni;
  }

  public void setPrijmeni(String prijmeni) {
    this.prijmeni = prijmeni;
  }

  public String getPsc() {
    return psc;
  }

  public void setPsc(String psc) {
    this.psc = psc;
  }

  public String getApikey() {
    return apikey;
  }

  public void setApikey(String apikey) {
    this.apikey = apikey;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getKontaktni() {
    return kontaktni;
  }

  public void setKontaktni(String kontaktni) {
    this.kontaktni = kontaktni;
  }

  public String getPwd() {
    return pwd;
  }

  public void setPwd(String pwd) {
    this.pwd = pwd;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getTelefon() {
    return telefon;
  }

  public void setTelefon(String telefon) {
    this.telefon = telefon;
  }

  public List<String> getNositel() {
    return nositel;
  }

  public void setNositel(List<String> nositel) {
    this.nositel = nositel;
  }

  public String getTitul() {
    return titul;
  }

  public void setTitul(String titul) {
    this.titul = titul;
  }

  public String getUlice() {
    return ulice;
  }

  public void setUlice(String ulice) {
    this.ulice = ulice;
  }

  public String getResetPwdToken() {
    return resetPwdToken;
  }

  public void setResetPwdToken(String resetPwdToken) {
    this.resetPwdToken = resetPwdToken;
  }

  public Date getResetPwdExpiration() {
    return resetPwdExpiration;
  }

  public void setResetPwdExpiration(Date resetPwdExpiration) {
    this.resetPwdExpiration = resetPwdExpiration;
  }

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public String getNotifikaceInterval() {
    return notifikace_interval;
  }


  public void setNotifikaceInterval(String notifikace_interval) {
    this.notifikace_interval = notifikace_interval;
  }


//  public boolean isProfileEditable() {
//    return profileEditable;
//  }
//  public void setProfileEditable(boolean profileEditable) {
//    this.profileEditable = profileEditable;
//  }

  public boolean isThirdPartyUser() {
    return thirdPartyUser;
  }

  public void setThirdPartyUser(boolean thirdPartyUser) {
    this.thirdPartyUser = thirdPartyUser;
  }

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(USERNAME_KEY, this.username);
    if (this.pwd != null)  jsonObject.put(PWD_KEY, this.pwd);
    if (this.apikey != null) jsonObject.put(APIKEY_KEY, this.apikey);
    if (this.role !=null) jsonObject.put(ROLE_KEY, this.role);
    if (this.state !=null) jsonObject.put(STATE_KEY, this.state);
    if (this.typ !=null) jsonObject.put(TYP_KEY, this.typ);
    if (this.jmeno !=null) jsonObject.put(JMENO_KEY, this.jmeno);
    if (this.prijmeni !=null) jsonObject.put(PRIJMENI_KEY, this.prijmeni);
    if (this.titul !=null) jsonObject.put(TITUL_KEY, this.titul);
    if (this.ico !=null) jsonObject.put(ICO_KEY, this.ico);
    if (this.adresa !=null) jsonObject.put(ADRESA_KEY, this.adresa);
    if (this.psc !=null) jsonObject.put(PSC_KEY, this.psc);
    if (this.mesto !=null) jsonObject.put(MESTO_KEY, this.mesto);
    if (this.ulice !=null) jsonObject.put(ULICE_KEY, this.ulice);
    if (this.cislo !=null) jsonObject.put(CISLO_KEY, this.cislo);
    if (this.telefon !=null) jsonObject.put(TELEFON_KEY, this.telefon);
    if (this.email !=null) jsonObject.put(EMAIL_KEY, this.email);
    if (this.kontaktni !=null) jsonObject.put(KONTAKTNI_KEY, this.kontaktni);
    if (this.nositel !=null) jsonObject.put(NOSITEL_KEY, this.nositel);
    if (this.poznamka !=null) jsonObject.put(POZNAMKA_KEY, this.poznamka);
    if (this.resetPwdToken !=null) jsonObject.put(RESET_PWD_KEY, this.resetPwdToken);
    if (this.resetPwdExpiration !=null) jsonObject.put(RESET_PWD_EXPIRATION_KEY, this.resetPwdExpiration);
    if (this.notifikace_interval != null) jsonObject.put(NOTIFIKACE_INTERVAL_KEY, this.notifikace_interval);
    if (this.institution != null) jsonObject.put(INSTITUTION_KEY, this.institution);
    if (this.thirdPartyUser) jsonObject.put(THIRD_PARTY_USER_KEY, this.thirdPartyUser);
    return jsonObject;
  }

  public static User fromJSON(String str) {
    JSONObject jsonObject = new JSONObject(str);
    User user = new User();
    if (jsonObject.has(USERNAME_KEY)) {
      user.setUsername(jsonObject.getString(USERNAME_KEY));
    }
    if (jsonObject.has(JMENO_KEY)) {
      user.setJmeno(jsonObject.getString(JMENO_KEY));
    }
    if (jsonObject.has(PRIJMENI_KEY)) {
      user.setPrijmeni(jsonObject.getString(PRIJMENI_KEY));
    }

    if (jsonObject.has(TITUL_KEY)) {
      user.setTitul(jsonObject.getString(TITUL_KEY));
    }

    if (jsonObject.has(PWD_KEY)) {
      user.setPwd(jsonObject.getString(PWD_KEY));
    }

    if (jsonObject.has(APIKEY_KEY)) {
      user.setApikey(jsonObject.getString(APIKEY_KEY));
    }

    if (jsonObject.has(ROLE_KEY)) {
      user.setRole(jsonObject.getString(ROLE_KEY));
    }
    if (jsonObject.has(STATE_KEY)) {
      user.setState(jsonObject.getString(STATE_KEY));
    }
    if (jsonObject.has(TYP_KEY)) {
      user.setTyp(jsonObject.getString(TYP_KEY));
    }
    if (jsonObject.has(ICO_KEY)) {
      user.setIco(jsonObject.getString(ICO_KEY));
    }
    if (jsonObject.has(ADRESA_KEY)) {
      user.setAdresa(jsonObject.getString(ADRESA_KEY));
    }
    if (jsonObject.has(PSC_KEY)) {
      user.setPsc(jsonObject.getString(PSC_KEY));
    }
    if (jsonObject.has(MESTO_KEY)) {
      user.setMesto(jsonObject.getString(MESTO_KEY));
    }
    if (jsonObject.has(ULICE_KEY)) {
      user.setUlice(jsonObject.getString(ULICE_KEY));
    }
    if (jsonObject.has(CISLO_KEY)) {
      user.setCislo(jsonObject.getString(CISLO_KEY));
    }
    if (jsonObject.has(TELEFON_KEY)) {
      user.setTelefon(jsonObject.getString(TELEFON_KEY));
    }
    if (jsonObject.has(EMAIL_KEY)) {
      user.setEmail(jsonObject.getString(EMAIL_KEY));
    }
    if (jsonObject.has(KONTAKTNI_KEY)) {
      user.setKontaktni(jsonObject.getString(KONTAKTNI_KEY));
    }
    if (jsonObject.has(NOSITEL_KEY)) {
      JSONArray jsonArray = jsonObject.getJSONArray(NOSITEL_KEY);
      List<String> list = new ArrayList<>();
      for (int i = 0; i < jsonArray.length(); i++) {
        list.add(jsonArray.getString(i));
      }
      user.setNositel(list);
    }

    if (jsonObject.has(POZNAMKA_KEY)) {
      user.setPoznamka(jsonObject.getString(POZNAMKA_KEY));
    }

    if (jsonObject.has(RESET_PWD_KEY)) {
      user.setResetPwdToken(jsonObject.getString(RESET_PWD_KEY));
    }

    if (jsonObject.has(RESET_PWD_EXPIRATION_KEY)) {
      user.setResetPwdToken(jsonObject.getString(RESET_PWD_EXPIRATION_KEY));
    }

    if(jsonObject.has(NOTIFIKACE_INTERVAL_KEY)) {
      String string = jsonObject.getString(NOTIFIKACE_INTERVAL_KEY);
      user.setNotifikaceInterval(string);
    }

    if (jsonObject.has(INSTITUTION_KEY)) {
      String inst = jsonObject.getString(INSTITUTION_KEY);
      user.setInstitution(inst);
    }

    return user;
  }

  public static User fromSolrDocument(SolrDocument doc) {
    User user = new User();
    if (doc.containsKey(USERNAME_KEY)) {
      user.setUsername((String) doc.getFieldValue(USERNAME_KEY));
    }
    if (doc.containsKey(JMENO_KEY)) {
      user.setJmeno((String) doc.getFieldValue(JMENO_KEY));
    }
    if (doc.containsKey(PRIJMENI_KEY)) {
      user.setPrijmeni((String) doc.getFieldValue(PRIJMENI_KEY));
    }

    if (doc.containsKey(TITUL_KEY)) {
      user.setTitul((String) doc.getFieldValue(TITUL_KEY));
    }

    if (doc.containsKey(PWD_KEY)) {
      user.setPwd((String) doc.getFieldValue(PWD_KEY));
    }

    if (doc.containsKey(APIKEY_KEY)) {
      user.setApikey((String) doc.getFieldValue(APIKEY_KEY));
    }

    if (doc.containsKey(ROLE_KEY)) {
      user.setRole((String) doc.getFieldValue(ROLE_KEY));
    }
    if (doc.containsKey(STATE_KEY)) {
      user.setState((String) doc.getFieldValue(STATE_KEY));
    }
    if (doc.containsKey(TYP_KEY)) {
      user.setTyp((String) doc.getFieldValue(TYP_KEY));
    }
    if (doc.containsKey(ICO_KEY)) {
      user.setIco((String) doc.getFieldValue(ICO_KEY));
    }
    if (doc.containsKey(ADRESA_KEY)) {
      user.setAdresa((String) doc.getFieldValue(ADRESA_KEY));
    }
    if (doc.containsKey(PSC_KEY)) {
      user.setPsc((String) doc.getFieldValue(PSC_KEY));
    }
    if (doc.containsKey(MESTO_KEY)) {
      user.setMesto((String) doc.getFieldValue(MESTO_KEY));
    }
    if (doc.containsKey(ULICE_KEY)) {
      user.setUlice((String) doc.getFieldValue(ULICE_KEY));
    }
    if (doc.containsKey(CISLO_KEY)) {
      user.setCislo((String) doc.getFieldValue(CISLO_KEY));
    }
    if (doc.containsKey(TELEFON_KEY)) {
      user.setTelefon((String) doc.getFieldValue(TELEFON_KEY));
    }
    if (doc.containsKey(EMAIL_KEY)) {
      user.setEmail((String) doc.getFieldValue(EMAIL_KEY));
    }
    if (doc.containsKey(KONTAKTNI_KEY)) {
      user.setKontaktni((String) doc.getFieldValue(KONTAKTNI_KEY));
    }
    if (doc.containsKey(NOSITEL_KEY)) {
      Collection<Object> fieldValues = doc.getFieldValues(NOSITEL_KEY);
      List<String> list = new ArrayList<>();
      fieldValues.stream().forEach(obj -> {
        list.add(obj.toString());
      });
//      for (int i = 0; i < fieldValues.size(); i++) {
//        list.add(jsonArray.getString(i));
//      }
      user.setNositel(list);
    }

    if (doc.containsKey(POZNAMKA_KEY)) {
      user.setPoznamka((String) doc.getFieldValue(POZNAMKA_KEY));
    }

    if (doc.containsKey(RESET_PWD_KEY)) {
      user.setResetPwdToken((String) doc.getFieldValue(RESET_PWD_KEY));
    }

    if (doc.containsKey(RESET_PWD_EXPIRATION_KEY)) {
      user.setResetPwdExpiration((Date) doc.getFieldValue(RESET_PWD_EXPIRATION_KEY));
    }

    if(doc.containsKey(NOTIFIKACE_INTERVAL_KEY)) {
      String string = (String) doc.getFieldValue(NOTIFIKACE_INTERVAL_KEY);
      user.setNotifikaceInterval(string);
    }

    if (doc.containsKey(INSTITUTION_KEY)) {
      String string = (String) doc.getFieldValue(NOTIFIKACE_INTERVAL_KEY);
      user.setInstitution(string);
    }

    return user;

  }


  public SolrInputDocument toSolrInputDocument() {
    // username, pwd, apikey, role. state, type, jmeno, prijmeni, titul, ico, adresa, psc, mesto, cislo, telefon, email, kontaktni
    SolrInputDocument sinput = new SolrInputDocument();
    sinput.addField(USERNAME_KEY, this.username);
    if (this.pwd != null) {
      sinput.addField(PWD_KEY, this.pwd);
    }

    if (this.apikey != null) {
      sinput.addField(APIKEY_KEY, this.apikey);
    }

    if (this.role != null) {
      sinput.addField(ROLE_KEY, this.role);
    }

    if (this.state != null) {
      sinput.addField(STATE_KEY, this.state);
    }

    if (this.typ != null) {
      sinput.addField(TYP_KEY, this.typ);
    }
    if (this.jmeno != null) {
      sinput.addField(JMENO_KEY, this.jmeno);
    }
    if (this.prijmeni != null) {
      sinput.addField(PRIJMENI_KEY, this.prijmeni);
    }
    if (this.titul != null) {
      sinput.addField(TITUL_KEY, this.titul);
    }
    if (this.ico != null) {
      sinput.addField(ICO_KEY, this.ico);
    }
    if (this.adresa != null) {
      sinput.addField(ADRESA_KEY, this.adresa);
    }
    if (this.psc != null) {
      sinput.addField(PSC_KEY, this.psc);
    }
    if (this.mesto != null) {
      sinput.addField(MESTO_KEY, this.mesto);
    }
    if (this.cislo != null) {
      sinput.addField(CISLO_KEY, this.cislo);
    }
    if (this.telefon != null) {
      sinput.addField(TELEFON_KEY, this.telefon);
    }
    if (this.email != null) {
      sinput.addField(EMAIL_KEY, this.email);
    }
    if (this.kontaktni != null) {
      sinput.addField(KONTAKTNI_KEY, this.kontaktni);
    }
    if (this.kontaktni != null) {
      sinput.addField(KONTAKTNI_KEY, this.kontaktni);
    }

    if (this.nositel != null) {
      sinput.addField(NOSITEL_KEY, this.nositel);
    }
    if (this.poznamka != null) {
      sinput.addField(POZNAMKA_KEY, this.poznamka);
    }
    if (this.resetPwdToken != null) {
      sinput.addField(RESET_PWD_KEY, this.resetPwdToken);
    }
    if (this.resetPwdExpiration != null) {
      sinput.addField(RESET_PWD_EXPIRATION_KEY, this.resetPwdExpiration);
    }
    if (this.notifikace_interval != null) {
      sinput.addField(NOTIFIKACE_INTERVAL_KEY, this.notifikace_interval);
    }

    return sinput;

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
