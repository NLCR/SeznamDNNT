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
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.MarcRecordFlags;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

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
            Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-001579067", "A", License.dnnto.name(),"poznamka", new JSONArray(), "test");

            alephImport(skcAlephStream("skc/update/oai_skc1_changed.xml"),31, true, true);

            try(SolrClient client = SolrTestServer.getClient()) {
                
                SolrQuery catalogQuery = new SolrQuery("identifier:\"oai:aleph-nkp.cz:SKC01-001579067\"")
                        .setRows(1000);
                SolrDocumentList catalogDocs = client.query(DataCollections.catalog.name(), catalogQuery).getResults();
                Assert.assertTrue(catalogDocs.size() == 1);
                
                MarcRecord fDoc = MarcRecord.fromDocDep(catalogDocs.get(0));

                Assert.assertNotNull(fDoc.dntstav);
                Assert.assertTrue(fDoc.dntstav.equals(Arrays.asList("A")));

                Assert.assertNotNull(fDoc.kuratorstav);
                Assert.assertTrue(fDoc.kuratorstav.equals(Arrays.asList("A")));
                
                Assert.assertNotNull(fDoc.license);
                Assert.assertTrue(fDoc.license.equals("dnnto"));

                SolrQuery historyQuery = new SolrQuery("*")
                        .setRows(1000);
                SolrDocumentList historyDocs = client.query(DataCollections.history.name(), historyQuery).getResults();
                Assert.assertTrue(historyDocs.size() == 5);
                    
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /** Test indexace DNT, indexace odpovidajici SKC a naslednemu update SKC */
    @Test
    public void testLinkDK() throws XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        try {
            alephImport(skcAlephStream("skc/update/oai_skc1.xml"),31, true, true);
            try(SolrClient client = SolrTestServer.getClient()) {
                MarcRecord mr = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:SKC01-001579065");
                if (mr != null) {
                    if (mr.recordsFlags == null) {
                        mr.recordsFlags = new MarcRecordFlags(true);
                    }
                }
                mr.recordsFlags.setPublicInDl(true);
                
                SolrInputDocument solrDoc = mr.toSolrDoc();
                client.add("catalog", solrDoc);

                SolrJUtilities.quietCommit(client, "catalog");
                alephImport(skcAlephStream("skc/update/oai_skc1_changed.xml"),31, true, true);                

                SolrJUtilities.quietCommit(client, "catalog");

                MarcRecord mr2 = MarcRecord.fromIndex(client, "oai:aleph-nkp.cz:SKC01-001579065");
                Assert.assertNotNull(mr2.recordsFlags);
                Assert.assertTrue(mr2.recordsFlags.isPublicInDl());
            }
        } catch(Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    @Test
    public void testDelete() {
        
    }
}
