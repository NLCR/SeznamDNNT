package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.Zadost;

import java.util.List;

/**
 * Functional interface dedicated for information about the save
 */
@FunctionalInterface
public interface AccountServiceInform {

    void saved(Zadost zadost);
}
