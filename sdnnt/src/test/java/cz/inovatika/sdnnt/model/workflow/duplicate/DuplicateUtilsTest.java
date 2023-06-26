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
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.ZadostProcess;
import cz.inovatika.sdnnt.utils.MarcModelTestsUtils;

public class DuplicateUtilsTest {

    
    
    //** merge */
    @Test 
    public void testFollowersIOP() throws IOException, SolrServerException {
        MarcRecord origin = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-000392643".replaceAll("\\:","_")).get(0));
        MarcRecord follower = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-002778029".replaceAll("\\:","_")).get(0));
        
        DuplicateUtils.moveProperties(origin, Arrays.asList(follower), null);
        
        Assert.assertTrue(origin.idEuipo != null);
        Assert.assertTrue(origin.idEuipo.size() == 1);
        Assert.assertTrue(origin.idEuipo.get(0).equals("euipo:0a715715-30eb-474a-a02b-3b6a0f6bd840"));
        
        Assert.assertTrue(origin.idEuipoExports != null);
        Assert.assertTrue(origin.idEuipoExports.size() == 1);
        Assert.assertTrue(origin.idEuipoExports.get(0).equals("inital-bk"));
        
        Assert.assertTrue(origin.exportsFacets != null);
        Assert.assertTrue(origin.exportsFacets.size() == 1);
        Assert.assertTrue(origin.exportsFacets.get(0).equals("euipo"));

        //===============================

        Assert.assertTrue(follower.idEuipo != null);
        Assert.assertTrue(follower.idEuipo.size() == 2);
        Assert.assertTrue(follower.idEuipo.contains("euipo:0a715715-30eb-474a-a02b-3b6a0f6bd840"));
        Assert.assertTrue(follower.idEuipo.contains("euipo:9382b2ea-2d48-4fa3-8580-d3b03c5828fb"));
        
        Assert.assertTrue(follower.idEuipoExports != null);
        Assert.assertTrue(follower.idEuipoExports.size() == 2);
        Assert.assertTrue(follower.idEuipoExports.contains("inital-se"));
        Assert.assertTrue(follower.idEuipoExports.contains("inital-bk"));

        Assert.assertTrue(follower.exportsFacets != null);
        Assert.assertTrue(follower.exportsFacets.size() == 1);
        Assert.assertTrue(follower.exportsFacets.get(0).equals("euipo"));
        
    }

    /** merge */
    @Test
    public void testFollowersIOP2() throws IOException, SolrServerException {
        MarcRecord origin = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-000813372".replaceAll("\\:","_")).get(0));
        MarcRecord follower = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-002778029".replaceAll("\\:","_")).get(0));
        
        
        DuplicateUtils.moveProperties(origin, Arrays.asList(follower), null);

        Assert.assertTrue(follower.idEuipo.size() == 2);
        Assert.assertTrue(follower.idEuipo.contains("euipo:9382b2ea-2d48-4fa3-8580-d3b03c5828fb"));
        Assert.assertTrue(follower.idEuipo.contains("euipo:9382b2ea-2d48-4fa3-8580-changed"));

        Assert.assertTrue(follower.idEuipoExports.size() == 2);
        Assert.assertTrue(follower.idEuipoExports.contains("inital-ch"));
        Assert.assertTrue(follower.idEuipoExports.contains("inital-se"));
        
    }


    @Test
    public void testFollowersIOP3() throws IOException, SolrServerException {
        // NPA stav
        MarcRecord origin = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-000813372".replaceAll("\\:","_")).get(0));

        // Naslednik je N ale bude NPA
        MarcRecord follower = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-002778029".replaceAll("\\:","_")).get(0));
        follower.dntstav = Arrays.asList(PublicItemState.N.name());
        
        DuplicateUtils.moveProperties(origin, Arrays.asList(follower), null);
        
        Assert.assertTrue(follower.idEuipo.size() == 1);
        Assert.assertTrue(follower.idEuipo.contains("euipo:9382b2ea-2d48-4fa3-8580-changed"));
        Assert.assertFalse(follower.idEuipo.contains("euipo:9382b2ea-2d48-4fa3-8580-d3b03c5828fb"));

        Assert.assertTrue(follower.idEuipoExports.size() == 1);
        Assert.assertTrue(follower.idEuipoExports.contains("inital-ch"));
        Assert.assertFalse(follower.idEuipoExports.contains("inital-se"));
        
    }


    @Test
    public void testFollowersIOP4() throws IOException, SolrServerException {
        // NPA 
        MarcRecord origin = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-000813372".replaceAll("\\:","_")).get(0));
        
        // mimo seznam 
        MarcRecord follower = MarcRecord.fromSolrDoc(prepareResultList("oai:aleph-nkp.cz:SKC01-002778029".replaceAll("\\:","_")).get(0));
        follower.dntstav = null;
        follower.kuratorstav = null;
        follower.historie_kurator_stavu=new JSONArray();
        
        DuplicateUtils.moveProperties(origin, Arrays.asList(follower), null);
        
        Assert.assertTrue(follower.idEuipo.size() == 1);
        Assert.assertTrue(follower.idEuipo.contains("euipo:9382b2ea-2d48-4fa3-8580-changed"));
        Assert.assertFalse(follower.idEuipo.contains("euipo:9382b2ea-2d48-4fa3-8580-d3b03c5828fb"));

        Assert.assertTrue(follower.idEuipoExports.size() == 1);
        Assert.assertTrue(follower.idEuipoExports.contains("inital-ch"));
        Assert.assertFalse(follower.idEuipoExports.contains("inital-se"));
        
    }

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
