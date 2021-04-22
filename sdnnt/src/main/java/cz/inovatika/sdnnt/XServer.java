package cz.inovatika.sdnnt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.XML;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class XServer {

  public static final Logger LOGGER = Logger.getLogger(XServer.class.getName());

  public static JSONObject find(String sysno) {
    JSONObject ret = new JSONObject();
    try {
      String url = Options.getInstance().getString("xserver", "http://aleph.techlib.cz/X?base=stk01") + "&op=find-doc&doc_num=" + sysno;
      // InputStream inputStream = RESTHelper.inputStream(url + "&op=find-doc&doc_num=" + sysno);

      String xml = org.apache.commons.io.IOUtils.toString(new URL(url), "UTF-8");
      ret = XML.toJSONObject(xml)
              .getJSONObject("find-doc")
              .getJSONObject("record")
              .getJSONObject("metadata")
              .getJSONObject("oai_marc");
    } catch (IOException | JSONException ex) {
      ret.put("error", "error getting info from xserver for " + sysno);
      LOGGER.log(Level.SEVERE, "error getting info from xserver for {0}", sysno);
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return ret;
  }

  public static JSONObject find(String field, String val) {
    JSONObject ret = new JSONObject();
    try {
      String url = Options.getInstance().getString("xserver", "http://aleph.techlib.cz/X?base=stk01");
      // InputStream inputStream = RESTHelper.inputStream(url);
      // String xml = org.apache.commons.io.IOUtils.toString(inputStream, "UTF8");
      String xml = org.apache.commons.io.IOUtils.toString(new URL(url + "&op=find&code=" + field + "&request=" + val), "UTF-8");

      JSONObject js = XML.toJSONObject(xml).getJSONObject("find");
      String set_no = js.getString("set_number");
      // http://aleph.net/X?op=present&set_no=000003&set_entry=000000011,000000032&format=marc
      // System.out.println(url + "&op=present&set_no=" + set_no + "&entry=000000001");
      // InputStream is2 = RESTHelper.inputStream(url + "&op=present&set_no=" + set_no + "&set_entry=000000001");
      // xml = org.apache.commons.io.IOUtils.toString(is2, "UTF8");
      xml = org.apache.commons.io.IOUtils.toString(new URL(url + "&op=present&set_no=" + set_no + "&set_entry=000000001"), "UTF-8");

      ret = XML.toJSONObject(xml)
              .getJSONObject("present")
              .getJSONObject("record")
              .getJSONObject("metadata")
              .getJSONObject("oai_marc");
    } catch (IOException | JSONException ex) {
      ret.put("error", "error getting info from xserver for " + field + "=" + val);
      LOGGER.log(Level.SEVERE, "error getting info from xserver for {0}", field + "=" + val);
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return ret;
  }

  public static String getLanguage(JSONObject marc) {
    /*
    Ve formátu MARC 21 jsou jako povinné - tzn. nelze použít výplňový znak - definovány jen pozice (0-5),
    datum uložení záznamu ve tvaru rrmmdd). 
    Na pozici M008 (15-17) se uvádí kód země podle kódovníku MARC code list for countries 
    (liší se od kódovníku UNIMARC, např. kód pro Českou republiku je xr). 
    Pozice (35-37) se používají pouze pro první jazyk textu, není možné označit, 
    zda jde o vícejazyčnou publikace, originál či překlad. 
    Pro SK Caslin je povinné i použití indikátoru, proto je třeba v komplikovanějším případu použít i pole 041 (viz dále).
    
    "fixfield": [
    ...
    {
      "id": "008",
      "content": "191209s2019----xxkabd-f------001-0-eng-d"
    }
  ]
     */
    JSONArray varfield = marc.optJSONArray("fixfield");
    String lang = "";
    for (int i = 0; i < varfield.length(); i++) {
      JSONObject vf = varfield.getJSONObject(i);
      if ("008".equals(vf.optString("id"))) {

        String sb = vf.optString("content");
        if (sb != null) {
          lang = sb.substring(35, 38);
        }
      }
    }
    return lang;
  }

  public static String getTitle(JSONObject info) {
    JSONArray varfield = info.optJSONArray("varfield");
    String title = "";
    for (int i = 0; i < varfield.length(); i++) {
      JSONObject vf = varfield.getJSONObject(i);
      if ("245".equals(vf.optString("id"))) {

        JSONArray sb = vf.optJSONArray("subfield");
        if (sb != null) {
          for (int j = 0; j < sb.length(); j++) {
            //Exclude author label = c
            if (!sb.getJSONObject(j).get("label").equals("c")) {
              title += sb.getJSONObject(j).getString("content") + " ";
            }
          }
        }
      }
    }
    return title;
  }

  public static String getAuthor(JSONObject info) {
    JSONArray varfield = info.optJSONArray("varfield");
    String author = "";
    for (int i = 0; i < varfield.length(); i++) {
      JSONObject vf = varfield.getJSONObject(i);
      if ("100".equals(vf.optString("id"))) {

        JSONArray sb = vf.optJSONArray("subfield");
        if (sb != null) {
          for (int j = 0; j < sb.length(); j++) {
            if (sb.getJSONObject(j).get("label").equals("a")) {
              author += sb.getJSONObject(j).getString("content") + " ";
            }
            if (sb.getJSONObject(j).get("label").equals("d")) {
              author += sb.getJSONObject(j).getString("content") + " ";
            }
          }
        }
      }
    }
    return author;
  }

}
