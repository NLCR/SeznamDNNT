package cz.inovatika.sdnnt.indexer.models.oai;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;

public class MarcModelOAIFormatsTests {

    @Ignore
    @Test
    public void testMarcModelOAIFormatsTests() throws IOException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("oai_aleph-nkp.cz_SKC01-004214235.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        JSONObject obj = new JSONObject(jsonString);
        
        MarcRecord marcRecord = MarcRecord.fromRAWJSON(obj.getString("raw"));
        
        List<DataField> a222 = marcRecord.dataFields.get("222");
        System.out.println(a222);
        
        List<DataField> a245 = marcRecord.dataFields.get("245");
        System.out.println(a245);
        String record = OAIMetadataFormat.dilia.record(marcRecord);
        System.out.println(record);
        
    }
    
}
