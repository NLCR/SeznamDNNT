package cz.inovatika.sdnnt.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class DntAlephTestUtils {

    public static InputStream dntAlephStream(String name) {
        return DntAlephImporterITTest.class.getResourceAsStream(name);
    }

    
    public static void alephImport(InputStream resourceAsStream, int expectedSize) throws FactoryConfigurationError,
            XMLStreamException, IOException, JsonProcessingException, SolrServerException {
        XMLStreamReader reader = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            reader = inputFactory.createXMLStreamReader(resourceAsStream);
            Assert.assertNotNull(reader);

            DntAlephImporter importer = new DntAlephImporter();
            importer.readDocument(reader);

            Assert.assertTrue(importer.recs.size() == expectedSize);

            // adding to catalog
            importer.addToCatalog(importer.recs, SolrTestServer.getClient(), false);
            SolrJUtilities.quietCommit(SolrTestServer.getClient(), "catalog");
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public static List<String> document1_ExpectedControlFields001() {
        return Arrays.asList("000012929", "000012930", "000064236", "000110651", "000110652", "000012906", "000012931",
                "000012932", "000012933", "000012934", "000012934", "000110653", "000110654", "000110655", "000110656",
                "000161561", "000161562", "000012938", "000012937", "000012940", "000012941", "000012935", "000012936",
                "000064248", "000064253", "000064254", "000064255", "000110657", "000110658", "000110659", "000110660",
                "000110661", "000110662", "000110663", "os0161565", "os0161566", "000161563", "000161564", "000161567",
                "os0161569", "os0161570", "000161571");
    }

    public static List<String> document2_expectedIDs() {
        return Arrays.asList("oai:aleph-nkp.cz:DNT01-000012929", "oai:aleph-nkp.cz:DNT01-000012930",
                "oai:aleph-nkp.cz:DNT01-000064236", "oai:aleph-nkp.cz:DNT01-000110651",
                "oai:aleph-nkp.cz:DNT01-000110652", "oai:aleph-nkp.cz:DNT01-000012906",
                "oai:aleph-nkp.cz:DNT01-000012931", "oai:aleph-nkp.cz:DNT01-000012932",
                "oai:aleph-nkp.cz:DNT01-000012933", "oai:aleph-nkp.cz:DNT01-000012934",
                "oai:aleph-nkp.cz:DNT01-000012934", "oai:aleph-nkp.cz:DNT01-000110653",
                "oai:aleph-nkp.cz:DNT01-000110654", "oai:aleph-nkp.cz:DNT01-000110655",
                "oai:aleph-nkp.cz:DNT01-000110656", "oai:aleph-nkp.cz:DNT01-000161561",
                "oai:aleph-nkp.cz:DNT01-000161562", "oai:aleph-nkp.cz:DNT01-000012938",
                "oai:aleph-nkp.cz:DNT01-000012937", "oai:aleph-nkp.cz:DNT01-000012940",
                "oai:aleph-nkp.cz:DNT01-000012941", "oai:aleph-nkp.cz:DNT01-000012935",
                "oai:aleph-nkp.cz:DNT01-000012936", "oai:aleph-nkp.cz:DNT01-000064248",
                "oai:aleph-nkp.cz:DNT01-000064253", "oai:aleph-nkp.cz:DNT01-000064254",
                "oai:aleph-nkp.cz:DNT01-000064255", "oai:aleph-nkp.cz:DNT01-000110657",
                "oai:aleph-nkp.cz:DNT01-000110658", "oai:aleph-nkp.cz:DNT01-000110659",
                "oai:aleph-nkp.cz:DNT01-000110660", "oai:aleph-nkp.cz:DNT01-000110661",
                "oai:aleph-nkp.cz:DNT01-000110662", "oai:aleph-nkp.cz:DNT01-000110663",
                "oai:aleph-nkp.cz:DNT01-000161565", "oai:aleph-nkp.cz:DNT01-000161566",
                "oai:aleph-nkp.cz:DNT01-000161566", "oai:aleph-nkp.cz:DNT01-000161563",
                "oai:aleph-nkp.cz:DNT01-000161564", "oai:aleph-nkp.cz:DNT01-000161567",
                "oai:aleph-nkp.cz:DNT01-000161569", "oai:aleph-nkp.cz:DNT01-000161570",
                "oai:aleph-nkp.cz:DNT01-000161571");
    }

}
