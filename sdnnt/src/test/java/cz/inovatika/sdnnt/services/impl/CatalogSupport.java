package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.it.SolrTestServer;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class CatalogSupport {

    public static final List<String> A_IDENTIFIERS = Arrays.asList("oai:aleph-nkp.cz:DNT01-000008874", "oai:aleph-nkp.cz:DNT01-000008884", "oai:aleph-nkp.cz:DNT01-000008886");
    public static final List<String> N_IDENTIFIERS = Arrays.asList("oai:aleph-nkp.cz:DNT01-000157742", "oai:aleph-nkp.cz:DNT01-000157765");

    private CatalogSupport() {}

    public static void insertCatalog(String states, String id) {
        try {
            String path = String.format("cz/inovatika/sdnnt/services/catalog/%s/%s.json", states,id.replaceAll(":","_"));
            InputStream stream1 = AccountServiceWorkflowImplITTest.class.getClassLoader().getResourceAsStream(path);
            Assert.assertNotNull(stream1);
            String json1 = IOUtils.toString(stream1, "UTF-8");
            MarcRecord marcRecord1 = MarcRecord.fromRAWJSON(new JSONObject(new JSONObject(json1).getString("raw")).toString());
            try (SolrClient client = SolrTestServer.getClient()){
                client.add("catalog", marcRecord1.toSolrDoc());
                client.commit("catalog");

            }
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }


    public static void inserNIdentifiers() {
        N_IDENTIFIERS.stream().forEach(it-> {
            insertCatalog("n", it);
        });
    }
    public static void inserAIdentifiers() {
        A_IDENTIFIERS.stream().forEach(it-> {
            insertCatalog("a", it);
        });
    }
}
