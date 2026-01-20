package cz.inovatika.sdnnt.index.imports;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class XMLReadSupportTest {

    @Test
    public void testReadXML() throws XMLStreamException, SolrServerException {
        InputStream resourceAsStream = XMLReadSupportTest.class.getResourceAsStream("heureka.xml");
        Assert.assertNotNull(resourceAsStream);
        XMLReadSupport readSupport = new XMLReadSupport(resourceAsStream, "SHOPITEM", new LinkedHashSet<>());

        List<Map<String,String>> itms = new ArrayList<>();

        readSupport.parse((item)-> {
            Assert.assertTrue(item.containsKey("ISBN"));
            Assert.assertTrue(item.containsKey("PRODUCTNAME"));
            Assert.assertTrue(item.containsKey("stock_availability"));
            itms.add(item);
        });

        Assert.assertEquals(3, itms.size());

    }
}
