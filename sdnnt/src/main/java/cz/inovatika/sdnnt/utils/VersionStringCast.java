package cz.inovatika.sdnnt.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;

public class VersionStringCast {

    public static JSONObject cast(JSONObject object) {
        Stack<JSONObject> stack = new Stack<>();
        stack.push(object);

        while(!stack.isEmpty()) {
            JSONObject popped = stack.pop();
            Set<String> keys = popped.keySet();

            new ArrayList<>(keys).forEach(key-> {
                Object o = popped.get(key);
                if (o instanceof JSONObject) {
                    stack.push((JSONObject)o);
                } else if (o instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) o;
                    for (int i=0,ll=jsonArray.length();i<ll;i++) {
                        Object o1 = jsonArray.get(i);
                        if (o1 instanceof JSONObject) {
                            stack.push((JSONObject) o1);
                        }
                    }
                } else {
                    if (key.equals("_version_")) {
                        long version = popped.getLong("_version_");
                        popped.remove("_version_");
                        // cast to string
                        popped.put("version", ""+version);
                    }
                }

            });
        }
        return object;
    }

}
