package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.RAW_FIELD;

public class MarcModelTests {

    @Test
    public void rawJSON() throws IOException, SolrServerException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("oai_aleph-nkp.cz_DNT01-000157317_RAW.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");

        MarcRecord marcRecord = MarcRecord.fromRAWJSON(jsonString);

        Assert.assertEquals(marcRecord.identifier, "oai:aleph-nkp.cz:DNT01-000157317");
        Assert.assertEquals(marcRecord.datestamp, "2021-08-01T12:07:52Z");
        Assert.assertTrue(marcRecord.controlFields.size() == 5);

        Assert.assertTrue(marcRecord.controlFields != null);
        Assert.assertTrue(marcRecord.controlFields.containsKey("008"));
        Assert.assertTrue(marcRecord.controlFields.containsKey("001"));
        Assert.assertTrue(marcRecord.controlFields.containsKey("003"));
        Assert.assertTrue(marcRecord.controlFields.containsKey("005"));
        Assert.assertTrue(marcRecord.controlFields.containsKey("007"));

        Assert.assertTrue(marcRecord.dataFields.containsKey("080"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("072"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("040"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("260"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("250"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("020"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("990"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("100"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("650"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("991"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("035"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("992"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("245"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("300"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("520"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("015"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("246"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("655"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("998"));
        Assert.assertTrue(marcRecord.dataFields.containsKey("504"));
    }

    @Test
    public void solrDoc1() throws IOException, SolrServerException {
        SolrClient mockClient = EasyMock.createMock(SolrClient.class);
        QueryResponse mockResponse = EasyMock.createMock(QueryResponse.class);

        SolrQuery q = new SolrQuery("*").setRows(1)
                .addFilterQuery(IDENTIFIER_FIELD+":\"" + "oai:aleph-nkp.cz:DNT01-000157317" + "\"")
                .setFields(RAW_FIELD+" "+ DNTSTAV_FIELD+" "+ HISTORIE_STAVU_FIELD+" "+ DATUM_STAVU_FIELD+" "+ LICENSE_FIELD +" "+LICENSE_HISTORY_FIELD);


        EasyMock.expect(mockResponse.getResults()).andReturn(prepareResultList("oai:aleph-nkp.cz:DNT01-000157317".replaceAll("\\:","_"))).anyTimes();
        EasyMock.expect(mockClient.query("catalog",q)).andReturn(mockResponse).anyTimes();
        EasyMock.replay(mockClient, mockResponse);

        MarcRecord marcRecord = MarcRecord.fromIndex(mockClient, q);
        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.stav);
        Assert.assertTrue(marcRecord.stav.size() == 2);
        Assert.assertTrue(marcRecord.stav.get(0).equals("PA"));
        Assert.assertTrue(marcRecord.stav.get(1).equals("N"));
    }

    @Test
    public void solrDoc2() throws IOException, SolrServerException {
        MarcRecord marcRecord = MarcRecord.fromDoc(prepareResultList("oai:aleph-nkp.cz:DNT01-000157317".replaceAll("\\:","_")).get(0));
        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.stav);
        Assert.assertTrue(marcRecord.stav.size() == 2);
        Assert.assertTrue(marcRecord.stav.get(0).equals("PA"));
        Assert.assertTrue(marcRecord.stav.get(1).equals("N"));
    }


    public static SolrDocumentList prepareResultList(String ident) throws IOException {
        //"oai_aleph-nkp.cz_DNT01-000157317.json"
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream(ident+".json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        SolrDocumentList documentList = new SolrDocumentList();
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
        documentList.add(document);
        return documentList;
    }
}