package cz.inovatika.sdnnt.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class VersionStringCastTest {

    @Test
    public void testCast() throws IOException {
        InputStream resourceAsStream = VersionStringCastTest.class.getResourceAsStream("result.json");
        String s = IOUtils.toString(resourceAsStream, "UTF-8");
        JSONObject object = new JSONObject(s);
        JSONObject cast = VersionStringCast.cast(object);

        JSONArray jsonArray = cast.getJSONObject("response").getJSONArray("docs");
        Assert.assertTrue(jsonArray.getJSONObject(0).has("version"));
        Assert.assertFalse(jsonArray.getJSONObject(0).has("_version_"));

        Assert.assertTrue(jsonArray.getJSONObject(1).has("version"));
        Assert.assertFalse(jsonArray.getJSONObject(1).has("_version_"));
    }
}
