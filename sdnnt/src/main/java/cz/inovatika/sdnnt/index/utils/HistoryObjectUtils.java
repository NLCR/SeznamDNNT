package cz.inovatika.sdnnt.index.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.model.License;

/**
 * History objects utility class
 */
public class HistoryObjectUtils {

    public static final String STAV_FIELD = "stav";
    public static final String DATE_FIELD = "date";
    public static final String YEAR_FIELD = "rocnik";
    public static final String NUMBER_FIELD = "cislo";
    public static final String USER_FIELD = "user";
    public static final String COMMENT_FIELD = "comment";
    public static final String LICENSE_FIELD = "license";
    public static final String REQUEST_FIELD = "zadost";

    private HistoryObjectUtils() {}

    // v pripade granularity zapisuje i parent
    public static JSONObject historyObjectParent(String stav, String license, String originator, String user, String poznamka, String historyDate) {
        return historyObject(null, null, stav, license, originator, user, poznamka, historyDate);
    }

    public static JSONObject historyObjectGranularityField(
            String numb,  // cislo 
            String yea,  // rok 
            String stav,  // stav
            String license, // licence 
            String originator, // puvodce zmeny
            String user,  //uzivatel
            String poznamka, // poznamka 
            String historyDate, //datum zmeny 
            String group,  //skupina
            String acronym, 
            String pid, 
            String link
            ) {
        JSONObject granularityField = historyObject(numb, yea, stav, license, originator, user, poznamka, historyDate);
        if (group != null) granularityField.put("group", group);
        if (acronym != null) granularityField.put("acronym", acronym);
        if (pid != null) granularityField.put("pid", pid);
        if (link != null) granularityField.put("link", link);
        
        return granularityField;
    }


    private static JSONObject historyObject(String number,String year, String stav, String license, String originator, String user, String poznamka, String historyDate) {
        JSONObject historyObject = new JSONObject();
        historyObject.put(STAV_FIELD, stav);
        historyObject.put(DATE_FIELD, historyDate);
        if (year != null) {
            historyObject.put(YEAR_FIELD, year);
        }
        if (number != null) {
            historyObject.put(NUMBER_FIELD, number);
        }
        if (user != null) {
            historyObject.put(USER_FIELD, user);
        }
        if (poznamka != null) {
            historyObject.put(COMMENT_FIELD, poznamka);
        }
        if (license != null) {
            historyObject.put(LICENSE_FIELD, license);
        }
        if (originator != null) {
            historyObject.put(REQUEST_FIELD, originator);
        }
        return historyObject;
    }


//    public static boolean eqHistoryObjectParent(JSONObject first, JSONObject second) {
//        String firstState = first.optString(HistoryObjectUtils.STAV_FIELD);
//        String firstLicense = first.optString(HistoryObjectUtils.LICENSE_FIELD);
//        String secondState = second.optString(HistoryObjectUtils.STAV_FIELD);
//        String secondLicense = second.optString(HistoryObjectUtils.LICENSE_FIELD);
//        boolean eq = StringUtils.equals(firstState,secondState);
//        if (eq  && firstLicense  != null && secondLicense != null) {
//            eq = StringUtils.equals(firstState,secondState);
//        }
//        return eq;
//    }
//
//    public static boolean eqHistoryObjectGranularity(JSONObject first, JSONObject second) {
//        boolean eq = eqHistoryObjectParent(first, second);
//        if (eq) {
//            String firstYear = first.optString(HistoryObjectUtils.YEAR_FIELD);
//            String secondYear = second.optString(HistoryObjectUtils.YEAR_FIELD);
//            eq = StringUtils.equals(firstYear,secondYear);
//        }
//        return eq;
//    }
    
    /** Raw history from aleph */
    public static void enhanceHistoryByLicense(JSONArray historyArray) {
        String license = null;
        for (int i = 0,ll=historyArray.length(); i < ll; i++) {
            JSONObject oneState = historyArray.getJSONObject(i);
            String st = oneState.optString("stav");
            if (st != null) {
                if (st.equals("A") || st.equals("PA")) {
                    if (license == null) {
                        license = License.dnnto.name();
                    }
                } else if (st.equals("NZ")) {
                    license = License.dnntt.name();
                } else {
                    license = null;
                }
                if (license != null) {
                    oneState.put("license", license);
                }
            }
        }
    }
}
