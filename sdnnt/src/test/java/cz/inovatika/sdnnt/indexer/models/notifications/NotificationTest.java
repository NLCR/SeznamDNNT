package cz.inovatika.sdnnt.indexer.models.notifications;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.indexer.models.notifications.RuleNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.SimpleNotification;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class NotificationTest {

    @Test
    public void deserializeAndSerializeNotification_SimpleNotification() throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("notification_knihovna_oai_aleph-nkp.cz_SKC01-000849392.json");
        Assert.assertNotNull(resourceAsStream);
        String s = IOUtils.toString(resourceAsStream, "UTF-8");
        SimpleNotification notification = SimpleNotification.fromJSON(s);

        Assert.assertTrue(notification.getId().equals("knihovna_oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(notification.getIdentifier().equals("oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(notification.getUser().equals("knihovna"));
        Assert.assertTrue(notification.getPeriodicity().equals("mesic"));
        //Assert.assertNotNull(notification.getIndextime());

        JSONObject serialized = notification.toJSONObject();
        Assert.assertTrue(serialized.getString("id").equals("knihovna_oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(serialized.getString("identifier").equals("oai:aleph-nkp.cz:SKC01-000849392"));
        Assert.assertTrue(serialized.getString("user").equals("knihovna"));
        Assert.assertTrue(serialized.getString("periodicity").equals("mesic"));

    }
    

    @Test
    public void deserializeAndSerializeNotification_RuleNotification() throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("notification_knihovna_rulebased.json");
        Assert.assertNotNull(resourceAsStream);
        String s = IOUtils.toString(resourceAsStream, "UTF-8");
        RuleNotification notification = RuleNotification.fromJSON(s);

        Assert.assertTrue(notification.getId().equals("identification"));
        Assert.assertTrue(notification.getName().equals("Moje pojmenovani"));
        Assert.assertTrue(notification.getQuery().equals("Query test"));
        Assert.assertTrue(notification.getPeriodicity().equals("mesic"));
        Assert.assertTrue(notification.getFilters() != null);
        Assert.assertFalse(notification.getFilters().isEmpty());
        Assert.assertTrue(notification.getFilters().containsKey("dntstav"));
        Assert.assertTrue(notification.getFilters().containsKey("license"));
        
        
        JSONObject notificationJSONObject = notification.toJSONObject();
        Assert.assertTrue(notificationJSONObject.has("id"));
        Assert.assertTrue(notificationJSONObject.has("name"));
        Assert.assertTrue(notificationJSONObject.has("query"));
        Assert.assertTrue(notificationJSONObject.has("periodicity"));
        Assert.assertTrue(notificationJSONObject.has("filters"));
    }
    

    @Test
    public void notificationSearchAndProcessFilter() throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("notification_knihovna_rulebased.json");
        String s = IOUtils.toString(resourceAsStream, "UTF-8");
        AbstractNotification an = NotificationFactory.fromJSON(s);
        RuleNotification rNotif = (RuleNotification) an;
        String provideSearchQueryFilters = rNotif.provideSearchQueryFilters();
        String expectedQueryFilters = "(title:\"Query test\" OR id_pid:\"Query test\" OR id_all_identifiers:\"Query test\" OR id_all_identifiers_cuts:\"Query test\" OR fullText:\"Query test\") AND dntstav:\"PA\" AND license:\"dnntt\"";
        Assert.assertTrue(expectedQueryFilters.equals(provideSearchQueryFilters));
        String provideProcessQueryFitlers = rNotif.provideProcessQueryFilters();
        expectedQueryFilters = "(title:\"Query test\" OR id_pid:\"Query test\" OR id_all_identifiers:\"Query test\" OR id_all_identifiers_cuts:\"Query test\" OR fullText:\"Query test\") AND historie_kurator_stavu_cut:PA AND NOT dntstav:PA AND historie_kurator_stavu_cut:dnntt";
        Assert.assertEquals(expectedQueryFilters, provideProcessQueryFitlers);

        
        
        resourceAsStream = this.getClass().getResourceAsStream("notification_knihovna_rule_rokvydani.json");
        s = IOUtils.toString(resourceAsStream, "UTF-8");
        an = NotificationFactory.fromJSON(s);
        rNotif = (RuleNotification) an;
        provideSearchQueryFilters = rNotif.provideSearchQueryFilters();
        Assert.assertEquals("(title:\"Query test\" OR id_pid:\"Query test\" OR id_all_identifiers:\"Query test\" OR id_all_identifiers_cuts:\"Query test\" OR fullText:\"Query test\") AND rokvydani:[2006 TO 2006] AND nakladatel:\"odeon\" AND license:\"dnntt\"", provideSearchQueryFilters);
        
    }

    @Test
    public void deserializeAndSerializeNotification_Factory() throws IOException {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("notification_knihovna_rulebased.json");
        Assert.assertNotNull(resourceAsStream);
        String s = IOUtils.toString(resourceAsStream, "UTF-8");
        AbstractNotification an = NotificationFactory.fromJSON(s);
        Assert.assertNotNull(an);
        Assert.assertTrue(an.getType().equals(AbstractNotification.TYPE.rule.name()));
    }


}
