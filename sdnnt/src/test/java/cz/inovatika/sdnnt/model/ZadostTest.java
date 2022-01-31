package cz.inovatika.sdnnt.model;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class ZadostTest {

    @Test
    public void testDeserializeAndSerialize() throws IOException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("zadost_test_1.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        Zadost zadost = Zadost.fromJSON(jsonString);
        Assert.assertTrue(zadost.getIdentifiers() != null);
        Assert.assertTrue(zadost.getIdentifiers().size() == 1);
        Assert.assertTrue(zadost.getNavrh().equals("VVS"));
        Assert.assertTrue(zadost.getId().equals("pokusny1631526967544"));
        Assert.assertTrue(zadost.getState().equals("open"));
        Assert.assertTrue(zadost.getUser().equals("pokusny"));
        Assert.assertTrue(zadost.getTypeOfRequest().equals("user"));


        JSONObject object = zadost.toJSON();

        Assert.assertTrue(object.has("identifiers"));
        Assert.assertTrue(object.getJSONArray("identifiers").length() == 1);

        Assert.assertTrue(object.has("navrh"));
        Assert.assertTrue(object.getString("navrh").equals("VVS"));

        Assert.assertTrue(object.has("id"));
        Assert.assertTrue(object.getString("id").equals("pokusny1631526967544"));

        Assert.assertTrue(object.has("state"));
        Assert.assertTrue(object.getString("state").equals("open"));

        Assert.assertTrue(object.has("user"));
        Assert.assertTrue(object.getString("user").equals("pokusny"));

        Assert.assertTrue(object.has("type_of_request"));
    }


    @Test
    public void testDeserializeAndSerialize2() throws IOException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("zadost_test_2.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        Zadost zadost = Zadost.fromJSON(jsonString);
        Assert.assertTrue(zadost.getIdentifiers() != null);
        Assert.assertTrue(zadost.getIdentifiers().size() == 1);
        Assert.assertTrue(zadost.getNavrh().equals("VVS"));
        Assert.assertTrue(zadost.getId().equals("pokusny16315269675442"));
        Assert.assertTrue(zadost.getState().equals("open"));
        Assert.assertTrue(zadost.getUser().equals("pokusny"));
        Assert.assertTrue(zadost.getTypeOfRequest().equals("user"));

        JSONObject object = zadost.toJSON();

        Assert.assertTrue(object.has("identifiers"));
        Assert.assertTrue(object.getJSONArray("identifiers").length() == 1);

        Assert.assertTrue(object.has("navrh"));
        Assert.assertTrue(object.getString("navrh").equals("VVS"));

        Assert.assertTrue(object.has("id"));
        Assert.assertTrue(object.getString("id").equals("pokusny16315269675442"));

        Assert.assertTrue(object.has("state"));
        Assert.assertTrue(object.getString("state").equals("open"));

        Assert.assertTrue(object.has("user"));
        Assert.assertTrue(object.getString("user").equals("pokusny"));

        Assert.assertTrue(object.has("type_of_request"));
        Assert.assertTrue(object.getString("type_of_request").equals("user"));
    }

    @Test
    public void testDeserializeAndSerialize3() throws IOException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("zadost_test_3.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        Zadost zadost = Zadost.fromJSON(jsonString);
        Assert.assertTrue(zadost.getIdentifiers() != null);
        Assert.assertTrue(zadost.getIdentifiers().size() == 1);
        Assert.assertTrue(zadost.getNavrh().equals("NZN"));
        Assert.assertTrue(zadost.getId().equals("pokusny1631526967546"));
        Assert.assertTrue(zadost.getState().equals("open"));
        Assert.assertTrue(zadost.getUser().equals("pokusny"));

        JSONObject object = zadost.toJSON();

        Assert.assertTrue(object.has("identifiers"));
        Assert.assertTrue(object.getJSONArray("identifiers").length() == 1);

        Assert.assertTrue(object.has("navrh"));
        Assert.assertTrue(object.getString("navrh").equals("NZN"));

        Assert.assertTrue(object.has("id"));
        Assert.assertTrue(object.getString("id").equals("pokusny1631526967546"));

        Assert.assertTrue(object.has("state"));
        Assert.assertTrue(object.getString("state").equals("open"));

        Assert.assertTrue(object.has("user"));
        Assert.assertTrue(object.getString("user").equals("pokusny"));
    }

    @Test
    public void testSerializeAndDeserialize() throws IOException {
        Zadost zadost = new Zadost("id");
        zadost.setDeadline(new Date());
        zadost.setUser("pokusny");
        zadost.setIdentifiers(Arrays.asList("oai-test-1","oai-test-2"));
        zadost.setNavrh("NZN");
        zadost.setInstitution("NKP");
        zadost.setTypeOfPeriod(Period.period_nzn_1_12_18.name());
        zadost.setTransitionType(Period.period_nzn_1_12_18.getTransitionType().name());
        zadost.setDesiredItemState("NPA");
        zadost.setDesiredLicense("dnntt");

        ZadostProcess zp = new ZadostProcess();
        zp.setDate(new Date());
        zp.setState("state");
        zp.setUser("user");

        zadost.addProcess("iddone", zp);


        JSONObject jsonObject = zadost.toJSON();
        Zadost deseriazed = Zadost.fromJSON(jsonObject.toString());
        Assert.assertNotNull(deseriazed.getDeadline());
        Assert.assertNotNull(deseriazed.getUser());
        Assert.assertEquals(deseriazed.getUser(), "pokusny");
        Assert.assertEquals(deseriazed.getNavrh(), "NZN");
        Assert.assertEquals(deseriazed.getInstitution(), "NKP");
        Assert.assertEquals(deseriazed.getTypeOfPeriod(), "period_nzn_1_12_18");
        Assert.assertEquals(deseriazed.getTransitionType(), Period.period_nzn_1_12_18.getTransitionType().name());

        Assert.assertTrue(deseriazed.getProcess().size() == 1);
        Assert.assertTrue(deseriazed.getProcess().containsKey("iddone"));
        Assert.assertTrue(deseriazed.getProcess().get("iddone").getUser().equals("user"));
        Assert.assertTrue(deseriazed.getProcess().get("iddone").getState().equals("state"));
        Assert.assertTrue(deseriazed.getProcess().get("iddone").getDate() != null );

        Assert.assertEquals(deseriazed.getDesiredItemState(), "NPA");
        Assert.assertEquals(deseriazed.getDesiredLicense(), "dnntt");

        SolrInputDocument solrInputFields = zadost.toSolrInputDocument();
        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.DEADLINE_KEY));
        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.USER_KEY));
        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.NAVRH_KEY));
        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.INSTITUTION_KEY));
        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.TYPE_OF_PERIOD_KEY));
        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.TRANSITION_TYPE_KEY));

        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.DESIRED_ITEM_STATE_KEY));
        Assert.assertNotNull(solrInputFields.getFieldValue(Zadost.DESIRED_LICENSE_KEY));
        Object fieldValue = solrInputFields.getFieldValue(Zadost.PROCESS_KEY);
        Assert.assertNotNull(fieldValue);
    }

    @Ignore
    @Test
    public void testDeserializeAndSerializeBigSet() throws IOException, SolrServerException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("zadost_index.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        JSONObject result = new JSONObject(jsonString);
        JSONArray docsArray = result.getJSONObject("response").getJSONArray("docs");

        for (int i = 0; i < docsArray.length(); i++) {
            JSONObject doc = docsArray.getJSONObject(i);
            Zadost zadost = Zadost.fromJSON(doc.toString());

            Assert.assertNotNull(zadost.getId());
            Assert.assertNotNull(zadost.getUser());
            Assert.assertNotNull(zadost.getState());
            Assert.assertNotNull(zadost.getVersion());
            Assert.assertTrue(Arrays.asList("open", "processed", "waiting").contains(zadost.getState()));
            Assert.assertTrue(zadost.getVersion() !=null );

            // not storing properties
            if (doc.has("indextime")) {
                doc.remove("indextime");
            }

            if (doc.has("_version_")) {
                long version_ = doc.getLong("_version_");
                doc.remove("_version_");
                doc.put("version", ""+version_);
            }

            JSONObject serialized = zadost.toJSON();
            boolean equalsJSON = doc.keySet().equals(serialized.keySet());
            if (!equalsJSON) {
                System.out.println(doc.keySet());
                System.out.println(serialized.keySet());
                System.out.println(String.format("Invalid item %d", i));
            }

            Assert.assertTrue(equalsJSON);

            // testing solr document
            SolrInputDocument solrDoc = zadost.toSolrInputDocument();
            Collection<String> fieldNames = solrDoc.getFieldNames();

            if (doc.has("_version_")) {
                doc.remove("_version_");
            }
            if (doc.has("version")) {
                doc.remove("version");
            }

            boolean equalsSOLR = doc.keySet().equals(new HashSet<>(fieldNames));
            if (!equalsSOLR) {
                System.out.println(doc.keySet());
                System.out.println(new HashSet<>(fieldNames));
                System.out.println(String.format("Invalid item %d", i));

            }
            Assert.assertTrue(doc.keySet().equals(new HashSet<>(fieldNames)));
        }
    }



    @Test
    public void testDeserializeAndSerializeProcessFromBigSet() throws IOException, SolrServerException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("zadost_index.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        JSONObject result = new JSONObject(jsonString);
        JSONArray docsArray = result.getJSONObject("response").getJSONArray("docs");
        Zadost zadost = Zadost.fromJSON(docsArray.getJSONObject(0).toString());

        Map<String, ZadostProcess> process = zadost.getProcess();
        Assert.assertTrue(process.keySet().size() == 4);
        Assert.assertTrue(process.containsKey("oai:aleph-nkp.cz:SKC01-005828288"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828288").getReason().equals("asd"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828288").getState().equals("rejected"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828288").getUser().equals("kurator"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828288").getDate().equals(new Date(1626784653614L)));

        Assert.assertTrue(process.containsKey("oai:aleph-nkp.cz:SKC01-006167087"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-006167087").getReason().equals("                              "));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-006167087").getState().equals("rejected"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-006167087").getUser().equals("kurator"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-006167087").getDate().equals(new Date(1626784657649L)));

        Assert.assertTrue(process.containsKey("oai:aleph-nkp.cz:SKC01-005828270"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828270").getReason().equals("asdasd"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828270").getState().equals("rejected"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828270").getUser().equals("kurator"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005828270").getDate().equals(new Date(1626784690778L)));

        Assert.assertTrue(process.containsKey("oai:aleph-nkp.cz:SKC01-005516208"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005516208").getReason().equals("odstraněno"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005516208").getState().equals("rejected"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005516208").getUser().equals("kurator"));
        Assert.assertTrue(process.get("oai:aleph-nkp.cz:SKC01-005516208").getDate().equals(new Date(1626784478698L)));

    }
}
