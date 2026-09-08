package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.model.Zadost;

/**
 * Functional interface dedicated for information about the save
 */
@FunctionalInterface
public interface AccountServiceInform {

    void saved(Zadost zadost);
}
