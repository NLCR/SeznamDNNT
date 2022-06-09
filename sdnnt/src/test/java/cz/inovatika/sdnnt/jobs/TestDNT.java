package cz.inovatika.sdnnt.jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.sched.Job;
import cz.inovatika.sdnnt.services.impl.AbstractCheckDeleteService;
import cz.inovatika.sdnnt.services.impl.DNTSKCPairServiceImpl;
import cz.inovatika.sdnnt.utils.QuartzUtils;

public class TestDNT {

    public static final String JSON_STATE = "{\r\n"
            + "                        \"type\":\"dnt_skc_pair\",\r\n"
            + "                        \"cron\":\"0 10 11 ? * TUE *\",\r\n"
            + "                        \"results\":{\r\n"
            + "                                \"state\":\"D\"\r\n"
            + "                        }\r\n"
            + "            }";
    
    public static final String JSON_REQUEST = "{\r\n"
            + "    \"type\": \"dnt_skc_pair\",\r\n"
            + "    \"cron\": \"0 10 11 ? * TUE *\",\r\n"
            + "    \"results\": {\r\n"
            + "        \"request\": {}\r\n"
            + "    }\r\n"
            + "}";
    
    public static final Logger LOGGER = Logger.getLogger(TestDNT.class.getName());
    
    public static void doPerform() {
        
        JSONObject jobData = new JSONObject(JSON_REQUEST);
        
        long start = System.currentTimeMillis();
        JSONObject results = jobData.optJSONObject("results");
        JSONArray jsonArrayOfStates = jobData.optJSONArray("states");
        List<String> states = new ArrayList<>();
        if (jsonArrayOfStates != null) {
            jsonArrayOfStates.forEach(it -> {
                states.add(it.toString());
            });
        }
        String loggerPostfix = jobData.optString("logger");
        AbstractCheckDeleteService service = new DNTSKCPairServiceImpl(loggerPostfix, results);
        try {
            service.update();
        } catch (IOException | SolrServerException e) {
            service.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }finally {
            QuartzUtils.printDuration(service.getLogger(), start);
        }
    }
    
    public static void main(String[] args) {
        doPerform();
        //System.out.println(JSON_STATE);
    }
}
