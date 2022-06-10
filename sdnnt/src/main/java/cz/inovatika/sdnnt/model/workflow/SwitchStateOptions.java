package cz.inovatika.sdnnt.model.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class SwitchStateOptions {
    
    private String[] options;

    public SwitchStateOptions() {
        super();
        this.options = new String[] {};
    }

    
    
    public SwitchStateOptions(String[] options) {
        super();
        List<String> opts = Arrays.stream(options).filter(it->  {return it !=null && it.length() > 0 ;}).collect(Collectors.toList());
        this.options = opts.toArray(new String[opts.size()]);
    }
    

    public String[] getOptions() {
        return options;
    }
    
    
    @Override
    public String toString() {
        return "SwitchStateOptions [options=" + Arrays.toString(options) + "]";
    }



    public static SwitchStateOptions fromString(String str) {
        return str !=  null  ? new SwitchStateOptions(str.split(",")) : new SwitchStateOptions();
    }
    
    
    
    public static SwitchStateOptions fromJSON(JSONObject json) {
        if (json.has("options")) {
            List<String> list = new ArrayList<>();
            JSONArray jsonArray = json.getJSONArray("options");
            jsonArray.forEach(it-> {
                list.add(it.toString());
            });
            return new SwitchStateOptions((String[]) list.toArray(new String[list.size()]));
        } else return new SwitchStateOptions();
    }
}
