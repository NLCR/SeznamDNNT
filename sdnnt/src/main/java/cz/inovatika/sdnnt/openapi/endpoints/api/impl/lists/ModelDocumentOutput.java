package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfListitem;
import cz.inovatika.sdnnt.openapi.endpoints.model.Listitem;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;

public class ModelDocumentOutput  implements  SolrDocumentOutput{

    private ArrayOfListitem arrayOfListitem;

    public ModelDocumentOutput(ArrayOfListitem arrayOfListitem) {
        this.arrayOfListitem = arrayOfListitem;
    }

    @Override
    public void output(String selectedInstitution, String label, Collection<Object> nazev, String identifier, String... pids) {
        for (int i = 0; i < pids.length; i++) {
            Listitem item = new Listitem()
                    .pid(pids[i])
                    .catalogIdentifier(identifier)
                    .sigla(selectedInstitution)
                    .title(nazev.toString())
                    .license(label);

            this.arrayOfListitem.add(item);
        }

    }
}
