package cz.inovatika.sdnnt.openapi.endpoints.api.impl.lists;

import cz.inovatika.sdnnt.openapi.endpoints.model.ArrayOfListitem;
import cz.inovatika.sdnnt.openapi.endpoints.model.Listitem;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ModelDocumentOutputTest {

    @Test
    public void catalogRecordWithoutDigitalCopyStillHasStateAndLicense() {
        ArrayOfListitem items = new ArrayOfListitem();
        ModelDocumentOutput output = new ModelDocumentOutput(items, Collections.emptyMap());
        Map<String, Object> record = new HashMap<>();
        record.put(SolrDocumentOutput.PIDS_KEY, new ArrayList<String>());
        record.put(SolrDocumentOutput.MASTERLINKS_KEY, new ArrayList<String>());
        record.put(SolrDocumentOutput.IDENTIFIER_KEY, "oai:aleph-nkp.cz:SKC01-002171136");
        record.put(SolrDocumentOutput.DNTSTAV_KEY, "A");
        record.put(SolrDocumentOutput.LABEL_KEY, "dnnto");

        output.output(null, record, Collections.emptyList(), null, false);

        assertEquals(1, items.size());
        Listitem item = items.get(0);
        assertEquals("oai:aleph-nkp.cz:SKC01-002171136", item.getCatalogIdentifier());
        assertEquals("A", item.getState());
        assertEquals("dnnto", item.getLicense());
        assertNull(item.getPid());
    }
}
