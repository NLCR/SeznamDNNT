package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.services.AccountService;

public class UserUsedIdentifierValidationTest {

    public List<String> allUsedIdentifiers() {
        List<String> used = new ArrayList<>();
        for (int i = 3; i < 20; i++) {
            used.add(String.format("oai-record%d", i));
        }
        return used;
    }
    
    @Test
    public void testUserUsedTest_1() throws Exception {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        UserUsedIdentifierValidation val = EasyMock.createMockBuilder(UserUsedIdentifierValidation.class)
                .addMockedMethod("format910ax")
                .withConstructor(client).createMock();
        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);

        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai-record1");
        identifiers.add("oai-record2");
        identifiers.add("oai-record3");
        
        EasyMock.expect(user.getUsername()).andReturn("testuser").anyTimes();
        EasyMock.expect(zadost.getIdentifiers()).andReturn(identifiers).anyTimes();
        
        EasyMock.expect(val.format910ax("oai-record3")).andReturn(Arrays.asList("910 |a xxxx |x zzzz")).anyTimes();
        
        EasyMock.expect(
                accounts.findIdentifiersUsedInRequests(EasyMock.eq("testuser"), 
                EasyMock.eq(Arrays.asList("open","waiting","waiting_for_automatic_process"))))
        .andReturn(allUsedIdentifiers())
        .anyTimes();
        
        EasyMock.replay(user,accounts,searcher, zadost, val);
        
        
        boolean validate = val.validate(user, accounts, zadost, identifiers, searcher);
        
        Assert.assertFalse(validate);
        Assert.assertEquals("The identifiers [oai-record3] are already used in other requests!", val.getErrorMessage());
        Assert.assertTrue(val.isSoftValidation());
        


        Assert.assertEquals(val.getInvalidIdentifiers(),Arrays.asList(
                "oai-record3"
        ));
        
        
        Assert.assertEquals(val.getValidIdentifiers(identifiers), 
                Arrays.asList(
                        "oai-record1",
                        "oai-record2"));
    }

    @Test
    public void testUserUsedTest_2() throws Exception {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        UserUsedIdentifierValidation val = EasyMock.createMockBuilder(UserUsedIdentifierValidation.class)
            .addMockedMethod("format910ax")
            .withConstructor(client).createMock();
        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);

        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai-record0");
        identifiers.add("oai-record1");
        identifiers.add("oai-record2");
        identifiers.add("oai-record3");
        identifiers.add("oai-record4");
        identifiers.add("oai-record5");
        identifiers.add("oai-record6");
        
        EasyMock.expect(user.getUsername()).andReturn("testuser").anyTimes();
        EasyMock.expect(zadost.getIdentifiers()).andReturn(identifiers).anyTimes();
        
        EasyMock.expect(val.format910ax("oai-record3")).andReturn(Arrays.asList("910 |a xxxx |x zzzz")).anyTimes();
        EasyMock.expect(val.format910ax("oai-record4")).andReturn(Arrays.asList("910 |a xxxx |x zzzz")).anyTimes();
        EasyMock.expect(val.format910ax("oai-record5")).andReturn(Arrays.asList("910 |a xxxx |x zzzz")).anyTimes();
        EasyMock.expect(val.format910ax("oai-record6")).andReturn(Arrays.asList("910 |a xxxx |x zzzz")).anyTimes();

        EasyMock.expect(
                accounts.findIdentifiersUsedInRequests(EasyMock.eq("testuser"), 
                EasyMock.eq(Arrays.asList("open","waiting","waiting_for_automatic_process"))))
        .andReturn(allUsedIdentifiers())
        .anyTimes();
        
        EasyMock.replay(user,accounts,searcher, zadost,val);
        
        
        boolean validate = val.validate(user, accounts, zadost, identifiers, searcher);
        
        Assert.assertFalse(validate);
        Assert.assertEquals("The identifiers [oai-record3, oai-record4, oai-record5, oai-record6] are already used in other requests!", val.getErrorMessage());
        Assert.assertTrue(val.isSoftValidation());

        
        
        Assert.assertEquals(val.getInvalidIdentifiers(),Arrays.asList(
                "oai-record3",
                "oai-record4",
                "oai-record5",
                "oai-record6"));
        
        
        Assert.assertEquals(val.getValidIdentifiers(identifiers), 
                Arrays.asList(
                        "oai-record0",
                        "oai-record1",
                        "oai-record2"));

        
        List<Detail> errorDetails = val.getErrorDetails();
        for (Detail detail : errorDetails) {
            System.out.println(detail);
        }

        //Assert.assertEquals(val.getDividedIdentifiers(identifiers), result);
    }
}
