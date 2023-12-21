package cz.inovatika.sdnnt.services.impl.zahorikutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.solr.security.AuditEvent.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;

public class ZahorikUtils {
    // neprava periodika

    private static final String GRANULARITY_SE_NP_DNNTT_DNNTT = "granularity.se.np.dnntt_dnntt";

    private static final String GRANULARITY_SE_NP_DNNTO_DNNTT = "granularity.se.np.dnnto_dnntt";
    private static final String GRANULARITY_SE_NP_DNNTO_DNNTT_ENABLED = "granularity.se.np.dnnto_dnntt_enabled";

    private static final String GRANULARITY_SE_NP_DNNTO_DNNTO = "granularity.se.np.dnnto_dnnto";
    
    private static final String GRANULARITY_SE_P_DNNTO_DNNTO = "granularity.se.p.dnnto_dnnto";

    private static final String GRANULARITY_PUBLIC_LICENSE = "granularity.public_license";

    private static final String GRANULARITY_BK_DNNTO_DNNTO = "granularity.bk.dnnto_dnnto";

    private static final String GRANULARITY_BK_DNNTO_DNNTT = "granularity.bk.dnnto_dnntt";

    private static final String GRANULARITY_BK_DNNTO_DNNTT_ENABLED = "granularity.bk.dnnto_dnntt_enabled";

    private static final String GRANULARITY_BK_DNNTT_DNNTT = "granularity.bk.dnntt_dnntt";
    
    
    
    static java.util.logging.Logger LOGGER = Logger.getLogger(ZahorikUtils.class.getName());
    
    private ZahorikUtils() {}
    
    private static List<String> parseYears(String years) {
        // parsuje skupinu cislic, konci 
        List<String> yearsList = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        char[] charArray = years.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            if (Character.isDigit(charArray[i])) { builder.append(charArray[i]); }
            else {
                if (builder.length() > 0) {
                    yearsList.add(builder.toString());
                    builder = new StringBuilder();
                }
            }
            
        }
        
        if (builder.length() > 0) {
            yearsList.add(builder.toString());
        }
        return yearsList;
    }
    
    
    // move to years utils
    public static int rocnik(JSONObject gItem, Logger logger) {
        int rok = -1;
        if (gItem.has("rocnik")) {
            try {
                String rocnik = gItem.getString("rocnik");
                List<Integer> collected = parseYears(rocnik).stream().map(Integer::valueOf).collect(Collectors.toList());
                Optional<Integer> minimum = collected.stream().filter(it -> it > 1700).min(Integer::compareTo);
                if (minimum.isPresent()) {
                    return minimum.get();
                } else {
                    //System.out.println("error");
                }
                
            } catch (NumberFormatException | JSONException  e) {
                e.printStackTrace();
            }
        }
        return rok;
    }

    
    public static void BK_DNNTO(String nState, String license, List<JSONObject> items, Logger logger) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem, logger);
            if (license != null && license.equals(License.dnnto.name())) {
                int publicLicense = Options.getInstance().intKey(GRANULARITY_PUBLIC_LICENSE, 1912);
                
                int t2003 = Options.getInstance().intKey(GRANULARITY_BK_DNNTO_DNNTO, 2002);
                int t2007 = Options.getInstance().intKey(GRANULARITY_BK_DNNTO_DNNTT, 2007);
                boolean t2007Enabled  = Options.getInstance().boolKey(GRANULARITY_BK_DNNTO_DNNTT_ENABLED, true);
                
                // pokud je mensi nez verejny rocnik
                if (rocnik <= publicLicense) continue;

                // hranice je klouzaval 
                if (rocnik > publicLicense &&  rocnik <= t2003) {
                    gItem.put("license", License.dnnto.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                } else if (rocnik > t2003 && rocnik <= t2007 && t2007Enabled) {
    
                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                    
                // hranice je pevna - je dana smlouvou s dilii 
                } else {
    
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
    
                }
            }
        }
    }
    
    
    
    

    public static void BK_DNNTT(String nState, String license, List<JSONObject> items, Logger logger) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem, logger);

            int publicLicense = Options.getInstance().intKey(GRANULARITY_PUBLIC_LICENSE, 1912);
            int t2003 = Options.getInstance().intKey(GRANULARITY_BK_DNNTT_DNNTT, 2002);

            if (license != null && license.equals(License.dnntt.name())) {
                // pokud je mensi nez verejny rocnik
                if (rocnik <= publicLicense) continue;
                
                if (rocnik > publicLicense && rocnik <= t2003) {
    
                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                    
                } else {
    
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                }
            }
        }
    }
    
    // neprava periodika  
    public static void SE_1_DNNTO(String nState, String license, List<JSONObject> items, Logger logger) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem, logger);
            if (license != null && license.equals(License.dnnto.name())) {
                int publicLicense = Options.getInstance().intKey(GRANULARITY_PUBLIC_LICENSE, 1912);

                int t2003 = Options.getInstance().intKey(GRANULARITY_SE_NP_DNNTO_DNNTO, 2002);
                int t2013 = Options.getInstance().intKey(GRANULARITY_SE_NP_DNNTO_DNNTT, 2012);
                boolean t2013Enabled = Options.getInstance().boolKey(GRANULARITY_SE_NP_DNNTO_DNNTT_ENABLED, true);
                
                // pokud je mensi nez verejny rocnik
                if (rocnik <= publicLicense) continue;
                
                // hranice je klouzaval, více než dvacet let 
                if (rocnik > publicLicense &&   rocnik <= t2003) {

                    gItem.put("license", License.dnnto.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
 
                // hranice je klouzava                  
                } else if (rocnik > t2003 && rocnik <= t2013 && t2013Enabled) {

                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                    
                } else  {

                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                }
            }
        }
    }

    // prava periodika
    // control field 008 pozice 18( a 19(r) 
    public static void SE_2_DNNTO(String nState, String license, List<JSONObject> items, Logger logger) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem, logger);
            if (license != null && license.equals(License.dnnto.name())) {

                int publicLicense = Options.getInstance().intKey(GRANULARITY_PUBLIC_LICENSE, 1912);
                int t2013 = Options.getInstance().intKey(GRANULARITY_SE_P_DNNTO_DNNTO, 2012);

                // pokud je mensi nez verejny rocnik
                if (rocnik <= publicLicense) continue;

                // hranice je klouzaval, více než dvacet let 
                if (rocnik > publicLicense &&  rocnik <= t2013) {

                    gItem.put("license", License.dnnto.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                } else if (rocnik > t2013) {

                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                }
            }
        }
        
        
    }
    
    // neprava dnntt
    public static void SE_1_DNNTT(String nState, String license, List<JSONObject> items, Logger logger) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem, logger);
            int publicLicense = Options.getInstance().intKey(GRANULARITY_PUBLIC_LICENSE, 1912);
            int t2013 = Options.getInstance().intKey(GRANULARITY_SE_NP_DNNTT_DNNTT, 2013);

            // pokud je mensi nez verejny rocnik
            if (rocnik <= publicLicense) continue;

            
            if (license != null && license.equals(License.dnntt.name())) {
                // rok je klouzavy 
                if (rocnik > publicLicense && rocnik <= t2013) {

                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                    
                } else if (rocnik > t2013) {
    
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                }
            }
        }
        
    }
    
    
    public static void main(String[] args) {
        Integer[] array = new Integer[] {2007,2008};
        Optional<Integer> minimum = Arrays.asList(array).stream().max(Integer::compareTo);
        System.out.println(minimum);
    }
    
}
