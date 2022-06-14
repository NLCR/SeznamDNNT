package cz.inovatika.sdnnt.index.utils.torefactor;

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
}
