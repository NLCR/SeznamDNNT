package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.services.AccountService;

public class SearchibilityValidationTest {
    
    private JSONObject foundOneResult() {
        JSONObject object = new JSONObject();
        JSONObject response = new JSONObject();
        response.put("numFound", 1);
        object.put("response", response);
        return object;
    }

    private JSONObject foundNoResult() {
        JSONObject object = new JSONObject();
        JSONObject response = new JSONObject();
        response.put("numFound", 0);
        object.put("response", response);
        return object;
    }

    @Test
    public void testSearchibility_1() {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        SearchibilityValidation val = EasyMock.createMockBuilder(SearchibilityValidation.class)
            .withConstructor(client).createMock();
        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);

        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai-record1");
        identifiers.add("oai-record2");

        EasyMock.expect(zadost.getIdentifiers()).andReturn(identifiers).anyTimes();
 
        Map<String, String> parametersRecord1 = new HashMap<>();
        parametersRecord1.put("fullCatalog", "true");
        parametersRecord1.put("catalog", "all");
        parametersRecord1.put("q", "oai-record1");
        EasyMock.expect(searcher.search(
                EasyMock.eq(parametersRecord1), 
                EasyMock.eq(new ArrayList<>()), 
                EasyMock.eq(user)))
            .andReturn(foundOneResult()).anyTimes();

        
        Map<String, String> parametersRecord2 = new HashMap<>();
        parametersRecord2.put("fullCatalog", "true");
        parametersRecord2.put("catalog", "all");
        parametersRecord2.put("q", "oai-record2");
        EasyMock.expect(searcher.search(
                EasyMock.eq(parametersRecord2), 
                EasyMock.eq(new ArrayList<>()), 
                EasyMock.eq(user)))
            .andReturn(foundNoResult()).anyTimes();

        EasyMock.replay(user, accounts, searcher, zadost);
        
        boolean validate = val.validate(user, accounts, zadost, identifiers, searcher);
        Assert.assertFalse(validate);
        Assert.assertEquals("The following records are not accessible: [oai-record2]!", val.getErrorMessage());
        Assert.assertTrue(val.isSoftValidation());
        
        Assert.assertEquals(val.getInvalidIdentifiers(), Arrays.asList("oai-record2"));
    }

    @Test
    public void testSearchibility_2() {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        SearchibilityValidation val = EasyMock.createMockBuilder(SearchibilityValidation.class)
            .withConstructor(client).createMock();
        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);

        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai-record1");
        identifiers.add("oai-record2");

        EasyMock.expect(zadost.getIdentifiers()).andReturn(identifiers).anyTimes();
 
        Map<String, String> parametersRecord1 = new HashMap<>();
        parametersRecord1.put("fullCatalog", "true");
        parametersRecord1.put("catalog", "all");
        parametersRecord1.put("q", "oai-record1");
        EasyMock.expect(searcher.search(
                EasyMock.eq(parametersRecord1), 
                EasyMock.eq(new ArrayList<>()), 
                EasyMock.eq(user)))
            .andReturn(foundOneResult()).anyTimes();

        
        Map<String, String> parametersRecord2 = new HashMap<>();
        parametersRecord2.put("fullCatalog", "true");
        parametersRecord2.put("catalog", "all");
        parametersRecord2.put("q", "oai-record2");
        EasyMock.expect(searcher.search(
                EasyMock.eq(parametersRecord2), 
                EasyMock.eq(new ArrayList<>()), 
                EasyMock.eq(user)))
            .andReturn(foundOneResult()).anyTimes();

        EasyMock.replay(user, accounts, searcher, zadost);
        
        boolean validate = val.validate(user, accounts, zadost, identifiers, searcher);
        Assert.assertTrue(validate);
        
        //Assert.assertEquals("The following records are not accessible: [oai-record2]!", val.getErrorMessage());
        //Assert.assertTrue(val.isRemoveableIdentifiersValidation());
    }

    @Test
    public void testSearchibility_3() {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        SearchibilityValidation val = EasyMock.createMockBuilder(SearchibilityValidation.class)
            .withConstructor(client).createMock();
        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);

        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai-record1");
        identifiers.add("oai-record2");

        EasyMock.expect(zadost.getIdentifiers()).andReturn(identifiers).anyTimes();
 
        Map<String, String> parametersRecord1 = new HashMap<>();
        parametersRecord1.put("fullCatalog", "true");
        parametersRecord1.put("catalog", "all");
        parametersRecord1.put("q", "oai-record1");
        EasyMock.expect(searcher.search(
                EasyMock.eq(parametersRecord1), 
                EasyMock.eq(new ArrayList<>()), 
                EasyMock.eq(user)))
            .andReturn(foundNoResult()).anyTimes();

        
        Map<String, String> parametersRecord2 = new HashMap<>();
        parametersRecord2.put("fullCatalog", "true");
        parametersRecord2.put("catalog", "all");
        parametersRecord2.put("q", "oai-record2");
        EasyMock.expect(searcher.search(
                EasyMock.eq(parametersRecord2), 
                EasyMock.eq(new ArrayList<>()), 
                EasyMock.eq(user)))
            .andReturn(foundNoResult()).anyTimes();

        EasyMock.replay(user, accounts, searcher, zadost);
        
        boolean validate = val.validate(user, accounts, zadost, identifiers, searcher);
        Assert.assertFalse(validate);
        Assert.assertEquals("The following records are not accessible: [oai-record1, oai-record2]!", val.getErrorMessage());

        Assert.assertEquals(val.getInvalidIdentifiers(), Arrays.asList("oai-record1","oai-record2"));

        List<Detail> errorDetails = val.getErrorDetails("NZN");
        for (Detail detail : errorDetails) {
            System.out.println(detail);
        }

        
        
        //Assert.assertTrue(val.isRemoveableIdentifiersValidation());
    }
}
