package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.model.Zadost;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class AccountServiceImplMaximumItemsTest {

    @Test
    public void maximumIsInclusive() {
        Assert.assertFalse(AccountServiceImpl.maximumItemsExceeded(requestWithItems(300), 300));
        Assert.assertTrue(AccountServiceImpl.maximumItemsExceeded(requestWithItems(301), 300));
    }

    @Test
    public void negativeMaximumDisablesLimit() {
        Assert.assertFalse(AccountServiceImpl.maximumItemsExceeded(requestWithItems(301), -1));
    }

    private Zadost requestWithItems(int count) {
        Zadost zadost = new Zadost("test-request");
        List<String> identifiers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            identifiers.add("identifier-" + i);
        }
        zadost.setIdentifiers(identifiers);
        return zadost;
    }
}
