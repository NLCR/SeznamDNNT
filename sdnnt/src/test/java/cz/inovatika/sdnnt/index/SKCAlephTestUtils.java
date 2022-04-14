package cz.inovatika.sdnnt.index;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class SKCAlephTestUtils {
    
    public static InputStream skcAlephStream(String name) {
        return OAIHarvesterTTest.class.getResourceAsStream(name);
    }

    public static void alephImport(InputStream resourceAsStream, int expectedSize, boolean merge, boolean update) throws FactoryConfigurationError,
            XMLStreamException, IOException, JsonProcessingException, SolrServerException {
        XMLStreamReader reader = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            reader = inputFactory.createXMLStreamReader(resourceAsStream);
            Assert.assertNotNull(reader);

            OAIHarvester importer = new OAIHarvester();
            importer.readDocument(reader);
            System.out.println(importer.recs.size());
            Assert.assertTrue(importer.recs.size() == expectedSize);
            Indexer.add( "catalog", importer.recs, merge, update, "harvester",SolrTestServer.getClient());

            SolrJUtilities.quietCommit(SolrTestServer.getClient(), "catalog");
            SolrJUtilities.quietCommit(SolrTestServer.getClient(), "history");
            
        } finally {
            if (reader != null)
                reader.close();
        }
    }

}
