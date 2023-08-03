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
import cz.inovatika.sdnnt.services.AccountService;

public class MaximumSizeExceededDNNTRequestValidationTest {

    @Test
    public void testMaximumSizeExceed() {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        MaximumSizeExceededDNNTRequestValidation val = EasyMock.createMockBuilder(MaximumSizeExceededDNNTRequestValidation.class)
            .addMockedMethod("maximumIntInRequest")
            .withConstructor(client).createMock();
        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadostMax = EasyMock.createMock(Zadost.class);
        Zadost zadostOk = EasyMock.createMock(Zadost.class);
        
        EasyMock.expect(val.maximumIntInRequest()).andReturn(100).anyTimes();
        
        List<String> identifiers101 = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            identifiers101.add("oai:record_"+i);
        }

        List<String> identifiers100 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            identifiers100.add("oai:record_"+i);
        }

        EasyMock.expect(zadostMax.getIdentifiers()).andReturn(identifiers101);
        EasyMock.expect(zadostOk.getIdentifiers()).andReturn(identifiers100);
        
        EasyMock.replay(user,val,accounts, searcher, zadostMax);
        
        boolean validateFalse = val.validate(user, accounts, zadostMax, identifiers101, searcher);
        Assert.assertFalse(validateFalse);
        Assert.assertFalse(val.isSoftValidation());
        Assert.assertEquals("The maximum number of identifiers in the request has been exceeded.", val.getErrorMessage());
        
        boolean validateOk  = val.validate(user, accounts, zadostOk, identifiers100, searcher);
        Assert.assertTrue(validateOk);
    }
}
