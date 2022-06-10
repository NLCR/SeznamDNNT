package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.services.impl.hackcerts.HttpsTrustManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

public class IterationTest {

    public static final Logger LOGGER = Logger.getLogger(IterationTest.class.getName());

    public static void main(String[] args) throws IOException {
        HttpsTrustManager.allowAllSSL();
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
