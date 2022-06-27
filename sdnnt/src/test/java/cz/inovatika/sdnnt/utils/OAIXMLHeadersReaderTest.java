package cz.inovatika.sdnnt.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.index.utils.OAIXMLHeadersReader;

public class OAIXMLHeadersReaderTest {
    
    @Test
    public void testReadHeaders() throws ParserConfigurationException, SAXException, IOException {
        InputStream iStream = OAIXMLHeadersReaderTest.class.getResourceAsStream("type_check.xml");
        OAIXMLHeadersReader reader = new OAIXMLHeadersReader(iStream);
        reader.readFromXML();
        List<String> records = reader.getRecords();
        List<String> toDelete = reader.getToDelete();
        Assert.assertTrue(records.contains("oai:aleph-nkp.cz:SKC01-009537565"));
        Assert.assertTrue(records.contains("oai:aleph-nkp.cz:SKC01-009466329"));
        Assert.assertTrue(records.contains("oai:aleph-nkp.cz:SKC01-009552083"));
        Assert.assertFalse(records.contains("oai:aleph-nkp.cz:SKC01-000785658"));
        
        Assert.assertFalse(toDelete.contains("oai:aleph-nkp.cz:SKC01-009537565"));
        Assert.assertFalse(toDelete.contains("oai:aleph-nkp.cz:SKC01-009466329"));
        Assert.assertFalse(toDelete.contains("oai:aleph-nkp.cz:SKC01-009552083"));
        Assert.assertTrue(toDelete.contains("oai:aleph-nkp.cz:SKC01-000785658"));
    }
}
