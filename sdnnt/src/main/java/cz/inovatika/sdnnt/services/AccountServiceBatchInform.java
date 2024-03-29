package cz.inovatika.sdnnt.services;

import java.util.List;
import java.util.Map;

import cz.inovatika.sdnnt.services.exceptions.AccountException;

/**
 * Functional inerface represents information about failing batch
 * @author happy
 *
 */
@FunctionalInterface
public interface AccountServiceBatchInform {
    
    public void failedItems(Map<String, AccountException>  exceptions);
}
