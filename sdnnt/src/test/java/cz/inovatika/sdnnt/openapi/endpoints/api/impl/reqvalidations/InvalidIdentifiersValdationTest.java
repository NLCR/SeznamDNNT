package cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.reqvalidations.DNNTRequestApiServiceValidation.DividedIdentifiers;
import cz.inovatika.sdnnt.openapi.endpoints.model.Detail;
import cz.inovatika.sdnnt.services.AccountService;

import static cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateUtilsTest.*;

public class InvalidIdentifiersValdationTest {

    @Test
    public void testOk() throws Exception {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        InvalidIdentifiersValdation val = EasyMock.createMockBuilder(InvalidIdentifiersValdation.class)
                .addMockedMethod("markRecordFromSolr")
                .addMockedMethod("documentById")
                .withConstructor(client).createMock();

        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-002778029", (marcRecord) ->{
            marcRecord.dntstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
            marcRecord.kuratorstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
        });
        SolrDocument mock029 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock029.getFieldValue("place_of_pub")).andReturn("xr ").anyTimes();
        EasyMock.expect(mock029.getFieldValue("fmt")).andReturn("BK").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-002778029")).andReturn(mock029).anyTimes();

        
        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-000392836", (marcRecord) ->{
            marcRecord.dntstav = new ArrayList<>();
            marcRecord.kuratorstav = new ArrayList<>();
            
        });
        SolrDocument mock836 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock836.getFieldValue("place_of_pub")).andReturn("xr ").anyTimes();
        EasyMock.expect(mock836.getFieldValue("fmt")).andReturn("BK").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-000392836")).andReturn(mock836).anyTimes();

        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);
        
        EasyMock.expect(zadost.getNavrh()).andReturn("NZN").anyTimes();
        
        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai:aleph-nkp.cz:SKC01-002778029");
        identifiers.add("oai:aleph-nkp.cz:SKC01-000392836");

        EasyMock.replay(user,accounts,searcher, zadost,val, mock029, mock836);
        
        boolean validated = val.validate(user, accounts, zadost, identifiers, searcher);
        Assert.assertTrue(validated);
    }
    
    @Test
    public void testInvalidStateIdentifiers() throws Exception {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        InvalidIdentifiersValdation val = EasyMock.createMockBuilder(InvalidIdentifiersValdation.class)
                .addMockedMethod("markRecordFromSolr")
                .addMockedMethod("documentById")
                .withConstructor(client).createMock();

        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-002778029", (marcRecord) ->{
            marcRecord.dntstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
            marcRecord.kuratorstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
        });
        SolrDocument mock029 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock029.getFieldValue("place_of_pub")).andReturn("xr ").anyTimes();
        EasyMock.expect(mock029.getFieldValue("fmt")).andReturn("BK").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-002778029")).andReturn(mock029).anyTimes();

        
        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-000392836", (marcRecord) ->{});
        SolrDocument mock836 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock836.getFieldValue("place_of_pub")).andReturn("xb ").anyTimes();
        EasyMock.expect(mock836.getFieldValue("fmt")).andReturn("BK").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-000392836")).andReturn(mock836).anyTimes();

        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);
        
        EasyMock.expect(zadost.getNavrh()).andReturn("NZN").anyTimes();
        
        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai:aleph-nkp.cz:SKC01-002778029");
        identifiers.add("oai:aleph-nkp.cz:SKC01-000392836");

        EasyMock.replay(user,accounts,searcher, zadost,val, mock029, mock836);
        
        boolean validated = val.validate(user, accounts, zadost, identifiers, searcher);
        
        Assert.assertFalse(validated);
        Assert.assertEquals("The following identifiers [(oai:aleph-nkp.cz:SKC01-000392836,PA)] cannot be included in the DNNT list proposal.", val.getErrorMessage());
        List<String> invalidIdentifiers =  val.getInvalidIdentifiers();
        List<String> validIdentifiers =  val.getValidIdentifiers(identifiers);
        
        
        //DividedIdentifiers divs = val.getDividedIdentifiers(identifiers);
        
        Assert.assertEquals(Arrays.asList("oai:aleph-nkp.cz:SKC01-002778029"), validIdentifiers);
        Assert.assertEquals(Arrays.asList("oai:aleph-nkp.cz:SKC01-000392836"), invalidIdentifiers);

        List<Detail> errorDetails = val.getErrorDetails();
        for (Detail detail : errorDetails) {
            System.out.println(detail);
        }
        
    }

    @Test
    public void testPlaceOfPublicationIdentifiers() throws Exception {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        InvalidIdentifiersValdation val = EasyMock.createMockBuilder(InvalidIdentifiersValdation.class)
                .addMockedMethod("markRecordFromSolr")
                .addMockedMethod("documentById")
                .withConstructor(client).createMock();

        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-002778029", (marcRecord) ->{
            marcRecord.dntstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
            marcRecord.kuratorstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
        });
        SolrDocument mock029 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock029.getFieldValue("place_of_pub")).andReturn("xr ").anyTimes();
        EasyMock.expect(mock029.getFieldValue("fmt")).andReturn("BK").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-002778029")).andReturn(mock029).anyTimes();

        
        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-000392836", (marcRecord) ->{
            marcRecord.dntstav = new ArrayList<>();
            marcRecord.kuratorstav = new ArrayList<>();
            
        });
        SolrDocument mock836 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock836.getFieldValue("place_of_pub")).andReturn("xb ").anyTimes();
        EasyMock.expect(mock836.getFieldValue("fmt")).andReturn("BK").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-000392836")).andReturn(mock836).anyTimes();

        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);
        
        EasyMock.expect(zadost.getNavrh()).andReturn("NZN").anyTimes();
        
        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai:aleph-nkp.cz:SKC01-002778029");
        identifiers.add("oai:aleph-nkp.cz:SKC01-000392836");

        EasyMock.replay(user,accounts,searcher, zadost,val, mock029, mock836);
        
        boolean validated = val.validate(user, accounts, zadost, identifiers, searcher);
        
        Assert.assertFalse(validated);

        Assert.assertEquals("The following identifiers [oai:aleph-nkp.cz:SKC01-000392836] have an incorrect place of publication. Expecting Czech Republic (control field 008, position 15-17, expecting value 'xr').", val.getErrorMessage());
        List<String> invalidIdentifiers = val.getInvalidIdentifiers();
        List<String> validIdentifiers = val.getValidIdentifiers(identifiers);
        //DividedIdentifiers divs = val.getDividedIdentifiers(identifiers);
        
        Assert.assertEquals(Arrays.asList("oai:aleph-nkp.cz:SKC01-002778029"), validIdentifiers);
        Assert.assertEquals(Arrays.asList("oai:aleph-nkp.cz:SKC01-000392836"), invalidIdentifiers);


        List<Detail> errorDetails = val.getErrorDetails();
        for (Detail detail : errorDetails) {
            System.out.println(detail);
        }

    }


    @Test
    public void testFormatIdentifiers() throws Exception {
        SolrClient client = EasyMock.createMock(SolrClient.class);

        InvalidIdentifiersValdation val = EasyMock.createMockBuilder(InvalidIdentifiersValdation.class)
                .addMockedMethod("markRecordFromSolr")
                .addMockedMethod("documentById")
                .withConstructor(client).createMock();

        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-002778029", (marcRecord) ->{
            marcRecord.dntstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
            marcRecord.kuratorstav = new ArrayList<>(Arrays.asList(PublicItemState.N.name()));
        });
        SolrDocument mock029 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock029.getFieldValue("place_of_pub")).andReturn("xr ").anyTimes();
        EasyMock.expect(mock029.getFieldValue("fmt")).andReturn("BK").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-002778029")).andReturn(mock029).anyTimes();

        
        mockMarcRecord(val, "oai:aleph-nkp.cz:SKC01-000392836", (marcRecord) ->{
            marcRecord.dntstav = new ArrayList<>();
            marcRecord.kuratorstav = new ArrayList<>();
            
        });
        SolrDocument mock836 = EasyMock.createMock(SolrDocument.class);
        EasyMock.expect(mock836.getFieldValue("place_of_pub")).andReturn("xr ").anyTimes();
        EasyMock.expect(mock836.getFieldValue("fmt")).andReturn("MP").anyTimes();
        EasyMock.expect(val.documentById("oai:aleph-nkp.cz:SKC01-000392836")).andReturn(mock836).anyTimes();

        
        User user = EasyMock.createMock(User.class);
        AccountService accounts = EasyMock.createMock(AccountService.class);
        CatalogSearcher searcher = EasyMock.createMock(CatalogSearcher.class);
        Zadost zadost = EasyMock.createMock(Zadost.class);
        
        EasyMock.expect(zadost.getNavrh()).andReturn("NZN").anyTimes();
        
        List<String> identifiers = new ArrayList<>();
        identifiers.add("oai:aleph-nkp.cz:SKC01-002778029");
        identifiers.add("oai:aleph-nkp.cz:SKC01-000392836");

        EasyMock.replay(user,accounts,searcher, zadost,val, mock029, mock836);
        
        boolean validated = val.validate(user, accounts, zadost, identifiers, searcher);
        
        Assert.assertFalse(validated);

        System.out.println(val.getErrorMessage());
        Assert.assertEquals("The following identifiers [oai:aleph-nkp.cz:SKC01-000392836] have an incorrect format. Expecting a book or a serial (BK, SE).", val.getErrorMessage());
        List<String> invalidIdents = val.getInvalidIdentifiers();
       List<String> validIdentifiers = val.getValidIdentifiers(identifiers);
        
        Assert.assertEquals(Arrays.asList("oai:aleph-nkp.cz:SKC01-002778029"), validIdentifiers);
        Assert.assertEquals(Arrays.asList("oai:aleph-nkp.cz:SKC01-000392836"), invalidIdents);
        
        List<Detail> errorDetails = val.getErrorDetails();
        for (Detail detail : errorDetails) {
            System.out.println(detail);
        }

    }

    private void mockMarcRecord(InvalidIdentifiersValdation val, String id, Consumer<MarcRecord> consumer)
            throws JsonProcessingException, JsonMappingException, IOException, SolrServerException {
        MarcRecord marcRecord = MarcRecord.fromSolrDoc(prepareResultList(id.replaceAll("\\:","_")).get(0));
        
        consumer.accept(marcRecord);

        EasyMock.expect(val.markRecordFromSolr(id)).andReturn(marcRecord).anyTimes();
    }
}
