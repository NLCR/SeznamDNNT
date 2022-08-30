package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.services.impl.hackcerts.HttpsTrustManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class IterationTest {

    public static final Logger LOGGER = Logger.getLogger(IterationTest.class.getName());

    public static void main(String[] args) throws IOException {
        HttpsTrustManager.allowAllSSL();
        changes();
    }

    private static void changes() throws JSONException, IOException {
        List<String> dnntt = new ArrayList<>();
        
        long start = System.currentTimeMillis();
        int totalNumberOfDocs = 0;
        String resumptionToken ="*";
        int rows = 3000;
        //https://sdnnt-test.nkp.cz/sdnnt/api/v1.0/lists/changes?format=BK&rows=1000&resumptionToken=%2A
        //String devBaseUrl = "https://sdnnt-test.nkp.cz/sdnnt/api/v1.0/lists/changes?format=BK&rows="+rows;
        String localhostUrl = "http://localhost:18080/sdnnt/api/v1.0/lists/changes?format=BK&rows="+rows;
        //String baseUrl = "http://localhost:18080/sdnnt/api/v1.0/lists/added/dnnto?rows="+rows;
        int numberOfDocs = 0;
        do {
            
            String url = localhostUrl +"&resumptionToken="+resumptionToken;
            System.out.println(url);
            JSONObject jsonObject = new JSONObject(SimpleGET.get(url));
            JSONArray items = jsonObject.getJSONArray("items");
            items.forEach(item-> {
                JSONObject itemObject = (JSONObject) item;
                if (itemObject.has("pid")) {
                    //pids.ad
                }
                //pids.add(devBaseUrl);
                //pids.add(devBaseUrl)
            });
            numberOfDocs = items.length();
            resumptionToken = jsonObject.getString("resumptiontoken");
            totalNumberOfDocs = totalNumberOfDocs +numberOfDocs;
            LOGGER.info("Total number of docs "+totalNumberOfDocs);
            long time = System.currentTimeMillis() - start;
            LOGGER.info("Time so far "+time+" ms");

        }while(numberOfDocs> 0);
        
    }
    
    private static void license() throws IOException {
        long start = System.currentTimeMillis();
        int totalNumberOfDocs = 0;
        String resumptionToken ="*";
        int rows = 3000;
        String devBaseUrl = "https://sdnnt-dev.nkp.cz/sdnnt/api/v1.0/lists/added/dnnto?rows="+rows;
        String baseUrl = "http://localhost:18080/sdnnt/api/v1.0/lists/added/dnnto?rows="+rows;
        int numberOfDocs = 0;
        do {
            String url = devBaseUrl +"&resumptionToken="+resumptionToken;
            JSONObject jsonObject = new JSONObject(SimpleGET.get(url));
            JSONArray items = jsonObject.getJSONArray("items");
            numberOfDocs = items.length();
            resumptionToken = jsonObject.getString("resumptiontoken");
            totalNumberOfDocs = totalNumberOfDocs +numberOfDocs;
            LOGGER.info("Total number of docs "+totalNumberOfDocs);
            long time = System.currentTimeMillis() - start;
            LOGGER.info("Time so far "+time+" ms");

        }while(numberOfDocs> 0);
    }
}
