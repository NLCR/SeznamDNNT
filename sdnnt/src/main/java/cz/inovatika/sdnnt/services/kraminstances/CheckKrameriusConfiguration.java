package cz.inovatika.sdnnt.services.kraminstances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
        this.apiConfiguration.put(normalizeValue(conf.getApiPoint()), conf);
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
            this.apiConfiguration.remove(normalizeValue(configuration.getApiPoint()));
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
    
    /**
     * Resolve the API endpoint used for Kramerius checks from a record link.
     */
    public String resolveApiPoint(String surl) {
        String apiCandidate = apiCandidate(surl);
        if (apiCandidate == null) {
            return null;
        }

        String configuredApiPoint = findValueByPrefix(apiCandidate);
        if (configuredApiPoint != null) {
            return normalizeValue(configuredApiPoint);
        }

        return fallbackApiPoint(apiCandidate);
    }

    // supports method
    // find baseUrl
    public String baseUrl(String surl) {
        return resolveApiPoint(surl);
    }

    private String apiCandidate(String surl) {
        if (surl.contains("/search/")) {
            return normalizeValue(surl.substring(0, surl.indexOf("/search") + "/search".length()));
        }

        List<String> prefixes = Arrays.asList("view", "uuid","periodical");
        for (String pref : prefixes) {
            if (surl.contains(pref)) {
                return normalizeValue(surl.substring(0, surl.indexOf(pref)));
            }
        }

        return null;
    }

    private String fallbackApiPoint(String apiCandidate) {
        if (apiCandidate.endsWith("/search")) {
            return apiCandidate;
        }
        return apiCandidate + (apiCandidate.endsWith("/") ? "" : "/") + "search";
    }

    private String normalizeValue(String foundByPrefix) {
        if (foundByPrefix != null && foundByPrefix.endsWith("/")) {
            foundByPrefix = foundByPrefix.substring(0, foundByPrefix.length() -1);
        }
        return foundByPrefix;
    }
    
    public String findValueByPrefix( String val) {
        InstanceConfiguration conf = matchWithOptionalSlash(val);
        return conf != null ? conf.getApiPoint() : null;
    }

    public InstanceConfiguration findByApiPoint(String apiPoint) {
        String normalizedApiPoint = normalizeValue(apiPoint);
        InstanceConfiguration conf = this.apiConfiguration.get(normalizedApiPoint);
        if (conf == null && normalizedApiPoint != null && !normalizedApiPoint.endsWith("/")) {
            conf = this.apiConfiguration.get(normalizedApiPoint + "/");
        }
        return conf;
    }

    private InstanceConfiguration matchWithOptionalSlash(String val) {
        InstanceConfiguration conf = match(val);
        if (conf == null && val != null && !val.endsWith("/")) {
            conf = match(val + "/");
        }
        return conf;
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
