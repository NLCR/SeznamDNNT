package cz.inovatika.sdnnt.index;

import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.*;

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
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;

// OAI U
public class OAIHarvesterTTest {
    
    public static final Logger LOGGER = Logger.getLogger(OAIHarvesterTTest.class.getName());

    public static SolrTestServer prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new SolrTestServer();
        prepare.setupBeforeClass("oaiharvest");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("catalog");
        prepare.deleteCores("history");
    }

    
    /** Test pro indexaci OAI zaznamu a naslednemu udpatu */
    @Test
    public void testOAIIndexAndUpdate() throws XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        try {
            alephImport(skcAlephStream("skc/update/oai_skc1.xml"),31, true, true);
            System.out.println("IMPORTED");
            
            AccountService accountService = new AccountServiceImpl();
            Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-001579067", "A", License.dnnto.name(),"poznamka", new JSONArray(), "test");

            alephImport(skcAlephStream("skc/update/oai_skc1_changed.xml"),31, true, true);
            System.out.println("IMPORTED");

//            try(SolrClient client = SolrTestServer.getClient()) {
//                
//                SolrQuery query = oai:aleph-nkp.cz:SKC01-001579067new SolrQuery("*")
//                        .setRows(1000);
//                SolrDocumentList docs = client.query(DataCollections.catalog.name(), query).getResults();
//                for (SolrDocument doc : docs) {
//                    MarcRecord fDoc = MarcRecord.fromDoc(doc);
//                    // only this one has dnnnt granularity
//                    JSONArray historyJSON = fDoc.historie_stavu;
//                    if (historyJSON != null) {
//
//                        List<String> licenses = new ArrayList<>();
//                        List<String> states = new ArrayList<>();
//                        for (Object obj : historyJSON) {
//                            JSONObject jObject = (JSONObject) obj;
//                            states.add(jObject.getString("stav"));
//                            licenses.add(jObject.getString("license"));
//                        }
//                        
//                        if (fDoc.identifier.equals("oai:aleph-nkp.cz:DNT01-000143604") || 
//                            fDoc.identifier.equals("oai:aleph-nkp.cz:DNT01-000143602")) {
//                            Assert.assertTrue(licenses.equals(Arrays.asList("dnnto","dnntt","dnntt")));
//                            Assert.assertTrue(states.equals(Arrays.asList("PA","NZ","A")));
//                        } else {
//                            Assert.assertTrue(licenses.equals(Arrays.asList("dnnto","dnnto")));
//                            Assert.assertTrue(states.equals(Arrays.asList("PA","A")));
//                            
//                        }
//                    }
//                }
//            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    /** Test indexace DNT, indexace odpovidajici SKC a naslednemu update SKC */
    @Test
    public void testIndexAndFollow() throws XMLStreamException, SolrServerException {
    }
}
