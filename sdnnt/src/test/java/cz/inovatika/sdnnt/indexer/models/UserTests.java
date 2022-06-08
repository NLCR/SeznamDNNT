package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.rights.Role;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class UserTests {

    @Test
    public void testSerializeAndDeserialize() throws JsonProcessingException {
        User user = new User();
        user.setRole(Role.admin.name());
        user.setUsername("testusername");
        user.setEmail( "test@test.eu");
        user.setJmeno("TEST");
        user.setPrijmeni("Testovic");
        user.setAdresa("Testovaci adresa, 123");
        user.setUlice("Ulice");
        user.setCislo("C1234");
        user.setApikey("API-KEY-123456");
        user.setNotifikaceInterval( NotificationInterval.mesic.name());


        JSONObject userJSON = user.toJSONObject();

        Assert.assertTrue(userJSON.getString("username")!=null);
        Assert.assertTrue(userJSON.getString("username").equals("testusername"));
        Assert.assertTrue(userJSON.getString("role").equals(Role.admin.name()));
        Assert.assertTrue(userJSON.getString("email").equals("test@test.eu"));
        Assert.assertTrue(userJSON.getString("jmeno").equals("TEST"));
        Assert.assertTrue(userJSON.getString("prijmeni").equals("Testovic"));
        Assert.assertTrue(userJSON.getString("adresa").equals("Testovaci adresa, 123"));
        Assert.assertTrue(userJSON.getString("apikey").equals("API-KEY-123456"));
        Assert.assertTrue(userJSON.getString("notifikace_interval").equals(NotificationInterval.mesic.name()));

        User readFromJSON = User.fromJSON(userJSON.toString());

        Assert.assertTrue(readFromJSON.getUsername().equals(user.getUsername()));
        Assert.assertTrue(readFromJSON.getRole().equals(user.getRole()));
        Assert.assertTrue(readFromJSON.getEmail().equals(user.getEmail()));
        Assert.assertTrue(readFromJSON.getJmeno().equals(user.getJmeno()));
        Assert.assertTrue(readFromJSON.getPrijmeni().equals(user.getPrijmeni()));
        Assert.assertTrue(readFromJSON.getNotifikaceInterval().equals(user.getNotifikaceInterval()));

    }

    @Test
    public void testSolrBinder() {

        User user = new User();
        user.setRole( Role.admin.name());
        user.setUsername( "testusername");
        user.setEmail( "test@test.eu");
        user.setJmeno("TEST");
        user.setPrijmeni("Testovic");
        user.setAdresa("Testovaci adresa, 123");
        user.setApikey("API-KEY-123456");


        SolrDocument document = new SolrDocument();
        document.setField("role", "admin");
        document.setField("username", "testusername");
        document.setField("email", "test@test.eu");
        document.setField("jmeno", "TEST");
        document.setField("prijmeni", "Testovic");
        document.setField("adresa", "Testovaci adresa, 123");
        document.setField("apikey", "apikey");
        document.setField("notifikace_interval", "den");

        SolrInputDocument solrInputFields = user.toSolrInputDocument();

        Assert.assertNotNull(solrInputFields.getFieldValue("role"));
        Assert.assertNotNull(solrInputFields.getFieldValue("username"));
        Assert.assertNotNull(solrInputFields.getFieldValue("email"));
        Assert.assertNotNull(solrInputFields.getFieldValue("jmeno"));
        Assert.assertNotNull(solrInputFields.getFieldValue("prijmeni"));
        Assert.assertNotNull(solrInputFields.getFieldValue("adresa"));
        Assert.assertNotNull(solrInputFields.getFieldValue("apikey"));
        Assert.assertNotNull(solrInputFields.getFieldValue("notifikace_interval"));
    }

}
