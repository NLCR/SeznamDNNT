package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import java.util.Collection;

@FunctionalInterface
public interface SolrDocumentOutput {

    void output(String selectedInstitution, String label, Collection<Object> nazev, String identifier, String... pids);

}
