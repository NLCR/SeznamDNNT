/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.model;

import java.util.Date;

import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ZadostProcess {

  public static final String USER_KEY = "user";
  public static final String DATE_KEY = "date";
  public static final String REASON_KEY = "reason";
  public static final String STATE_KEY = "state";

  private Date date;
  private String user;
  private String reason;
  private String state;

  public ZadostProcess() {}

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public static ZadostProcess fromJSON(String json)  {
    JSONObject jsonObject = new JSONObject(json);
    ZadostProcess zp = new ZadostProcess();
    if (jsonObject.has(USER_KEY)) {
      zp.setUser(jsonObject.getString(USER_KEY));
    }

    if (jsonObject.has(DATE_KEY)) {
      zp.setDate(new Date(jsonObject.getLong(DATE_KEY)));
    }
    if (jsonObject.has(REASON_KEY)) {
      zp.setReason(jsonObject.optString(REASON_KEY));
    }
    if(jsonObject.has(STATE_KEY)) {
      zp.setState(jsonObject.optString(STATE_KEY));
    }
    return zp;
  }
  
  public JSONObject toJSON() {
    JSONObject jsonObject = new JSONObject();
    if (getUser() != null) {
      jsonObject.put(USER_KEY, getUser());
    }
    if (getReason() != null) {
      jsonObject.put(REASON_KEY, getReason());
    }
    if (getState() != null) {
      jsonObject.put(STATE_KEY, getState());
    }
    if (getDate() != null) {
      jsonObject.put(DATE_KEY, getDate().getTime());
    }
    return jsonObject;
  }
}
