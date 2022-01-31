package cz.inovatika.sdnnt.model;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.Indexer;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cz.inovatika.sdnnt.model.workflow.ZadostTyp;
import cz.inovatika.sdnnt.services.impl.HistoryImpl;
import cz.inovatika.sdnnt.utils.BeanUtilities;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Zadost implements NotNullAwareObject{
  
  public static final Logger LOGGER = Logger.getLogger(Zadost.class.getName());

  // neni, vyhodit
  public static final String TYP_KEY = "typ";

  // typ zadost open, waiting, processed - dalsi stav waiting_for_automatic_process
  public static final String STATE_KEY = "state";
  // kurator posuzujici zadost
  public static final String KURATOR_KEY = "kurator";
  // typ navrhu
  public static final String NAVRH_KEY = "navrh";
  // poznamka
  public static final String POZNAMKA_KEY = "poznamka";
  // pozdavek
  public static final String POZADAVEK_KEY = "pozadavek";

  // datum zadadni - kdy to poslal uzivatel
  public static final String DATUM_ZADANI_KEY = "datum_zadani";

  // datum vyrizeni - kdy to vyridil kurator, nebo kdy to bylo automaticky prepnuto
  public static final String DATUM_VYRIZENI_KEY = "datum_vyrizeni";

  // vyodit
  public static final String FORMULAR_KEY = "formular";

  // uzivatel, ktery to poslal
  public static final String USER_KEY = "user";

  // instituce, ke ktere uzivatel patri
  public static final String INSTITUTION_KEY = "institution";

  // delegovano na jineho kurator
  public static final String DELEGATED_KEY = "delegated";

  // delegovano s prioritou
  public static final String PRIORITY_KEY = "priority";

  // verze
  //public static final String VERSION_KEY = "_version_";

  public static final String SOLR_VERSION_KEY = "version";
  public static final String VERSION_KEY = "version";

  // identifikatory zarazene do zadosti
  public static final String IDENTIFIERS_KEY = "identifiers";

  // id zadosti
  public static final String ID_KEY = "id";

  // aktulalni stav zpracovani zadosti - polozka po polozce
  public static final String PROCESS_KEY = "process";

  // lhuty navazane na zadosti
  // typ lhuty - period_0, period_1, period_2
  public static final String TYPE_OF_PERIOD_KEY = "type_of_period";

  // deadline vypocitany z lhuty
  public static final String DEADLINE_KEY = "deadline";

  // typ prepnuti do stavu - Kurator nebo automat
  public static final String TRANSITION_TYPE_KEY = "type_of_transition";

  // email
  public static final String EMAIL_KEY = "email";

  public static final String DESIRED_ITEM_STATE_KEY = "desired_item_state";
  public static final String DESIRED_LICENSE_KEY = "desired_license";

  // typ zadosti; pokud je generovany systemem nebo uzivatelem
  public static final String TYPE_OF_REQUEST = "type_of_request";

  // historie zadosti
  private String id;
  private String typ;
  private String state;
  private String kurator;
  private String navrh;
  private String poznamka;
  private String pozadavek;
  private Date datum_zadani;
  private Date datum_vyrizeni;
  private String formular;
  private Map<String, ZadostProcess> process;
  private List<String> identifiers;
  private String user;
  private String institution;
  private String delegated;
  private String priority;

  private String typeOfPeriod;
  private Date deadline;

  private String transitionType;

  private String desiredItemState;
  private String desiredLicense;

  private String email;

  // version for
  private String version;

  private String typeOfRequest = ZadostTyp.user.name();

  public Zadost(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public List<String> getIdentifiers() {
    return identifiers;
  }

  public void setIdentifiers(List<String> identifiers) {
    this.identifiers = identifiers;
  }

  public void addIdentifier(String ident) {
    if (this.identifiers == null) {
      this.identifiers = new ArrayList<>();
    }
    this.identifiers.add(ident);
  }

  public void removeIdentifier(String ident) {
    if (this.identifiers != null) this.identifiers.remove(ident);
  }

  public String getTyp() {
    return typ;
  }

  public void setTyp(String typ) {
    this.typ = typ;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getKurator() {
    return kurator;
  }

  public void setKurator(String kurator) {
    this.kurator = kurator;
  }

  public String getNavrh() {
    return navrh;
  }

  public void setNavrh(String navrh) {
    this.navrh = navrh;
  }

  public String getPozadavek() {
    return pozadavek;
  }

  public void setPozadavek(String pozadavek) {
    this.pozadavek = pozadavek;
  }

  public String getPoznamka() {
    return poznamka;
  }

  public void setPoznamka(String poznamka) {
    this.poznamka = poznamka;
  }

  public void setDatumVyrizeni(Date datum_vyrizeni) {
    this.datum_vyrizeni = datum_vyrizeni;
  }

  public Date getDatumVyrizeni() {
    return datum_vyrizeni;
  }

  public Date getDatumZadani() {
    return datum_zadani;
  }

  public void setDatumZadani(Date datum_zadani) {
    this.datum_zadani = datum_zadani;
  }

  public String getFormular() {
    return formular;
  }

  public void setFormular(String formular) {
    this.formular = formular;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public String getDelegated() {
    return delegated;
  }

  public void setDelegated(String delegated) {
    this.delegated = delegated;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getTypeOfPeriod() {
    return typeOfPeriod;
  }

  public void setTypeOfPeriod(String typeOfPeriod) {
    this.typeOfPeriod = typeOfPeriod;
  }

  public String getTransitionType() {
    return transitionType;
  }

  public void setTransitionType(String tt) {
    this.transitionType = tt;
  }

  public Date getDeadline() {
    return deadline;
  }

  public void setDeadline(Date deadline) {
    this.deadline = deadline;
  }

  public String getDesiredItemState() {
    return desiredItemState;
  }

  public void setDesiredItemState(String desiredItemState) {
    this.desiredItemState = desiredItemState;
  }


  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }


  public void setTypeOfRequest(String typeOfRequest) {
    this.typeOfRequest = typeOfRequest;
  }

  public String getTypeOfRequest() {
    return typeOfRequest;
  }

  public boolean isEscalated() {
    if (getDeadline() != null) {
      JSONObject jsonObject = Options.getInstance().getJSONObject("workflow").getJSONObject("escalation");
      String typ = this.getNavrh();
      if (typ != null && jsonObject.has(typ)) {
        JSONObject type = jsonObject.getJSONObject(typ);
        int value = type.getInt("value");
        String unit = "day";
        if (type.has("unit")) {
          unit = type.getString("unit");
        }

        Calendar instance = Calendar.getInstance();
        instance.setTime(getDeadline());
        if (unit.equals("day")) {
          instance.add(Calendar.DAY_OF_MONTH, (-1)*value);
        } else if (unit.equals("minute")) {
          instance.add(Calendar.MINUTE, (-1)*value);

        }

        return new Date().after(instance.getTime());
      } else return false;

    } else return false;
  }

  public boolean isExpired() {
    return getDeadline() != null ? new Date().after(getDeadline()) : false;
  }
  public String getDesiredLicense() {
    return desiredLicense;
  }

  public void setDesiredLicense(String desiredLicense) {
    this.desiredLicense = desiredLicense;
  }

  public void addProcess(String id, ZadostProcess zp) {
    if (this.process == null) this.process = new HashMap<>();
    this.process.put(id, zp);

    if (zp.getTransitionName() != null) {
      this.process.put(id+"_"+zp.getTransitionName(), zp);
    }
  }

  public void removeProcess(String id, ZadostProcess zp) {
    if (this.process != null) this.process.remove(id);
  }

  public Map<String, ZadostProcess> getProcess() {
    return process;
  }
  public void setProcess(Map<String, ZadostProcess> zp) {
    this.process = zp;
  }

  // string represenation
  public String getProcessAsString() {
    Map<String, ZadostProcess> process = getProcess();
    JSONObject processObject = new JSONObject();
    process.keySet().forEach(name->{
      processObject.put(name, process.get(name).toJSON());
    });
    return processObject.toString();
  }


  @Override
  public List<String> getNotNullProperties() {
    try {
      return BeanUtilities.getNotNullProperties(this, Zadost.class);
    } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      return new ArrayList<>();
    }
  }


  public boolean allRejected(String transitionName) {
    return allItemsInOneState(transitionName, "rejected");
  }

  private boolean allItemsInOneState(String transitionName, String itemState) {
    List<String> identifiers = this.getIdentifiers() != null ? this.getIdentifiers() : new ArrayList<>();
    List<Boolean> bools = new ArrayList<>();
    Map<String, ZadostProcess> process = this.getProcess();
    if (process != null) {
      this.process.keySet().forEach(ident-> {
        ZadostProcess zProcess = process.get(ident);
        if (zProcess.getTransitionName() != null && ident.endsWith(transitionName) && zProcess.getTransitionName().equals(transitionName)) {
          if (zProcess.getState().equals(itemState))  bools.add(zProcess.getState().equals(itemState));
        }
      });
      return bools.size() == identifiers.size();
    } else return false;
  }

  public boolean allApproved(String transitionName) {
    return allItemsInOneState(transitionName, "approved");
  }

  public void setVersion(String v) {
    this.version = v;
  }

  /**
   * Get current version
   * @return
   */
  public String getVersion() {
    return version;
  }

  public SolrInputDocument toSolrInputDocument() {
    SolrInputDocument sinput = new SolrInputDocument();
    sinput.addField(ID_KEY, getId());
    if (getTyp() != null) {
      sinput.addField(TYP_KEY, getTyp());
    }
    if (getDatumVyrizeni() != null) {
      sinput.addField(DATUM_VYRIZENI_KEY, SolrJUtilities.solrDateString(getDatumVyrizeni()));
    }
    if (getDatumZadani() != null) {
      sinput.addField(DATUM_ZADANI_KEY, SolrJUtilities.solrDateString(getDatumZadani()));
    }
    if(getDelegated() != null) {
      sinput.addField(DELEGATED_KEY, getDelegated());
    }

    if (getFormular() != null) {
      sinput.addField(FORMULAR_KEY, getFormular());
    }

    if (getUser() != null) {
      sinput.addField(USER_KEY, getUser());
    }

    if (getIdentifiers() != null) {
      getIdentifiers().stream().forEach(ident-> {
        sinput.addField(IDENTIFIERS_KEY, ident);
      });
    }
    if (getInstitution() != null) {
      sinput.addField(INSTITUTION_KEY, getInstitution());
    }

    if (getKurator() != null) {
      sinput.addField(KURATOR_KEY, getKurator());
    }

    if (getNavrh() != null) {
      sinput.addField(NAVRH_KEY, getNavrh());
    }

    if (getPozadavek() != null) {
      sinput.addField(POZADAVEK_KEY, getPozadavek());
    }

    if (getPoznamka() != null) {
      sinput.addField(POZNAMKA_KEY, getPoznamka());
    }
    if (getPriority() != null) {
      sinput.addField(PRIORITY_KEY, getPriority());
    }
    if (getState() != null) {
      sinput.addField(STATE_KEY, getState());
    }

    if (getProcess() != null) {
      sinput.addField(PROCESS_KEY, getProcessAsString());
    }

    if(getTypeOfPeriod() != null) {
      sinput.addField(TYPE_OF_PERIOD_KEY, getTypeOfPeriod());
    }

    if (getTransitionType() != null) {
      sinput.addField(TRANSITION_TYPE_KEY, getTransitionType());
    }

    if (getDeadline() != null) {
      sinput.addField(DEADLINE_KEY, getDeadline());
    }

    if (getDesiredItemState() != null) {
      String desitemstate = getDesiredItemState();
      sinput.addField(DESIRED_ITEM_STATE_KEY, desitemstate);
    }

    if (getDesiredLicense() != null) {
      String desLicense = getDesiredLicense();
      sinput.addField(DESIRED_LICENSE_KEY, desLicense);
    }

    if (getEmail() != null) {
      String email = getEmail();
      sinput.addField(EMAIL_KEY, email);
    }

    if (getTypeOfRequest() != null) {
      String typeOfREquest = getTypeOfRequest();
      sinput.addField(TYPE_OF_REQUEST, typeOfREquest);
    }

    return sinput;

  }



  public static Zadost fromJSON(String json) {

    JSONObject jsonobj = new JSONObject(json);
    if (jsonobj.has(ID_KEY)) {
      Zadost zadost = new Zadost(jsonobj.getString(ID_KEY));

      if (jsonobj.has(TYP_KEY)) {
        zadost.setTyp(jsonobj.getString(TYP_KEY));
      }
      if (jsonobj.has(STATE_KEY)) {
        zadost.setState(jsonobj.getString(STATE_KEY));
      }
      if (jsonobj.has(KURATOR_KEY)) {
        zadost.setKurator(jsonobj.getString(KURATOR_KEY));
      }
      if (jsonobj.has(NAVRH_KEY)) {
        zadost.setNavrh(jsonobj.getString(NAVRH_KEY));
      }
      if (jsonobj.has(POZNAMKA_KEY)) {
        zadost.setPoznamka(jsonobj.getString(POZNAMKA_KEY));
      }
      if (jsonobj.has(POZADAVEK_KEY)) {
        zadost.setPozadavek(jsonobj.getString(POZADAVEK_KEY));
      }
      if (jsonobj.has(DATUM_ZADANI_KEY)) {
        zadost.setDatumZadani(SolrJUtilities.solrDate(jsonobj.getString(DATUM_ZADANI_KEY)));
      }
      if (jsonobj.has(DATUM_VYRIZENI_KEY)) {
        zadost.setDatumVyrizeni(SolrJUtilities.solrDate(jsonobj.getString(DATUM_VYRIZENI_KEY)));
      }
      if (jsonobj.has(FORMULAR_KEY)) {
        zadost.setFormular(jsonobj.getString(FORMULAR_KEY));
      }
      if (jsonobj.has(USER_KEY)) {
        zadost.setUser(jsonobj.getString(USER_KEY));
      }
      if (jsonobj.has(INSTITUTION_KEY)) {
        zadost.setInstitution(jsonobj.getString(INSTITUTION_KEY));
      }
      if (jsonobj.has(DELEGATED_KEY)) {
        zadost.setDelegated(jsonobj.getString(DELEGATED_KEY));
      }
      if (jsonobj.has(PRIORITY_KEY)) {
        zadost.setPriority(jsonobj.getString(PRIORITY_KEY));
      }
      if (jsonobj.has(VERSION_KEY) || jsonobj.has("_version_")) {
        if (jsonobj.has(VERSION_KEY)) zadost.setVersion(jsonobj.optString(VERSION_KEY));
        else if (jsonobj.has("_version_")) zadost.setVersion(""+jsonobj.optLong("_version_"));
      }
      if (jsonobj.has(IDENTIFIERS_KEY)) {
        List<Object> identifiers = new ArrayList<>();
        jsonobj.getJSONArray(IDENTIFIERS_KEY).forEach(identifiers::add);
        zadost.setIdentifiers(identifiers.stream().map(Objects::toString).collect(Collectors.toList()));
      }

      if (jsonobj.has(PROCESS_KEY)) {
        Object object = jsonobj.get(PROCESS_KEY);

        if (zadost.getProcess() == null) {
          zadost.setProcess(new HashMap<>());
        }

        // two type of synchronization as text as json
        if (object instanceof  JSONObject) {
          JSONObject jsonObject = (JSONObject) object;
          jsonObject.keySet().forEach(key-> {
            Object  process = jsonObject.get(key);
            zadost.process.put(key, ZadostProcess.fromJSON(process.toString()));
          });

        } else if (object instanceof String) {

          String process = jsonobj.getString(PROCESS_KEY);
          JSONObject processObject = new JSONObject(process);
          processObject.keySet().forEach(key-> {
            Object o = processObject.get(key);
            zadost.process.put(key, ZadostProcess.fromJSON(o.toString()));
          });


        }
      }

      if(jsonobj.has(TRANSITION_TYPE_KEY)) {
        zadost.setTransitionType(jsonobj.getString(TRANSITION_TYPE_KEY));
      }

      if (jsonobj.has(TYPE_OF_PERIOD_KEY)) {
        zadost.setTypeOfPeriod(jsonobj.getString(TYPE_OF_PERIOD_KEY));
      }

      if (jsonobj.has(DEADLINE_KEY)) {
        zadost.setDeadline(SolrJUtilities.solrDate(jsonobj.getString(DEADLINE_KEY)));
      }

      if (jsonobj.has(DESIRED_ITEM_STATE_KEY)) {
        zadost.setDesiredItemState(jsonobj.getString(DESIRED_ITEM_STATE_KEY));
      }

      if (jsonobj.has(DESIRED_LICENSE_KEY)) {
        zadost.setDesiredLicense(jsonobj.getString(DESIRED_LICENSE_KEY));
      }

      if (jsonobj.has(EMAIL_KEY)) {
        zadost.setEmail(jsonobj.getString(EMAIL_KEY));
      }

      if (jsonobj.has(TYPE_OF_REQUEST)) {
        zadost.setTypeOfRequest(jsonobj.getString(TYPE_OF_REQUEST));
      }
      return zadost;

    } else throw new IllegalArgumentException("given object doesnt contain id ");
  }



  public JSONObject toJSON() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ID_KEY, getId());
    if (getTyp() != null) {
      jsonObject.put(TYP_KEY, getTyp());
    }
    if (getDatumVyrizeni() != null) {
      jsonObject.put(DATUM_VYRIZENI_KEY, SolrJUtilities.solrDateString(getDatumVyrizeni()));
    }
    if (getDatumZadani() != null) {
      jsonObject.put(DATUM_ZADANI_KEY, SolrJUtilities.solrDateString(getDatumZadani()));
    }
    if(getDelegated() != null) {
      jsonObject.put(DELEGATED_KEY, getDelegated());
    }

    if (getFormular() != null) {
      jsonObject.put(FORMULAR_KEY, getFormular());
    }


    if (getUser() != null) {
      jsonObject.put(USER_KEY, getUser());
    }

    if (getIdentifiers() != null) {
      JSONArray jsonArray = new JSONArray();
      getIdentifiers().stream().forEach(jsonArray::put);
      jsonObject.put(IDENTIFIERS_KEY, jsonArray);
    }
    if (getInstitution() != null) {
      jsonObject.put(INSTITUTION_KEY, getInstitution());
    }

    if (getKurator() != null) {
      jsonObject.put(KURATOR_KEY, getKurator());
    }

    if (getNavrh() != null) {
      jsonObject.put(NAVRH_KEY, getNavrh());
    }

    if (getPozadavek() != null) {
      jsonObject.put(POZADAVEK_KEY, getPozadavek());
    }

    if (getPoznamka() != null) {
      jsonObject.put(POZNAMKA_KEY, getPoznamka());
    }
    if (getPriority() != null) {
      jsonObject.put(PRIORITY_KEY, getPriority());
    }
    if (getState() != null) {
      jsonObject.put(STATE_KEY, getState());
    }

    if (getProcess() != null) {
      jsonObject.put(PROCESS_KEY, getProcessAsString());
    }

    if (getVersion() != null) {
      jsonObject.put(VERSION_KEY, getVersion());
    }

    if (getDeadline() != null) {
      jsonObject.put(DEADLINE_KEY, SolrJUtilities.solrDateString(getDeadline()));
    }

    if (getTransitionType() != null) {
      jsonObject.put(TRANSITION_TYPE_KEY, getTransitionType());
    }

    if (getTypeOfPeriod() != null) {
      jsonObject.put(TYPE_OF_PERIOD_KEY, getTypeOfPeriod());
    }

    if (getDesiredItemState() != null) {
      jsonObject.put(DESIRED_ITEM_STATE_KEY, getDesiredItemState());
    }

    if (getDesiredLicense() != null) {
      jsonObject.put(DESIRED_LICENSE_KEY, getDesiredLicense());
    }

    if (getTypeOfRequest() != null) {
      jsonObject.put(TYPE_OF_REQUEST, getTypeOfRequest());
    }
    return jsonObject;

  }


  public static JSONObject save(String js, String username, String version) {
    try {
      Zadost zadost = Zadost.fromJSON(js);
      zadost.user = username;
      return save(zadost);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }

  public static JSONObject save(String js, String username) {
    try {
      Zadost zadost = Zadost.fromJSON(js);
      zadost.user = username;
      return save(zadost);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject saveWithFRBR(String js, String username, String frbr) {

    try {
      Zadost zadost = Zadost.fromJSON(js);
      zadost.user = username;
      SolrClient solr = Indexer.getClient();
      SolrQuery query = new SolrQuery("frbr:\"" + frbr + "\"")
              .setFields("identifier")
              .setRows(10000);
      SolrDocumentList docs = solr.query("catalog", query).getResults();
      for (SolrDocument doc : docs) {
        zadost.identifiers.add((String) doc.getFirstValue("identifier"));
      }
      return save(zadost);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }


  /// move to service

  public static JSONObject markAsProcessed(SolrClient client, String js, String username, String requestProcessedState) {
    Zadost zadost = Zadost.fromJSON(js);
    zadost.kurator = username;
    zadost.datum_vyrizeni = new Date(); // ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    //zadost.state = "processed";
    return save(client, zadost);
  }
  public static JSONObject markAsProcessed(String js, String username, String requestProcessedState) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      return markAsProcessed(solr, js, username, requestProcessedState);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }

  public static JSONObject approve(String identifier, String js, String komentar, String username, String approvestate, String transition) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      return approve(solr, identifier, js, komentar, username, approvestate, transition);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  public static JSONObject approve(SolrClient client, String identifier, String js, String komentar, String username, String approvestate, String transition) {
    try {
      Zadost zadost = Zadost.fromJSON(js);
      String oldProcess = new JSONObject().put("process", zadost.process).toString();

      ZadostProcess zprocess = new ZadostProcess();
      zprocess.setState(approvestate != null ? approvestate : "approved");
      zprocess.setUser(username);
      zprocess.setReason(komentar);
      zprocess.setDate(new Date());
      zprocess.setTransitionName(transition);
      zadost.addProcess(identifier, zprocess);

      String newProcess = new JSONObject().put("process", zadost.process).toString();
      // history must be updated after success
      new HistoryImpl(client).log(zadost.id, oldProcess, newProcess, username, "zadost", zadost.getId());
      return save(client, zadost);
    } catch (JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }


  public static JSONObject reject(String identifier, String js, String reason, String username, String transition) {
    try {
      
      Zadost zadost = Zadost.fromJSON(js);
      if (zadost.process == null) {
        zadost.process = new HashMap<>();
      }
      String oldProcess = new JSONObject().put("process", zadost.process).toString();
      ZadostProcess zprocess = new ZadostProcess();
      zprocess.setState("rejected");
      zprocess.setUser(username);
      zprocess.setReason(reason);
      zprocess.setDate(new Date());
      zprocess.setTransitionName(transition);
      //zadost.process.put(identifier, zprocess);
      zadost.addProcess(identifier, zprocess);

      String newProcess = new JSONObject().put("process", zadost.process).toString();
      new HistoryImpl(Indexer.getClient()).log(zadost.id, oldProcess, newProcess, username, "zadost", zadost.getId());
      return save(zadost);
    } catch (JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  

  public static JSONObject save(Zadost zadost) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      return save(solr, zadost);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }

  public static JSONObject save(SolrClient client, Zadost zadost) {
    try{
      SolrInputDocument idoc =zadost.toSolrInputDocument();

      UpdateRequest updateRequest = new UpdateRequest();
      updateRequest.add(idoc);

      if (zadost.getVersion() != null ) {
        updateRequest.setParam(VERSION_KEY, ""+zadost.getVersion());
      }
      client.add("zadost", idoc);
      client.commit("zadost");
      client.close();
      return zadost.toJSON();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }

  @Override
  public String toString() {
    return "Zadost{" +
            "id='" + id + '\'' +
            ", typ='" + typ + '\'' +
            ", state='" + state + '\'' +
            ", kurator='" + kurator + '\'' +
            ", navrh='" + navrh + '\'' +
            ", poznamka='" + poznamka + '\'' +
            ", pozadavek='" + pozadavek + '\'' +
            ", datum_zadani=" + datum_zadani +
            ", datum_vyrizeni=" + datum_vyrizeni +
            ", formular='" + formular + '\'' +
            ", process=" + process +
            ", identifiers=" + identifiers +
            ", user='" + user + '\'' +
            ", institution='" + institution + '\'' +
            ", delegated='" + delegated + '\'' +
            ", priority='" + priority + '\'' +
            ", typeOfPeriod='" + typeOfPeriod + '\'' +
            ", deadline=" + deadline +
            ", transitionType='" + transitionType + '\'' +
            ", desiredItemState='" + desiredItemState + '\'' +
            ", version=" + version +
            '}';
  }
}
