package cz.inovatika.sdnnt.services.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.indexer.models.MarcModelTests;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;

import static cz.inovatika.sdnnt.indexer.models.MarcModelTests.*;

public class ChangeProcessStatesUtilityTests {

    
    @Test
    public void testChangeStateNPA() throws IOException {
        MarcRecord marcRecord = MarcRecord.fromDocDep(prepareResultList("oai:aleph-nkp.cz:DNT01-000157317".replaceAll("\\:","_")).get(0));
        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.dntstav);
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("PA"));
        Assert.assertTrue(marcRecord.historie_stavu.length() == 3);
        Assert.assertTrue(marcRecord.historie_stavu.getJSONObject(2).getString("stav").equals("N"));
        Assert.assertTrue(marcRecord.historie_kurator_stavu.length() == 3);
        Assert.assertTrue(marcRecord.historie_kurator_stavu.getJSONObject(2).getString("stav").equals("N"));
        
        ChangeProcessStatesUtility.changeProcessState("NPA", marcRecord,"Zmena stavu");
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("N"));
        Assert.assertTrue(marcRecord.historie_stavu.length() == 4);
        Assert.assertTrue(marcRecord.historie_stavu.getJSONObject(3).getString("stav").equals("N"));
        Assert.assertTrue(marcRecord.historie_kurator_stavu.length() == 4);
        Assert.assertTrue(marcRecord.historie_kurator_stavu.getJSONObject(3).getString("stav").equals("NPA"));
        
    }
    @Test
    public void testChangeStateD() throws IOException {
        MarcRecord marcRecord = MarcRecord.fromDocDep(prepareResultList("oai:aleph-nkp.cz:DNT01-000157317".replaceAll("\\:","_")).get(0));
        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.dntstav);
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("PA"));

        ChangeProcessStatesUtility.changeProcessState("D", marcRecord, "Zmena stavu");
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("D"));
        Assert.assertTrue(marcRecord.kuratorstav.size() == 1);
        Assert.assertTrue(marcRecord.kuratorstav.get(0).equals("D"));
        
        Assert.assertTrue(marcRecord.historie_stavu.length() == 4);
        Assert.assertTrue(marcRecord.historie_stavu.getJSONObject(3).getString("stav").equals("D"));
        Assert.assertTrue(marcRecord.historie_kurator_stavu.length() == 4);
        Assert.assertTrue(marcRecord.historie_kurator_stavu.getJSONObject(3).getString("stav").equals("D"));
    }

    @Test
    public void testChangeStateDX() throws JsonProcessingException, IOException {
        MarcRecord marcRecord = MarcRecord.fromDocDep(prepareResultList("oai:aleph-nkp.cz:DNT01-000157317".replaceAll("\\:","_")).get(0));
        Assert.assertNotNull(marcRecord.datum_stavu);
        Assert.assertNotNull(marcRecord.dntstav);
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("PA"));

        ChangeProcessStatesUtility.changeProcessState("DX", marcRecord, "Zmena stavu");
        Assert.assertTrue(marcRecord.dntstav.size() == 1);
        Assert.assertTrue(marcRecord.dntstav.get(0).equals("PA"));
        Assert.assertTrue(marcRecord.kuratorstav.size() == 1);
        Assert.assertTrue(marcRecord.kuratorstav.get(0).equals("DX"));
        
        Assert.assertTrue(marcRecord.historie_stavu.length() == 4);
        Assert.assertTrue(marcRecord.historie_stavu.getJSONObject(3).getString("stav").equals("PA"));
        Assert.assertTrue(marcRecord.historie_kurator_stavu.length() == 4);
        Assert.assertTrue(marcRecord.historie_kurator_stavu.getJSONObject(3).getString("stav").equals("DX"));
    }
}
