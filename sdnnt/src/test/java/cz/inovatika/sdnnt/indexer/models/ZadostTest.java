package cz.inovatika.sdnnt.indexer.models;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class ZadostTest {

    @Test
    public void testDeserializeAndSerialize() throws IOException {
        InputStream resourceAsStream = MarcModelTests.class.getResourceAsStream("zadost_pokusny1631526967544.json");
        String jsonString = IOUtils.toString(resourceAsStream, "UTF-8");
        Zadost zadost = Zadost.fromJSON(jsonString);
        Assert.assertTrue(zadost.identifiers != null);
        Assert.assertTrue(zadost.identifiers.size() == 1);
        Assert.assertTrue(zadost.navrh.equals("VVS"));
        Assert.assertTrue(zadost.id.equals("pokusny1631526967544"));
        Assert.assertTrue(zadost.state.equals("open"));
        Assert.assertTrue(zadost.user.equals("pokusny"));

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
    }
}
