package cz.inovatika.sdnnt.indexer.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.beans.Field;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class DataField {

  public String tag;
  public String ind1;
  public String ind2;
  public Map<String, List<SubField>> subFields = new HashMap();

  public DataField() {
    this.tag = "error";
  }

  public DataField(String tag, String ind1, String ind2) {
    this.tag = tag;
    this.ind1 = ind1;
    this.ind2 = ind2;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    json.put("tag", getTag());
    json.put("ind1", getInd1());
    json.put("ind2", getInd2());
    json.put("subFields", getSubFields());
    return json;
  }
  
  public String getTag() {
    return tag;
  }

  /**
   * @return the ind1
   */
  public String getInd1() {
    return ind1;
  }

  /**
   * @return the ind2
   */
  public String getInd2() {
    return ind2;
  }

  /**
   * @return the subFields
   */
  public Map<String, List<SubField>> getSubFields() {
    return subFields;
  }

  /**
   * @param tag the tag to set
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * @param ind1 the ind1 to set
   */
  public void setInd1(String ind1) {
    this.ind1 = ind1;
  }

  /**
   * @param ind2 the ind2 to set
   */
  public void setInd2(String ind2) {
    this.ind2 = ind2;
  }

  /**
   * @param subFields the subFields to set
   */
  public void setSubFields(Map<String, List<SubField>> subFields) {
    this.subFields = subFields;
  }
}
