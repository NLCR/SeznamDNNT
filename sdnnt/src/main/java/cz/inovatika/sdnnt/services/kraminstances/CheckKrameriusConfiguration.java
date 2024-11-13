package cz.inovatika.sdnnt.services.kraminstances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;


public class CheckKrameriusConfiguration {

    public static final int DEFAULT_BUFFER_SIZE = 90;
    
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private List<String> matchingNames = new ArrayList<>();

    private Map<String, InstanceConfiguration> matchNameConfigurations = new HashMap<>();
    private Map<String, InstanceConfiguration> apiConfiguration = new HashMap<>();
    
    
    private CheckKrameriusConfiguration() {}

    public void add(String key, InstanceConfiguration conf) {
        this.matchingNames.add(key);
        this.matchNameConfigurations.put(key, conf);
        this.apiConfiguration.put(conf.getApiPoint(), conf);
    }
    
    public InstanceConfiguration match(String key) {
        // only name configuration
        Set<String> keySet = this.matchNameConfigurations.keySet();
        for (String kIter : keySet) {
            boolean matches = key.matches(kIter);
            if (matches) return this.matchNameConfigurations.get(kIter);
        }
        
        return null;
    }
    
    public void remove(String key) {
        InstanceConfiguration configuration = this.matchNameConfigurations.get(key);
        if (configuration != null) {
            this.apiConfiguration.remove(configuration.getApiPoint());
            this.matchNameConfigurations.remove(key);
            this.matchingNames.remove(key);
        }
    }

    public List<InstanceConfiguration> getInstances() {
        List<InstanceConfiguration> instances = new ArrayList<>(this.matchNameConfigurations.values());
        return instances;
    }
    
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    
    public InstanceConfiguration findByAcronym(String acronym) {
        Collection<InstanceConfiguration> values = this.matchNameConfigurations.values();
        for (InstanceConfiguration inst : values) {
            if (inst.getAcronym() != null && inst.getAcronym().equals(acronym)) return inst;
        }
        return null;
    }
    public InstanceConfiguration findBySigla(String sigla) {
        Collection<InstanceConfiguration> values = this.matchNameConfigurations.values();
        for (InstanceConfiguration inst : values) {
            if (inst.matchSigla(sigla)) return inst;
            else {
                String acronym = inst.getAcronym();
                if (acronym != null && acronym.toUpperCase().equals(sigla)) {
                    return inst;
                }
            }
        }
        return null;
    }
    
    // supports method
    // find baseUrl
    public String baseUrl(String surl) {
        if (surl.contains("/search/")) {
            String val = surl.substring(0, surl.indexOf("/search") + "/search".length());
            String foundByPrefix = findValueByPrefix(val);
            return foundByPrefix != null ? normalizeValue(foundByPrefix) : val;
        } else {
            List<String> prefixes = Arrays.asList("view", "uuid","periodical");
            for (String pref : prefixes) {
                if (surl.contains(pref)) {
                    String remapping = normalizeValue(surl.substring(0, surl.indexOf(pref)));
                    
                    String foundByPrefix = findValueByPrefix(remapping);
                    return foundByPrefix != null ? normalizeValue(foundByPrefix)
                            : (remapping + (remapping.endsWith("/") ? "" : "/") + "search");
                }
            }
            return null;
        }
    }

    private String normalizeValue(String foundByPrefix) {
        if (foundByPrefix != null && foundByPrefix.endsWith("/")) {
            foundByPrefix = foundByPrefix.substring(0, foundByPrefix.length() -1);
        }
        return foundByPrefix;
    }
    
    public String findValueByPrefix( String val) {
        InstanceConfiguration conf = match(val);
        return conf != null ? conf.getApiPoint() : null;
    }

    public static CheckKrameriusConfiguration initConfiguration(JSONObject checkObject) {
        CheckKrameriusConfiguration check = new CheckKrameriusConfiguration();
        if (checkObject != null) {
            if (checkObject.has("buffersize")) {
                check.setBufferSize(checkObject.getInt("buffersize"));
            }
            if (checkObject.has("urls")) {
                JSONObject urls = checkObject.getJSONObject("urls");
                Set<String> keys = urls.keySet();
                for (String key : keys) {
                    JSONObject instanceObj = urls.getJSONObject(key);
                    InstanceConfiguration conf = InstanceConfiguration.initConfiguration(key, instanceObj);
                    check.add(key, conf);
                }
                
            }
        }
        
        return check;
    }
    
}
