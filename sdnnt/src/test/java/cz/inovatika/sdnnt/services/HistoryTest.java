package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.services.impl.HistoryImpl;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class HistoryTest {


    protected class HistorySolrInputDocument extends SolrInputDocument {
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof  SolrInputDocument) {
                String myIdentifier = (String) getFieldValue("identifier");
                String myChanges = (String) getFieldValue("changes");
                String myType = (String) getFieldValue("type");
                String myUser = (String) getFieldValue("user");

                SolrInputDocument fDocument = (SolrInputDocument) obj;
                String fIdentifier = (String) fDocument.getFieldValue("identifier");
                String fChanges = (String) fDocument.getFieldValue("changes");
                String fType = (String) fDocument.getFieldValue("type");
                String fUser = (String) fDocument.getFieldValue("user");

                if (myIdentifier != null && fIdentifier != null && myIdentifier.equals(fIdentifier)) {
                    if (myChanges != null && fChanges != null && myChanges.equals(fChanges)) {
                        if (myType != null && fType != null && myType.equals(fType)) {
                            if (myUser != null && fUser != null && myUser.equals(fUser)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            } else return false;
        }
    }

    @Test
    public void testHistory() throws IOException, SolrServerException {
        SolrClient mockClient = EasyMock.createMock(SolrClient.class);
        UpdateResponse mockUpdateResponse = EasyMock.createMock(UpdateResponse.class);

        HistorySolrInputDocument expected = new HistorySolrInputDocument();
        expected.setField("identifier", "oai:aleph-nkp.cz:DNT01-000106789");
        expected.setField("user", "user");
        expected.setField("type", "catalog");
        expected.setField("changes", "{\"forward_patch\":[{\"op\":\"replace\",\"path\":\"/dntstav/0\",\"value\":\"PA\"},{\"op\":\"add\",\"path\":\"/historie_stavu/3\",\"value\":{\"stav\":\"PA\",\"date\":\"20210909\",\"user\":\"user\"}}],\"backward_patch\":[{\"op\":\"replace\",\"path\":\"/dntstav/0\",\"value\":\"N\"},{\"op\":\"remove\",\"path\":\"/historie_stavu/3\"}]}");

        EasyMock.expect(mockClient.add(EasyMock.eq("history"), EasyMock.eq(expected))).andReturn(mockUpdateResponse).times(1);
        EasyMock.expect(mockClient.commit(EasyMock.eq("history"))).andReturn(mockUpdateResponse).times(1);

        EasyMock.replay( mockClient, mockUpdateResponse);
        History history = new HistoryImpl(mockClient);
        history.log("oai:aleph-nkp.cz:DNT01-000106789",stream("old"), stream("new"), "user","catalog");

        EasyMock.verify(mockClient, mockUpdateResponse);
    }

    private String stream(String name) throws IOException {
        InputStream resourceAsStream = HistoryTest.class.getResourceAsStream(name+".json");
        return IOUtils.toString(resourceAsStream, "UTF-8");
    }

}
