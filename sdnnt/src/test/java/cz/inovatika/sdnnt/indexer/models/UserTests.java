package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        user.role = Role.admin.name();
        user.username ="testusername";
        user.email = "test@test.eu";
        user.jmeno="TEST";
        user.prijmeni="Testovic";
        user.adresa="Testovaci adresa, 123";
        user.apikey="API-KEY-123456";
        user.notifikace_interval = NotificationInterval.mesic.name();

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

        Assert.assertTrue(readFromJSON.username.equals(user.username));
        Assert.assertTrue(readFromJSON.role.equals(user.role));
        Assert.assertTrue(readFromJSON.email.equals(user.email));
        Assert.assertTrue(readFromJSON.jmeno.equals(user.jmeno));
        Assert.assertTrue(readFromJSON.prijmeni.equals(user.prijmeni));
        Assert.assertTrue(readFromJSON.notifikace_interval.equals(user.notifikace_interval));

    }

    @Test
    public void testSolrBinder() {

        User user = new User();
        user.role = Role.admin.name();
        user.username = "testusername";
        user.email = "test@test.eu";
        user.jmeno="TEST";
        user.prijmeni="Testovic";
        user.adresa="Testovaci adresa, 123";
        user.apikey="API-KEY-123456";


        SolrDocument document = new SolrDocument();
        document.setField("role", "admin");
        document.setField("username", "testusername");
        document.setField("email", "test@test.eu");
        document.setField("jmeno", "TEST");
        document.setField("prijmeni", "Testovic");
        document.setField("adresa", "Testovaci adresa, 123");
        document.setField("apikey", "apikey");
        document.setField("notifikace_interval", "den");

        DocumentObjectBinder binder = new DocumentObjectBinder();
        SolrInputDocument solrInputFields = binder.toSolrInputDocument(user);

        User bean = binder.getBean(User.class, document);

    }

}
