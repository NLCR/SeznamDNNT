package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.util.ArrayList;
import java.util.Arrays;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.DNNTRequestApiServiceImpl.BadRequestEmptyIdentifiersException;
import cz.inovatika.sdnnt.services.AccountService;

public class EmptyRequestValidationTest {
    
    @Test
    public void testValidation()  {
        EmptyRequestValidation val = new EmptyRequestValidation(null);
        Zadost zadostEmpty = EasyMock.createMock(Zadost.class);
        Zadost zadostOk = EasyMock.createMock(Zadost.class);

        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        
        EasyMock.expect(zadostEmpty.getIdentifiers()).andReturn(new ArrayList<>());
        EasyMock.expect(zadostOk.getIdentifiers()).andReturn(new ArrayList<>(
                Arrays.asList("oai:record1")
        ));
        
        EasyMock.replay(zadostEmpty, zadostOk ,user,accounts, searcher);
        
        boolean notValidated = val.validate(user, accounts, zadostEmpty, new ArrayList<>(), searcher);
        Assert.assertFalse(notValidated);
        Assert.assertEquals("The request must contain at least one identifier!", val.getErrorMessage());
        
        boolean validated = val.validate(user, accounts, zadostOk, new ArrayList<>(Arrays.asList("oai:record1")), searcher);
        Assert.assertTrue(validated);
    }
}
