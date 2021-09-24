package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class MarcModelTestsUtils {
    private MarcModelTestsUtils() {}

    public static SolrDocument prepareResultDocument(String ident) throws IOException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream(ident+".json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        SolrDocument document = new SolrDocument();
        JSONObject jsonObject = new JSONObject(jsonString);
        jsonObject.keySet().forEach(key-> {
            if (key.equals("datum_stavu")) {

                TemporalAccessor datum = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).parse(jsonObject.get(key).toString());
                document.setField("datum_stavu", Date.from(Instant.from(datum)));

            } else {
                Object o = jsonObject.get(key);
                if (o instanceof String) {
                    document.setField(key, o);
                } else if (o instanceof JSONArray) {
                    JSONArray jArr = (JSONArray) o;
                    document.setField(key, jArr.toList());
                } else  {
                    document.setField(key, o);
                }
            }
        });
        return document;
    }
}