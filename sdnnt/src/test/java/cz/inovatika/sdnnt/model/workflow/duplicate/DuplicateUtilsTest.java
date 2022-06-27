package cz.inovatika.sdnnt.model.workflow.duplicate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.ZadostProcess;
import cz.inovatika.sdnnt.utils.MarcModelTestsUtils;

public class DuplicateUtilsTest {

    @Test
    public void testFollowers() throws IOException, SolrServerException {
        MarcRecord marcRecord = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:DNT01-000102092".replaceAll("\\:","_")).get(0));

        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.dntstav);
        
        MarcRecord follower = MarcRecord.fromDocDep(prepareResultList("oai:aleph-nkp.cz:SKC01-000995692".replaceAll("\\:","_")).get(0));
        DuplicateUtils.moveProperties(marcRecord, Arrays.asList(follower), null);
        
        Assert.assertNotNull(follower.dntstav);
        Assert.assertTrue(follower.dntstav.size() > 0);
        
        Assert.assertNotNull(follower.kuratorstav);
        Assert.assertTrue(follower.kuratorstav.size() > 0);

        Assert.assertTrue(follower.historie_kurator_stavu != null);
        Assert.assertTrue(follower.historie_stavu != null);

        Assert.assertTrue(follower.historie_kurator_stavu.length() > 0);
        Assert.assertTrue(follower.historie_stavu.length() > 0);
    }
    

    @Test
    public void testZadost() throws JsonMappingException, JsonProcessingException, IOException {
        MarcRecord marcRecord = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:DNT01-000102092".replaceAll("\\:","_")).get(0));
        MarcRecord follower = MarcRecord.fromDocDep(prepareResultList("oai:aleph-nkp.cz:SKC01-000995692".replaceAll("\\:","_")).get(0));

        InputStream zadostStream = MarcModelTests.class.getResourceAsStream("zadost_dx_change.json");
        String string = IOUtils.toString(zadostStream, "UTF-8");
        Zadost zadost = Zadost.fromJSON(string);

        DuplicateUtils.changeRequests(marcRecord, Arrays.asList(follower), Arrays.asList(zadost));

        Assert.assertFalse(zadost.getIdentifiers().contains("oai:aleph-nkp.cz:DNT01-000102092"));
        Assert.assertTrue(zadost.getIdentifiers().contains("oai:aleph-nkp.cz:SKC01-000995692"));
        
        Map<String,ZadostProcess> process = zadost.getProcess();
        
        Optional<Boolean> noDNT = process.keySet().stream().map(key-> {
            return key.startsWith("oai:aleph-nkp.cz:DNT01-000102092");
        }).filter(v-> v).findAny();
        
        Assert.assertTrue(noDNT.isEmpty());
        
        Optional<Boolean> sKC = process.keySet().stream().map(key-> {
            return key.startsWith("oai:aleph-nkp.cz:SKC01-000995692");
        }).filter(v-> v).findAny();
        
        Assert.assertTrue(sKC.isPresent());
    }

    
    @Test
    public void testZadost_emptyIdentifiers_Processed() throws JsonMappingException, JsonProcessingException, IOException {
        MarcRecord marcRecord = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:DNT01-000102092".replaceAll("\\:","_")).get(0));
        InputStream zadostStream = MarcModelTests.class.getResourceAsStream("zadost_dx_change.json");
        String string = IOUtils.toString(zadostStream, "UTF-8");
        Zadost zadost = Zadost.fromJSON(string);
        Assert.assertTrue(zadost.getState() != null && zadost.getState().equals("waiting"));
        DuplicateUtils.changeRequests(marcRecord, new ArrayList<>(), Arrays.asList(zadost));
        Assert.assertTrue(zadost.getIdentifiers().isEmpty());
        Assert.assertTrue(zadost.getState().equals("processed"));
    }

    
    @Test
    public void test() throws JsonMappingException, JsonProcessingException, IOException {
        MarcRecord marcRecord = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:DNT01-000102092".replaceAll("\\:","_")).get(0));
        marcRecord.setLicense("dnnto");
        Assert.assertFalse(DuplicateSKCUtils.matchLicenseAndState(marcRecord, null, null));
        Assert.assertTrue(DuplicateSKCUtils.matchLicenseAndState(marcRecord, "A", "dnnto"));
        
    }
    
    public static SolrDocumentList prepareResultList(String ident) throws IOException {
        SolrDocumentList documentList = new SolrDocumentList();
        documentList.add(MarcModelTestsUtils.prepareResultDocument(ident));
        documentList.setNumFound(1);
        return documentList;
    }

    
    
}
