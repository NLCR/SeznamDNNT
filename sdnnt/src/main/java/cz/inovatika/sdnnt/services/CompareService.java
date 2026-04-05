package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.services.compare.DifferencesResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface CompareService {

    public DifferencesResult check();
}
