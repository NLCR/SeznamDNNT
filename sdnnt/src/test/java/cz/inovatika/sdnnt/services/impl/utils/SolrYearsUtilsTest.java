package cz.inovatika.sdnnt.services.impl.utils;

import org.junit.Assert;
import org.junit.Test;

public class SolrYearsUtilsTest {
    
    @Test
    public void testParse() {
        Integer syear = SolrYearsUtils.solrDate("2.7.1884");
        Assert.assertTrue(syear == 1884);

        syear = SolrYearsUtils.solrDate("20.7.1885");
        Assert.assertTrue(syear == 1885);

        syear = SolrYearsUtils.solrDate("16.1.1886");
        Assert.assertTrue(syear == 1886);

        syear = SolrYearsUtils.solrDate("14.1.1888");
        Assert.assertTrue(syear == 1888);
        
        
        Integer solrDate = SolrYearsUtils.solrDate("1940..1941");
        Assert.assertTrue(solrDate == 1941);
    }
}
