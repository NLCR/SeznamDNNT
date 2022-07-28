package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.TestChangeProcessState;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.MarcModelTestsUtils;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.RAW_FIELD;

public class MarcModelTests {

    @Test
    public void deserializeFromRAW2JSON() throws IOException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("oai_aleph-nkp.cz_DNT01-000157317_RAW.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        MarcRecord marcRecord = MarcRecord.fromRAWJSON(jsonString);
        marcRecord.dntstav = new ArrayList<>(Arrays.asList("PA"));
        System.out.println(marcRecord.license);
        System.out.println(marcRecord.dntstav);
        ChangeProcessStatesUtility.changeProcessState("A", marcRecord, "message");
        System.out.println(marcRecord.dntstav);
        System.out.println(marcRecord.license);
    }
    
    @Test
    public void deserializeFromRAWJSON() throws IOException, SolrServerException {
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
    public void readFromSolrDoc() throws IOException, SolrServerException {
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
        Assert.assertNotNull(marcRecord.dntstav);
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("PA"));

    }

    @Test
    public void readFromSolrDoc2() throws IOException, SolrServerException {
        MarcRecord marcRecord = MarcRecord.fromDocDep(prepareResultList("oai:aleph-nkp.cz:DNT01-000157317".replaceAll("\\:","_")).get(0));
        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.dntstav);
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("PA"));

        // nenastaveny kuratorsky stav; vyplni dle verejneho stavu
        Assert.assertNotNull(marcRecord.kuratorstav);
        Assert.assertTrue(marcRecord.kuratorstav.equals(Arrays.asList("PA")));
        Assert.assertNotNull(marcRecord.historie_kurator_stavu);
        Assert.assertNotNull(marcRecord.datum_krator_stavu);
        Assert.assertTrue(marcRecord.datum_krator_stavu.equals(marcRecord.datum_stavu));

    }

    @Test
    public void readFromSolrDoc4() throws IOException, SolrServerException {
        MarcRecord marcRecord = MarcRecord.fromDocDep(prepareResultList("oai:aleph-nkp.cz:DNT01-000106789".replaceAll("\\:","_")).get(0));
        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.dntstav);
        Assert.assertTrue(marcRecord.dntstav.size() == 1);

        Assert.assertTrue(marcRecord.dntstav.get(0).equals("N"));
        Assert.assertNotNull(marcRecord.historie_stavu);
        Assert.assertNotNull(marcRecord.datum_stavu);

        // ma kuratorsky stav, datum i historii
        Assert.assertNotNull(marcRecord.kuratorstav);
        Assert.assertTrue(marcRecord.kuratorstav.equals(Arrays.asList("NPA")));
        Assert.assertNotNull(marcRecord.historie_kurator_stavu);
        Assert.assertNotNull(marcRecord.datum_krator_stavu);
    }

    @Test
    public void testHistorieStavu() throws JsonProcessingException {
        MarcRecord marcRecord = new MarcRecord();
        marcRecord.identifier = "oai:aleph-nkp.cz:DNT01-000157317";
        marcRecord.historie_stavu = new JSONArray();
        JSONObject object =  marcRecord.toJSON();
        Assert.assertNotNull(MarcRecord.fromRAWJSON(object.toString()));
    }


    @Test
    public void testRecordFields() throws JsonProcessingException {
        MarcRecord marcRecord = new MarcRecord();
        marcRecord.identifier = "oai:aleph-nkp.cz:DNT01-000157317";
        marcRecord.historie_stavu = new JSONArray();
        marcRecord.recordsFlags = new MarcRecordFlags(true);

        SolrInputDocument solrInputFields = marcRecord.toSolrDoc();
        Assert.assertTrue(solrInputFields.containsKey(FLAG_PUBLIC_IN_DL));
    }


    public static SolrDocumentList prepareResultList(String ident) throws IOException {
        SolrDocumentList documentList = new SolrDocumentList();
        documentList.add(MarcModelTestsUtils.prepareResultDocument(ident));
        documentList.setNumFound(1);
        return documentList;
    }

}