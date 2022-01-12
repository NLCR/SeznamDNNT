package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfListitem;
import cz.inovatika.sdnnt.openapi.endpoints.model.Listitem;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ModelDocumentOutput  implements  SolrDocumentOutput{

    private ArrayOfListitem arrayOfListitem;

    public ModelDocumentOutput(ArrayOfListitem arrayOfListitem) {
        this.arrayOfListitem = arrayOfListitem;
    }

//    @Override
//    public void output(String selectedInstitution, String label, Collection<Object> nazev, String identifier, String... pids) {
//        for (int i = 0; i < pids.length; i++) {
//            Listitem item = new Listitem()
//                    .pid(pids[i])
//                    .catalogIdentifier(identifier)
//                    .sigla(selectedInstitution)
//                    .title(nazev.toString())
//                    .license(label);
//
//            this.arrayOfListitem.add(item);
//        }
//
//    }


    @Override
    public void output(Map<String, Object> outputDocument, List<String> ordering) {
        Collection pids = (Collection) outputDocument.get(PIDS_KEY);
        pids.forEach(pid-> {
            String identifier = outputDocument.get(IDENTIFIER_KEY) != null ? outputDocument.get(IDENTIFIER_KEY).toString() : null ;
            String selInstitution = outputDocument.get(SELECTED_INSTITUTION_KEY) != null ? outputDocument.get(SELECTED_INSTITUTION_KEY).toString() : null ;
            String nazev = outputDocument.get(NAZEV_KEY) != null ? outputDocument.get(NAZEV_KEY).toString() : null ;
            //String nazev =  nazvy.stream().map(Object::toString).collect(Collectors.joining(" ")).toString();
            String label = outputDocument.get(LABEL_KEY) != null ? outputDocument.get(LABEL_KEY).toString() : null ;

            Listitem item = new Listitem()
                    .pid(pid.toString())
                    .catalogIdentifier(identifier)
                    .sigla(selInstitution)
                    .title(nazev)
                    .license(label);

            this.arrayOfListitem.add(item);
        });
    }
}
