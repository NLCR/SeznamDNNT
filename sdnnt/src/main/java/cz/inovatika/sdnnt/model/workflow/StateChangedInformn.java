package cz.inovatika.sdnnt.model.workflow;

import org.apache.commons.lang3.tuple.Pair;

@FunctionalInterface
public interface StateChangedInformn {

    void changed(WorkflowOwner owner, String ident, Pair<String, String> oldStateLicensePair, Pair<String, String> newStateLicensePair);
}