package cz.inovatika.sdnnt.utils;

import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

import cz.inovatika.sdnnt.Options;

public class LinksUtilitiesTest {

    @Test
    public void testLink() {
        JSONObject digitalized = Options.getInstance().getJSONObject("digitalized");

        String link = "http://kramerius.safra.cz/search/handle/uuid:2f7e369e-c3ca-11e7-b25d-001b63bd97ba";
        List<String> digitalizedKeys = LinksUtilities.digitalizedKeys(digitalized, Arrays.asList(link));
        System.out.println(digitalizedKeys);
    }
    
}
