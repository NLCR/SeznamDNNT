package cz.inovatika.sdnnt.index.utils.torefactor;

import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.Assert;
import org.junit.Test;

public class MarcRecordUtilsToRefactorTest {

    @Test
    public void testMarcRecordUtilsToRefactor() {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        MarcRecordUtilsToRefactor.setFMT(solrInputDocument, "t", "s");
        SolrInputField field = solrInputDocument.getField("fmt");
        Assert.assertTrue(field.getFirstValue() != null);
        Assert.assertTrue(field.getFirstValue().equals("SE"));
    }
    
    @Test
    public void testMarcRecordUtilsToRefactor2() {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        MarcRecordUtilsToRefactor.setFMT(solrInputDocument, "t", "i");
        SolrInputField field = solrInputDocument.getField("fmt");
        Assert.assertTrue(field.getFirstValue() != null);
        Assert.assertTrue(field.getFirstValue().equals("SE"));
    }

    @Test
    public void indexesAdditionalIsbnMarcFields() {
        Map<String, List<DataField>> dataFields = new HashMap<>();
        dataFields.put("902", Arrays.asList(dataField("902", "isbn-902")));

        SolrInputDocument document = new SolrInputDocument();
        MarcRecordUtilsToRefactor.marcFields(document, dataFields, MarcRecord.tagsToIndex);

        Assert.assertEquals("isbn-902", document.getFieldValue("marc_902a"));
    }

    private DataField dataField(String tag, String value) {
        DataField dataField = new DataField(tag, " ", " ", 0);
        dataField.getSubFields().put("a", Arrays.asList(new SubField("a", value, 0)));
        return dataField;
    }
}
