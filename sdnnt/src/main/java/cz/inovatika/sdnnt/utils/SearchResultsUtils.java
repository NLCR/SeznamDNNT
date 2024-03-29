package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.model.PublicItemState;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Search results utils */
public class SearchResultsUtils {

    public static final String FACET_COUNTS_KEY = "facet_counts";
    public static final String FACET_FIELDS_KEY = "facet_fields";
    public static final String NAME_KEY = "name";

    private SearchResultsUtils() {}

    /**
     * Returns ids from result
     * @param ret List of ids
     * @return
     */
    public static List<String> getIdsFromResult(JSONObject ret) {
      List<String> ids = new ArrayList<>();
      for (Object o : ret.getJSONObject("response").getJSONArray("docs")) {
        JSONObject doc = (JSONObject) o;
        ids.add("\""+doc.getString("identifier")+"\"");
      }
      return ids;
    }

    public static void  iterateResult(JSONObject ret, Consumer<JSONObject> consumer) {
        List<String> ids = new ArrayList<>();
        for (Object o : ret.getJSONObject("response").getJSONArray("docs")) {
            JSONObject doc = (JSONObject) o;
            consumer.accept(doc);
        }
    }



    public static  void removePublicStatesFromCuratorStatesFacets(JSONObject ret) {
        if (ret.has(FACET_COUNTS_KEY)) {
            // filtr public state from kurator state
            JSONObject fCounts = ret.getJSONObject(SearchResultsUtils.FACET_COUNTS_KEY);
            if (fCounts.has(FACET_FIELDS_KEY)) {
                if(fCounts.getJSONObject(FACET_FIELDS_KEY).has(MarcRecordFields.KURATORSTAV_FIELD)) {
                    JSONArray jsonArray = fCounts.getJSONObject(FACET_FIELDS_KEY).getJSONArray(MarcRecordFields.KURATORSTAV_FIELD);
                    List<String> pStateNames = Arrays.stream(PublicItemState.values()).map(PublicItemState::name).collect(Collectors.toList());
                    JSONArray newJsonArray = new JSONArray();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        if (jsonArray.getJSONObject(i).has(NAME_KEY) && !pStateNames.contains(jsonArray.getJSONObject(i).getString(NAME_KEY))) {
                            newJsonArray.put(jsonArray.getJSONObject(i));
                        }
                    }
                    fCounts.getJSONObject(FACET_FIELDS_KEY).put(MarcRecordFields.KURATORSTAV_FIELD, newJsonArray);
                }
            }
        }
    }
}
