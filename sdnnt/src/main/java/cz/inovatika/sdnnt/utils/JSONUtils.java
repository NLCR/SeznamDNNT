package cz.inovatika.sdnnt.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtils {
    
    private JSONUtils() {}

    public static List<String> list(JSONObject parent, String key) {
        Object opt = parent.opt(key);
        if (opt != null) {
            if (opt instanceof JSONArray) {
                JSONArray arr = (JSONArray) opt;
                List<String> retvals = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    retvals.add(arr.get(i).toString());
                }
                return retvals;
            } else {
                return Arrays.asList(opt.toString());
            }
            
        } else return new ArrayList<>();
        
    }
    
    public static String first(JSONObject parent, String key) {
        Object opt = parent.opt(key);
        if (opt instanceof JSONArray) {
            JSONArray arr = (JSONArray) opt;
            return arr.getString(0);
        } else {
            return (String) opt;
        }
    }
}
