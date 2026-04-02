package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.index.OAIHarvesterTTest;
import cz.inovatika.sdnnt.it.SolrTestServer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.ResourceServiceService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.ContentStreamBase;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.alephImport;
import static cz.inovatika.sdnnt.index.SKCAlephTestUtils.skcAlephStream;

public class OAIHarvestTTest {

    public static final Logger LOGGER = Logger.getLogger(OAIHarvestTTest.class.getName());

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
        prepare.deleteCores("zadost");
    }

    /** Initial update*/
    @Test
    public void testUpdate() throws FactoryConfigurationError, XMLStreamException, SolrServerException {
        if (!SolrTestServer.TEST_SERVER_IS_RUNNING) {
            LOGGER.warning(String.format("%s is skipping", this.getClass().getSimpleName()));
            return;
        }
        try {
            InputStream is =  OAIHarvesterTTest.class.getResourceAsStream("skc/updateproblem/init.json");
            String json = IOUtils.toString(is, "UTF-8");
            Assert.assertNotNull(is);

            try(SolrClient client = SolrTestServer.getClient("catalog")) {
                ContentStreamUpdateRequest request =
                        new ContentStreamUpdateRequest("/update/json/docs");
                request.addContentStream(
                        new ContentStreamBase.StringStream(json, "application/json")
                );

                client.request(request);
                client.commit();
            }

            alephImport(prepare.getClient(), skcAlephStream("skc/updateproblem/oai_update_problem_update.xml"),33, true, true);
            //Indexer.changeStavDirect(prepare.getClient(), "oai:aleph-nkp.cz:SKC01-000910820", "A","A", License.dnnto.name(),"poznamka",  "zmena na A");

//            try(SolrClient client = SolrTestServer.getClient()) {
//                SolrQuery catalogQuery = new SolrQuery(
//                        "identifier:\"oai:aleph-nkp.cz:SKC01-000910820\"")
//                        .setRows(1000);
//                SolrDocumentList catalogDocs = client.query(DataCollections.catalog.name(), catalogQuery).getResults();
//                Assert.assertTrue(catalogDocs.size() == 1);
//                Assert.assertTrue(catalogDocs.get(0).containsKey(MarcRecordFields.DNTSTAV_FIELD));
//                Assert.assertTrue(catalogDocs.get(0).getFirstValue(MarcRecordFields.DNTSTAV_FIELD).equals(PublicItemState.A.name()));
//                Assert.assertEquals("cnb00problematic", catalogDocs.get(0).getFirstValue("id_ccnb"));
//
//                Object firstValue = catalogDocs.get(0).getFirstValue(MarcRecordFields.HISTORIE_STAVU_FIELD);
//                JSONArray inputArr = new JSONArray(firstValue.toString());
//                Assert.assertTrue(inputArr.length() == 1);
//
//                Assert.assertTrue(inputArr.getJSONObject(0).has("stav"));
//                Assert.assertTrue(inputArr.getJSONObject(0).getString("stav").equals(PublicItemState.A.name()));
//            }
//
//
//            alephImport(prepare.getClient(), skcAlephStream("skc/updateproblem/oai_update_problem_update.xml"),33, true, true);
//
//            try(SolrClient client = SolrTestServer.getClient()) {
//                SolrQuery catalogQuery = new SolrQuery(
//                        "identifier:\"oai:aleph-nkp.cz:SKC01-000910820\"")
//                        .setRows(1000);
//                SolrDocumentList catalogDocs = client.query(DataCollections.catalog.name(), catalogQuery).getResults();
//                Assert.assertTrue(catalogDocs.size() == 1);
//                Assert.assertTrue(catalogDocs.get(0).containsKey(MarcRecordFields.DNTSTAV_FIELD));
//                Assert.assertTrue(catalogDocs.get(0).getFirstValue(MarcRecordFields.DNTSTAV_FIELD).equals(PublicItemState.A.name()));
//                Assert.assertEquals("cnb001242191", catalogDocs.get(0).getFirstValue("id_ccnb"));
//
//
//                Object firstValue = catalogDocs.get(0).getFirstValue(MarcRecordFields.HISTORIE_STAVU_FIELD);
//                JSONArray inputArr = new JSONArray(firstValue.toString());
//                Assert.assertTrue(inputArr.length() == 1);
//
//                Assert.assertTrue(inputArr.getJSONObject(0).has("stav"));
//                Assert.assertTrue(inputArr.getJSONObject(0).getString("stav").equals(PublicItemState.A.name()));
//            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
