package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import org.apache.commons.lang.time.DateUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;


public class IndexerChangeStateTest {


    @Test
    public void testChangeState_NZN_1() throws IOException, SolrServerException {
        // stav N -> stav PA
        MarcRecord marcRecord = testChangeState("oai:aleph-nkp.cz:DNT01-000106789", "NZN");
        Assert.assertTrue(marcRecord.stav!=null);
        Assert.assertTrue(marcRecord.stav.size()==1);
        Assert.assertTrue(marcRecord.stav.get(0).equals("PA"));

        // underlaying solr document
        Collection<Object> fieldValues = marcRecord.sdoc.getFieldValues(DNTSTAV_FIELD);
        Assert.assertTrue(fieldValues.toString().equals("[PA]"));

        boolean sameday = DateUtils.isSameDay(new Date(),(Date) marcRecord.sdoc.getFieldValue(DATUM_STAVU_FIELD));
        Assert.assertTrue(sameday);

        String history = (String) marcRecord.sdoc.getFieldValue(HISTORIE_STAVU_FIELD);
        JSONArray object = new JSONArray(history);
        Assert.assertTrue(object.length() == 4);

    }

    @Test
    public void testChangeState_NZN_2() throws IOException, SolrServerException {
        // stav  PA -> stav PA
        MarcRecord marcRecord = testChangeState("oai:aleph-nkp.cz:DNT01-000157317", "NZN");
        Assert.assertTrue(marcRecord.stav!=null);
        Assert.assertTrue(marcRecord.stav.get(0).equals("PA"));

        Collection<Object> fieldValues = marcRecord.sdoc.getFieldValues(DNTSTAV_FIELD);
        Assert.assertTrue(fieldValues.toString().equals("[PA]"));

        //Date date = (Date) marcRecord.sdoc.getFieldValue(DATUM_STAVU_FIELD);
        boolean sameday = DateUtils.isSameDay(new Date(),(Date) marcRecord.sdoc.getFieldValue(DATUM_STAVU_FIELD));
        Assert.assertFalse(sameday);

        String history = (String) marcRecord.sdoc.getFieldValue(HISTORIE_STAVU_FIELD);
        JSONArray object = new JSONArray(history);
        Assert.assertTrue(object.length() == 3);
    }

    @Test
    public void testChangeState_VVS_1() throws IOException, SolrServerException {
        MarcRecord marcRecord = testChangeState("oai:aleph-nkp.cz:DNT01-000057930", "VVS");
        Assert.assertTrue(marcRecord.stav!=null);
        Assert.assertTrue(marcRecord.stav.get(0).equals("VS"));

        Collection<Object> fieldValues = marcRecord.sdoc.getFieldValues(DNTSTAV_FIELD);
        Assert.assertTrue(fieldValues.toString().equals("[VS]"));

        //Date date = (Date) marcRecord.sdoc.getFieldValue(DATUM_STAVU_FIELD);
        boolean sameday = DateUtils.isSameDay(new Date(),(Date) marcRecord.sdoc.getFieldValue(DATUM_STAVU_FIELD));
        Assert.assertTrue(sameday);

        String history = (String) marcRecord.sdoc.getFieldValue(HISTORIE_STAVU_FIELD);
        JSONArray object = new JSONArray(history);
        Assert.assertTrue(object.length() == 2);
    }

    @Test
    public void testChangeState_VVS_2() throws IOException, SolrServerException {
        MarcRecord marcRecord = testChangeState("oai:aleph-nkp.cz:DNT01-000106789", "VVS");
        Assert.assertTrue(marcRecord.stav!=null);
        Assert.assertTrue(marcRecord.stav.get(0).equals("N"));

        Collection<Object> fieldValues = marcRecord.sdoc.getFieldValues(DNTSTAV_FIELD);
        Assert.assertTrue(fieldValues.toString().equals("[N]"));

        boolean sameday = DateUtils.isSameDay(new Date(),(Date) marcRecord.sdoc.getFieldValue(DATUM_STAVU_FIELD));
        Assert.assertFalse(sameday);

        String history = (String) marcRecord.sdoc.getFieldValue(HISTORIE_STAVU_FIELD);
        JSONArray object = new JSONArray(history);
        Assert.assertTrue(object.length() == 3);
    }

    @Test
    public void testChangeState_VVN_1() throws IOException, SolrServerException {
        MarcRecord marcRecord = testChangeState("oai:aleph-nkp.cz:DNT01-000057930", "VVN");
        Assert.assertTrue(marcRecord.stav!=null);
        Assert.assertTrue(marcRecord.stav.get(0).equals("A"));

        Collection<Object> fieldValues = marcRecord.sdoc.getFieldValues(DNTSTAV_FIELD);
        Assert.assertTrue(fieldValues.toString().equals("[A]"));

        boolean sameday = DateUtils.isSameDay(new Date(),(Date) marcRecord.sdoc.getFieldValue(DATUM_STAVU_FIELD));
        Assert.assertFalse(sameday);

        String history = (String) marcRecord.sdoc.getFieldValue(HISTORIE_STAVU_FIELD);
        JSONArray object = new JSONArray(history);
        Assert.assertTrue(object.length() == 1);

    }

    private MarcRecord testChangeState(String ident, String navrh) throws IOException, SolrServerException {
        SolrClient mockClient = EasyMock.createMock(SolrClient.class);
        UpdateResponse mockUpdateResponse = EasyMock.createMock(UpdateResponse.class);
        MarcRecord marcRecord = MarcRecord.fromDoc(MarcModelTests.prepareResultList(ident.replaceAll("\\:","_")).get(0));


        EasyMock.expect(mockClient.commit(EasyMock.eq("history"))).andReturn(mockUpdateResponse).anyTimes();

        // commit to history log
        EasyMock.expect(mockClient.add(EasyMock.eq("history"),EasyMock.anyObject(SolrInputDocument.class))).andReturn(mockUpdateResponse).anyTimes();
        // commit to catalog
        EasyMock.expect(mockClient.add(EasyMock.eq("catalog"),EasyMock.anyObject(SolrInputDocument.class))).andReturn(mockUpdateResponse).anyTimes();


        EasyMock.expect(mockClient.commit(EasyMock.eq("catalog"))).andReturn(mockUpdateResponse).anyTimes();

        EasyMock.replay(mockClient,mockUpdateResponse);
        Indexer.changeStav(ident, navrh, "user", marcRecord, mockClient);
        return marcRecord;
    }
}
