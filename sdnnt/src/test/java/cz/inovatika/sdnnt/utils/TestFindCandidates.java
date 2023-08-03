package cz.inovatika.sdnnt.utils;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.index.utils.torefactor.MarcRecordUtilsToRefactor;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateSKCUtils;

public class TestFindCandidates {
    // neni updatovane zrusene ccnb 
    //find(client, "oai:aleph-nkp.cz:SKC01-000641203");
    //find(client, "oai:aleph-nkp.cz:SKC01-002367305");
    //find(client, "oai:aleph-nkp.cz:SKC01-000641203");
    //find(client, "oai:aleph-nkp.cz:SKC01-006805750");
    
    //find(client, "oai:aleph-nkp.cz:SKC01-008296864");
    
//    find(client, "oai:aleph-nkp.cz:SKC01-008296864");
//    find(client, "oai:aleph-nkp.cz:SKC01-008768208");
//    find(client, "oai:aleph-nkp.cz:SKC01-003251263");
//    find(client, "oai:aleph-nkp.cz:SKC01-000858471");
//    find(client, "oai:aleph-nkp.cz:SKC01-001090636");
//    find(client, "oai:aleph-nkp.cz:SKC01-000535621");
//    find(client, "oai:aleph-nkp.cz:SKC01-001083536");
//    find(client, "oai:aleph-nkp.cz:SKC01-000917938");
//    find(client, "oai:aleph-nkp.cz:SKC01-001663142");
//    find(client, "oai:aleph-nkp.cz:SKC01-001090638");
//    find(client, "oai:aleph-nkp.cz:SKC01-000840725");
//    find(client, "oai:aleph-nkp.cz:SKC01-001213477");
//    find(client, "oai:aleph-nkp.cz:SKC01-000641203");
    


    //oai:aleph-nkp.cz:SKC01-000917938
   
    public static final Logger LOGGER = Logger.getLogger(TestFindCandidates.class.getName());
    
    public static void main(String[] args) throws JsonProcessingException, SolrServerException, IOException {
        SolrClient client = Indexer.getClient();
        
        SolrDocument byId = client.getById( DataCollections.catalog.name(), "oai:aleph-nkp.cz:SKC01-001249395");
        Object fieldValue = byId.getFieldValue("raw");
//        if (fieldValue != null) {
//            MarcRecord fromRAWJSON = MarcRecord.fromRAWJSON(fieldValue.toString());
//            JSONObject jsonString = new JSONObject(fieldValue.toString());
//            System.out.println(jsonString.toString(2));
//            SolrInputDocument sInputDocument = new SolrInputDocument();
//            MarcRecordUtilsToRefactor.marcFields(sInputDocument, fromRAWJSON.dataFields, MarcRecord.tagsToIndex);
//            System.out.println(sInputDocument);
//        }
        
        
        //MarcRecordUtilsToRefactor.marcFields(cDoc, origJSON.dataFields, MarcRecord.tagsToIndex);

        marcId(client,"oai:aleph-nkp.cz:SKC01-002517039");
        //System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000635066");

//        marcId(client, "oai:aleph-nkp.cz:SKC01-001889589");
        
        
        //find(client, "oai:aleph-nkp.cz:SKC01-008768210");
//        parovani1(client);
//        parovani2(client);
    }

    private static  Pair<Case, List<String>>  find(SolrClient client, String id)
            throws JsonProcessingException, SolrServerException, IOException {
        SolrDocument byId = client.getById( DataCollections.catalog.name(), id);
        MarcRecord mRec = MarcRecord.fromSolrDoc(byId);
        //MarcRecord.fromSolrDoc(solrDocument);
                //MarcRecord mRec = MarcRecord.fromIndex(id);
        Pair<Case,List<String>> findSKCFollowers = null;
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println(id+" => "+findSKCFollowers);
        }
        return findSKCFollowers;
    }

    private static void parovani2(SolrClient client)
            throws JsonProcessingException, SolrServerException, IOException {

        marcId(client,"oai:aleph-nkp.cz:SKC01-000443789");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-009552336");

        marcId(client,"oai:aleph-nkp.cz:SKC01-001635354");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-009552640");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000496649");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-009544838");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000895870");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-002716444");

        marcId(client,"oai:aleph-nkp.cz:SKC01-007244876");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-008202989");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000889174");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-002215428");
        
        marcId(client,"oai:aleph-nkp.cz:SKC01-000433419");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-007863491");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000908631");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-005044279");

        marcId(client,"oai:aleph-nkp.cz:SKC01-001038063");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-007545258");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000091644");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-009552821");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000567384");
        System.out.println("No record");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000641203");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-009553375");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000782689");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000782688");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000707837");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-003209434");

        marcId(client,"oai:aleph-nkp.cz:SKC01-002320119");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000789960");
        
        marcId(client,"oai:aleph-nkp.cz:SKC01-000505245");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000635066");
        
        marcId(client,"oai:aleph-nkp.cz:SKC01-000596247");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000635066");

        
        marcId(client,"oai:aleph-nkp.cz:SKC01-000596247");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000635066");

        marcId(client,"oai:aleph-nkp.cz:SKC01-001066168");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-002265469");
        
        marcId(client,"oai:aleph-nkp.cz:SKC01-000467799");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000464801");
        
        marcId(client,"oai:aleph-nkp.cz:SKC01-000587117");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000614648");

        marcId(client,"oai:aleph-nkp.cz:SKC01-002862937");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-005598178");

        marcId(client,"oai:aleph-nkp.cz:SKC01-007152213");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-000511554");

        marcId(client,"oai:aleph-nkp.cz:SKC01-003370848");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-006805750");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000909403");
        System.out.println("Expecting  oai:aleph-nkp.cz:SKC01-005455737");
        //oai:aleph-nkp.cz:SKC01-000596247
        //oai:aleph-nkp.cz:SKC01-000505245
    }    

    private static void parovani1(SolrClient client)
            throws JsonProcessingException, SolrServerException, IOException {

        marcId(client,"oai:aleph-nkp.cz:SKC01-002046715");
        System.out.println("Expecting no record");

        marcId(client,"oai:aleph-nkp.cz:SKC01-001813225");
        System.out.println("Expecting no record");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000785658");
        System.out.println("Expecting no record");

        marcId(client,"oai:aleph-nkp.cz:SKC01-000785658");
        System.out.println("Expecting no record");

        marcId(client,"oai:aleph-nkp.cz:SKC01-008768253");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-000890002");

        marcId(client,"oai:aleph-nkp.cz:SKC01-004947544");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-009552727");

        marcId(client,"oai:aleph-nkp.cz:SKC01-002319979");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-004668043");

        marcId(client,"oai:aleph-nkp.cz:SKC01-005965771");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-000427160");
        
        marcId(client,"oai:aleph-nkp.cz:SKC01-005965772");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-000408694");
        
        marcId(client,"oai:aleph-nkp.cz:SKC01-008120674");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-000592230");
        
        marcId(client, "oai:aleph-nkp.cz:SKC01-008768214");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-001456465");

        marcId(client, "oai:aleph-nkp.cz:SKC01-002363860");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-005965773");
        
        marcId(client, "oai:aleph-nkp.cz:SKC01-002948691");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-008036353");
        
        marcId(client, "oai:aleph-nkp.cz:SKC01-000421140");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-005965774");

        marcId(client, "oai:aleph-nkp.cz:SKC01-003219768");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-000871973");

        marcId(client, "oai:aleph-nkp.cz:SKC01-001779549");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-000409872");

        marcId(client, "oai:aleph-nkp.cz:SKC01-001565724");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-000868935");

        marcId(client, "oai:aleph-nkp.cz:SKC01-001095871");
        System.out.println("No record");
        
        marcId(client, "oai:aleph-nkp.cz:SKC01-000687659");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-009547277");
        

        marcId(client, "oai:aleph-nkp.cz:SKC01-000687659");
        System.out.println("Expecting oai:aleph-nkp.cz:SKC01-009547277");

    }

    private static void marcId(SolrClient client, String id)
            throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mRec;
        Pair<Case, List<String>> findSKCFollowers;
        //mRec = MarcRecord.fromIndex(id);
        SolrDocument byId = client.getById( DataCollections.catalog.name(), id);
        mRec = MarcRecord.fromSolrDoc(byId);
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println(id+" => "+findSKCFollowers);
        }
    }

    private static void ccnb(SolrClient client)
            throws JsonProcessingException, SolrServerException, IOException {
        MarcRecord mRec = MarcRecord.fromIndex("oai:aleph-nkp.cz:SKC01-002046715");
        Pair<Case,List<String>> findSKCFollowers = null;
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println("oai:aleph-nkp.cz:SKC01-002046715 => "+findSKCFollowers);
        }
        
        mRec = MarcRecord.fromIndex("oai:aleph-nkp.cz:SKC01-001813225");
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println("oai:aleph-nkp.cz:SKC01-001813225 =>"+findSKCFollowers);
        }

        mRec = MarcRecord.fromIndex("oai:aleph-nkp.cz:SKC01-000785658");
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println("oai:aleph-nkp.cz:SKC01-000785658 => "+findSKCFollowers);
        }
 
        mRec = MarcRecord.fromIndex("oai:aleph-nkp.cz:SKC01-004947544");
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println("oai:aleph-nkp.cz:SKC01-004947544 => "+findSKCFollowers);
        }
        mRec = MarcRecord.fromIndex("oai:aleph-nkp.cz:SKC01-002319979");
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println("oai:aleph-nkp.cz:SKC01-002319979 => "+findSKCFollowers);
        }
        mRec = MarcRecord.fromIndex("oai:aleph-nkp.cz:SKC01-002363860");
        if (mRec != null) {
            findSKCFollowers = DuplicateSKCUtils.findSKCFollowers(client, mRec);
            System.out.println("oai:aleph-nkp.cz:SKC01-002363860 => "+findSKCFollowers);
        }

        marcId(client, "oai:aleph-nkp.cz:SKC01-002948691");
        marcId(client, "oai:aleph-nkp.cz:SKC01-000421140");
        marcId(client, "oai:aleph-nkp.cz:SKC01-001779549");
        marcId(client, "oai:aleph-nkp.cz:SKC01-001095871");
        marcId(client, "oai:aleph-nkp.cz:SKC01-000687659");
   }
}
