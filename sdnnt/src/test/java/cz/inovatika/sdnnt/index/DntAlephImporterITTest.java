package cz.inovatika.sdnnt.index;

import static cz.inovatika.sdnnt.index.DntAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.DntAlephTestUtils.dntAlephStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.services.impl.NotificationServiceImplITTest;

public class DntAlephImporterITTest {

    public static final Logger LOGGER = Logger.getLogger(DntAlephImporterITTest.class.getName());

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("dntaleph");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("catalog");
    }
    
    /** Test na vyplneni historie v pripade PA -> NZ -> A  a prideleni licence*/
    @Test
    public void testDNTAleph_History_DNNT_Serials() throws XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        InputStream resourceAsStream = dntAlephStream("oai_SE_history_PA_NZ_A.xml");
        Assert.assertNotNull(resourceAsStream);
        try {
            alephImport(resourceAsStream,48);

            try(SolrClient client = SolrTestServer.getClient()) {
                
                SolrQuery query = new SolrQuery("*")
                        .setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                for (SolrDocument doc : docs) {
                    MarcRecord fDoc = MarcRecord.fromDoc(doc);
                    // only this one has dnnnt granularity
                    JSONArray historyJSON = fDoc.historie_stavu;
                    if (historyJSON != null) {

                        List<String> licenses = new ArrayList<>();
                        List<String> states = new ArrayList<>();
                        for (Object obj : historyJSON) {
                            JSONObject jObject = (JSONObject) obj;
                            states.add(jObject.getString("stav"));
                            licenses.add(jObject.getString("license"));
                        }
                        
                        if (fDoc.identifier.equals("oai:aleph-nkp.cz:DNT01-000143604") || 
                            fDoc.identifier.equals("oai:aleph-nkp.cz:DNT01-000143602")) {
                            Assert.assertTrue(licenses.equals(Arrays.asList("dnnto","dnntt","dnntt")));
                            Assert.assertTrue(states.equals(Arrays.asList("PA","NZ","A")));
                        } else {
                            Assert.assertTrue(licenses.equals(Arrays.asList("dnnto","dnnto")));
                            Assert.assertTrue(states.equals(Arrays.asList("PA","A")));
                            
                        }
                    }
                }
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

    }



    
    /** Serialy - test na granularitu a stavy  A|PA,NZ; Vysledny stav A|PA a licence dnntt  */
    @Test
    public void testDNTAleph_Granularity_DNNT_Serials() throws XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        InputStream resourceAsStream = dntAlephStream("oai_SE_dnnt.xml");
        Assert.assertNotNull(resourceAsStream);
        try {
            alephImport(resourceAsStream,36);

            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*")
                        .setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                for (SolrDocument doc : docs) {
                    MarcRecord fDoc = MarcRecord.fromDoc(doc);
                    // only this one has dnnnt granularity
                    JSONArray granularityJSON = fDoc.granularity;
                    if (granularityJSON != null) {
                        granularityJSON.forEach(gItem -> {
                            JSONObject gItemJSON = (JSONObject) gItem;
                            Assert.assertTrue( gItemJSON.getJSONArray("stav").length() == 1 );
                            Assert.assertTrue( gItemJSON.getJSONArray("kuratorstav").length() == 1 );
                            if (gItemJSON.has("license")) {
                                String license = gItemJSON.getString("license");
                                if (fDoc.identifier.equals("oai:aleph-nkp.cz:DNT01-000074729")) {
                                    Assert.assertEquals("dnntt", license);
                                } else {
                                    Assert.assertEquals("dnnto", license);
                                }
                            }
                        });
                    }
                    
                }
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    
    /** Nove pole 996 - odkazy do SKC  */
    @Test
    public void testDNTAleph_Pole996() throws XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        InputStream resourceAsStream = dntAlephStream("oai.4_996.xml");
        Assert.assertNotNull(resourceAsStream);
        try {
            alephImport(resourceAsStream,37);

            try(SolrClient client = SolrTestServer.getClient()) {
                SolrQuery query = new SolrQuery("*")
                        .setRows(1000);
                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
                for (SolrDocument doc : docs) {
                    MarcRecord fDoc = MarcRecord.fromDoc(doc);
                    Assert.assertTrue(fDoc.dataFields.containsKey("996"));
                }
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

}
