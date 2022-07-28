package cz.inovatika.sdnnt.services.impl.zahorikutils;

import java.util.List;
import java.util.logging.Logger;

import org.apache.solr.security.AuditEvent.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;

public class ZahorikUtils {
    
    
    static java.util.logging.Logger LOGGER = Logger.getLogger(ZahorikUtils.class.getName());
    
    private ZahorikUtils() {}

    public static int rocnik(JSONObject gItem) {
        int rok = -1;
        if (gItem.has("rocnik")) {
            try {
                String rocnik = gItem.getString("rocnik");

                if (rocnik != null && rocnik.trim().startsWith("[")) {
                    int index = rocnik.indexOf("[");
                    rocnik = rocnik.substring(index+1);
                }
                
                if (rocnik != null && rocnik.trim().endsWith("]")) {
                    int index = rocnik.indexOf("]");
                    rocnik = rocnik.substring(0, index);
                }
                
                
                if (rocnik.contains("-") || rocnik.contains("_") || rocnik.contains("..") ) {
                    // expecting range pattern yyyy - yyyyy 
                    String[] split = new String[0];
                    if (rocnik.contains("-")) {
                        split = rocnik.split("-");
                    } else if (rocnik.contains("_")){
                        split = rocnik.split("_");
                    } else {
                        split = rocnik.split("..");
                    }

                    if (split.length > 1) {
                        rok = parsingYear(split[1].trim());
                    } else if (split.length == 1){
                        rok = parsingYear(split[0]);
                    } else {
                        rok = parsingYear( rocnik);
                    }
                } else {
                    rok = parsingYear(gItem.getString("rocnik").trim());
                }
            } catch (NumberFormatException | JSONException  e) {
                e.printStackTrace();
            }
        }
        return rok;
    }

    private static int parsingYear( String rocnik) {
        try {
            return Integer.parseInt(rocnik);
        } catch (Exception e) {
            LOGGER.warning(String.format("Input date parsing problem '%s'", rocnik));
            return -1;
        }
    }

    public static void BK_DNNTO(String nState, String license, List<JSONObject> items) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem);
            if (license != null && license.equals(License.dnnto.name())) {
                int t2001 = Options.getInstance().getInt("granularity.bk.dnnto_dnnto", 2001);
                //int t2011 = Options.getInstance().getInt("granularity.bk.dnnto_dnntt", 2011);
                // hranice je klouzaval 
                if (rocnik <= t2001) {
                    gItem.put("license", License.dnnto.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                // hranice je klouzava                  
                } else if (rocnik > t2001 && rocnik <= 2007) {
    
                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                    
                // hranice je pevna - je dana smlouvou s dilii 
                } else if (rocnik > 2007) {
    
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
    
                }
            }
        }
    }

    public static void BK_DNNTT(String nState, String license, List<JSONObject> items) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem);
            if (license != null && license.equals(License.dnntt.name())) {
                // hranice je pevna - smlouva s Diliii
                if (rocnik <= 2007) {
    
                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                                 
                    
                } else if (rocnik > 2007) {
    
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                }
            }
        }
    }
    // neprava periodika  
    public static void SE_1_DNNTO(String nState, String license, List<JSONObject> items) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem);
            if (license != null && license.equals(License.dnnto.name())) {
                int t2001 = Options.getInstance().getInt("granularity.bk.dnnto_dnnto", 2001);
                int t2012 = Options.getInstance().getInt("granularity.bk.dnnto_dnntt", 2012);
                // hranice je klouzaval, více než dvacet let 
                if (rocnik <= t2001) {

                    gItem.put("license", License.dnnto.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                // hranice je klouzava                  
                } else if (rocnik > t2001 && rocnik < t2012) {

                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                    
                // hranice je pevna - je dana smlouvou s dilii 
                } else if (rocnik >= t2012) {

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
    public static void SE_2_DNNTO(String nState, String license, List<JSONObject> items) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem);
            if (license != null && license.equals(License.dnnto.name())) {
                int t2012 = Options.getInstance().getInt("granularity.bk.dnnto_dnntt", 2012);
                // hranice je klouzaval, více než dvacet let 
                if (rocnik < t2012) {

                    gItem.put("license", License.dnnto.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                } else if (rocnik >= t2012) {

                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                }
            }
        }
        
        
    }
    
    // stopro pouze typ 1
    public static void SE_1_DNNTT(String nState, String license, List<JSONObject> items) {
        for (JSONObject gItem : items) {
            int rocnik = rocnik(gItem);
            int t2012 = Options.getInstance().getInt("granularity.bk.dnntt_dnntt", 2012);
            if (license != null && license.equals(License.dnntt.name())) {
                // rok je klouzavy 
                if (rocnik < t2012) {

                    gItem.put("license", License.dnntt.name());
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(nState);
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);
                    
                } else if (rocnik >= t2012) {
    
                    JSONArray stavArr = new JSONArray();
                    stavArr.put(PublicItemState.N.name());
                    gItem.put("stav", stavArr);
                    gItem.put("kuratorstav", stavArr);

                }
            }
        }
        
    }
    
}
