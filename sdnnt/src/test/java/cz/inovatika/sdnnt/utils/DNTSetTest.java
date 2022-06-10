package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.DntAlephImporter;
import cz.inovatika.sdnnt.index.Indexer;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;

// vytvorit utilitu
public class DNTSetTest {

    public static final Logger LOGGER = Logger.getLogger(DNTSetTest.class.getName());
    private static final int LIMIT = 100;

    public static void main(String[] args) {
        JSONArray institutions = Options.getInstance().getJSONArray("institutions");
        System.out.println(institutions);
    }


//    <add>
//  <doc>
//    <field name="employeeId">05991</field>
//    <field name="office" update="set">Walla Walla</field>
//    <field name="skills" update="add">Python</field>
//  </doc>
//</add>
    public static void post(List<String> bulk) {
        try {
            StringBuilder builder = new StringBuilder("<add>\n");
            bulk.stream().forEach(identifier-> {
                String document = String.format("<doc><field name=\"identifier\">%s</field><field name=\"touch\" update=\"set\">true</field></doc>", identifier);
                builder.append(document);
            });

            builder.append("\n</add>");
            String solrHosts = Options.getInstance().getString("solr.host", "http://localhost:8983/solr/");
            SimplePOST.post(solrHosts+(solrHosts.endsWith("/")?"":"/")+"catalog/update", builder.toString());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }

}
