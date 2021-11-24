package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

@Ignore
public class IndexerReduceStateTest {

    @Test
    public void testChangeState_VVS_1() throws IOException, SolrServerException {
//        MarcRecord marcRecord = testReduceState("oai:aleph-nkp.cz:DNT01-000057930", "VVS");
//        Assert.assertTrue(marcRecord.dntstav !=null);
//        Assert.assertTrue(marcRecord.dntstav.get(0).equals("A"));
//        Assert.assertTrue(marcRecord.dntstav.get(1).equals("NZ"));
    }
    @Test
    public void testChangeState_VVN_1() throws IOException, SolrServerException {
//        MarcRecord marcRecord = testReduceState("oai:aleph-nkp.cz:DNT01-000157317", "VVN");
//        Assert.assertTrue(marcRecord.dntstav !=null);
//        Assert.assertTrue(marcRecord.dntstav.get(0).equals("A"));
//        Assert.assertTrue(marcRecord.dntstav.get(1).equals("NZ"));
    }



    private MarcRecord testReduceState(String ident, String navrh) throws IOException, SolrServerException {
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
        Indexer.reduceVisbilityState(ident, navrh, "user", marcRecord, mockClient);
        return marcRecord;
    }

}
