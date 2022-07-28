package cz.inovatika.sdnnt.services.impl.utils;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class MarcUtils {
    
    private static List<Character> MEANINGLESS_CHARACTERS = new ArrayList<>();
    static {
        MEANINGLESS_CHARACTERS.add('.');
        MEANINGLESS_CHARACTERS.add(',');
        MEANINGLESS_CHARACTERS.add(':');
        MEANINGLESS_CHARACTERS.add('-');
        MEANINGLESS_CHARACTERS.add('_');
        MEANINGLESS_CHARACTERS.add('?');
    }
    
    private MarcUtils() {}
    
    
    
    public static String removeMeaningLessCharacters(String input) {
        if (input != null) {
            String stInput = input.trim();
            StringBuilder builder = new StringBuilder();
            char[] charArray = stInput.toCharArray();
            for (char iCh : charArray) {
                if (!MEANINGLESS_CHARACTERS.contains(Character.valueOf(iCh))) {
                    builder.append(iCh);
                }
            }
            
            return builder.toString();
        } else return null;
    }
    
    public static String marc310aValue(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        if (jsonObject.has("dataFields")) {
            JSONObject dataFields = jsonObject.getJSONObject("dataFields");
            if (dataFields.has("310")) {
                JSONArray j310Array = dataFields.getJSONArray("310");
                if (j310Array.length() > 0) {
                    JSONObject j310Item =  j310Array.getJSONObject(0);
                    if (j310Item.has("subFields") && j310Item.getJSONObject("subFields").has("a")) {
                        JSONArray aArray =  j310Item.getJSONObject("subFields").getJSONArray("a");
                        if (aArray.length() > 0) {
                            return aArray.getJSONObject(0).optString("value");
                        }
                    }
                    
                }
            }
        }
        return null;
    }

}
