package cz.inovatika.sdnnt.indexer.models;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class NotificationTest {

    @Test
    public void deserializeAndSerializeNotification() throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("notification_knihovna_oai_aleph-nkp.cz_SKC01-000849392.json");
        Assert.assertNotNull(resourceAsStream);
        String s = IOUtils.toString(resourceAsStream, "UTF-8");
        Notification notification = Notification.fromJSON(s);

        Assert.assertTrue(notification.getId().equals("knihovna_oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(notification.getIdentifier().equals("oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(notification.getUser().equals("knihovna"));
        Assert.assertTrue(notification.getPeriodicity().equals("mesic"));
        Assert.assertNotNull(notification.getIndextime());

        JSONObject serialized = notification.toJSONObject();
        Assert.assertTrue(serialized.getString("id").equals("knihovna_oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(serialized.getString("identifier").equals("oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(serialized.getString("user").equals("knihovna"));
        Assert.assertTrue(serialized.getString("periodicity").equals("mesic"));

        Assert.assertTrue(serialized.has("indextime"));
    }
}
