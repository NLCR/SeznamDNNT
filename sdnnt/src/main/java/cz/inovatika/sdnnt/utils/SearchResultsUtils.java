package cz.inovatika.sdnnt.utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Search results utils */
public class SearchResultsUtils {
    private SearchResultsUtils() {}

    public static List<String> getIdsFromResult(JSONObject ret) {
      List<String> ids = new ArrayList<>();
      for (Object o : ret.getJSONObject("response").getJSONArray("docs")) {
        JSONObject doc = (JSONObject) o;
        ids.add("\""+doc.getString("identifier")+"\"");
      }
      return ids;
    }

    public static List<JSONObject> getDocsFromResult(JSONObject ret) {
        List<JSONObject> res = new ArrayList<>();
        for (Object o : ret.getJSONObject("response").getJSONArray("docs")) {
            JSONObject doc = (JSONObject) o;
            res.add(doc);
        }
        return res;
    }
}
