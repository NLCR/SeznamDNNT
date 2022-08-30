package cz.inovatika.sdnnt.index.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handling granularity
 * @author happy
 */
public class GranularityUtils {

    public static final String STAV_FIELD = "stav";
    public static final String KURATORSTAV_FIELD = "stav";
    public static final String LICENSE_FIELD = "license";
    public static final String LINK_FIELD = "link";
    public static final String NUMBER_FIELD = "cislo";
    public static final String YEAR_FIELD = "rocnik";

    private GranularityUtils() {}

    /**
     * Returns true if given objects are equal
     * @param first First item
     * @param second Second item
     */
    public static boolean eqGranularityObject(JSONObject first, JSONObject second) {
        JSONArray fStav = first.optJSONArray(STAV_FIELD);
        JSONArray fKuratorStav = first.optJSONArray(KURATORSTAV_FIELD);
        String fLicense = first.optString(LICENSE_FIELD);
        String fLink = first.optString(LINK_FIELD);
        String fNumber= first.optString(NUMBER_FIELD);

        JSONArray sStav = second.optJSONArray(STAV_FIELD);
        JSONArray sKuratorStav = second.optJSONArray(KURATORSTAV_FIELD);
        String sLicense = second.optString(LICENSE_FIELD);
        String sLink = second.optString(LINK_FIELD);
        String sNumber= second.optString(NUMBER_FIELD);

        if (!jsonArrayCompare(fStav, sStav)) return false;
        if (!jsonArrayCompare(fKuratorStav, sKuratorStav)) return false;
        if (!StringUtils.equals(fLicense, sLicense)) return false;
        if (!StringUtils.equals(fLink, sLink)) return false;
        if (!StringUtils.equals(fNumber, sNumber)) return false;

        return true;
    }

    private static boolean jsonArrayCompare(JSONArray fStav, JSONArray sStav) {
        if (fStav != null && sStav != null ) {
            if (fStav.length() == sStav.length()) {
                for (int i = 0; i < fStav.length(); i++) {
                    if (!StringUtils.equals(fStav.optString(i), sStav.optString(i))) {
                        return false;
                    }
                }
                return true;
            } else return false;
        }
        return false;
    }

    
    /**
     * Returns true if given object represents object item 
     * @param jsonObject
     */
    public static boolean isGranularityItem(JSONObject jsonObject) {
        return (jsonObject.has("cislo") || jsonObject.has("rocnik") || jsonObject.has("fetched"));
    }
    
}
