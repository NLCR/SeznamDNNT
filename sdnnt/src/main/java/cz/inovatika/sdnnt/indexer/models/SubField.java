package cz.inovatika.sdnnt.indexer.models;

/**
 *
 * @author alberto
 */
public class SubField {
  public String code;
  public String value;
  
  public SubField() {
    this.code = "error";
  }
  
  public SubField(String code, String value) {
    this.code = code;
    this.value = value;
  }

  /**
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param code the code to set
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }
}
