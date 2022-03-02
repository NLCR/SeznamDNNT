package cz.inovatika.sdnnt.index.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * History objects
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

    public static JSONObject historyObjectParent(String stav, String license, String originator, String user, String poznamka, String historyDate) {
        return historyObject(null, null, stav, license, originator, user, poznamka, historyDate);
    }

    public static JSONObject historyObjectGranularityField(String year, String number, String stav, String license, String originator, String user, String poznamka, String historyDate) {
        return historyObject(year, number, stav, license, originator, user, poznamka, historyDate);
    }

    public static JSONObject historyObjectGranularityField(JSONObject granularityField, String user, String poznamka, String historyDate) {
        return historyObjectGranularityField(
                granularityField.optString(GranularityUtils.NUMBER_FIELD),
                granularityField.optString(GranularityUtils.YEAR_FIELD),
                // musi mit alespon jedno pole ?
                granularityField.optJSONArray(GranularityUtils.STAV_FIELD).getString(0),
                granularityField.optString(GranularityUtils.LICENSE_FIELD),
        null,
                user,
                poznamka,
                historyDate
                );
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


    public static boolean eqHistoryObjectParent(JSONObject first, JSONObject second) {
        String firstState = first.optString(HistoryObjectUtils.STAV_FIELD);
        String firstLicense = first.optString(HistoryObjectUtils.LICENSE_FIELD);
        String secondState = second.optString(HistoryObjectUtils.STAV_FIELD);
        String secondLicense = second.optString(HistoryObjectUtils.LICENSE_FIELD);
        boolean eq = StringUtils.equals(firstState,secondState);
        if (eq  && firstLicense  != null && secondLicense != null) {
            eq = StringUtils.equals(firstState,secondState);
        }
        return eq;
    }

    public static boolean eqHistoryObjectGranularity(JSONObject first, JSONObject second) {
        boolean eq = eqHistoryObjectParent(first, second);
        if (eq) {
            String firstYear = first.optString(HistoryObjectUtils.YEAR_FIELD);
            String secondYear = second.optString(HistoryObjectUtils.YEAR_FIELD);
            eq = StringUtils.equals(firstYear,secondYear);
        }
        return eq;
    }
}
