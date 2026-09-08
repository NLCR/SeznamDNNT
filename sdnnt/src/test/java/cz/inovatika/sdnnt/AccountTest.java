package cz.inovatika.sdnnt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxyException;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;

public class AccountTest {
    
    
    public static void main(String[] args) throws JsonProcessingException, SolrServerException, IOException, DocumentProxyException, ConflictException, AccountException {

        accountSwitch();
        

    }

    private static void accountSwitch() throws ConflictException, AccountException, IOException, SolrServerException {
        AccountServiceImpl serviceImpl = new AccountServiceImpl();
        serviceImpl.schedulerSwitchStates("bca7dd59-447b-4918-86c1-f9d8fb086be4");
    }

    private static void switchState() throws FileNotFoundException, IOException, JsonProcessingException,
            SolrServerException, DocumentProxyException {
        File file = new File("zadost.json");
        boolean exists = file.exists();
        
        System.out.println(exists);
        
        FileInputStream fis = new FileInputStream(file);
        String string = IOUtils.toString(fis, "UTF-8");
        
        Zadost zadost = Zadost.fromJSON(string);
        System.out.println(zadost);
        
        //SolrClient client =  new HttpSolrClient.Builder("http://localhost:8983/solr").build();
        MarcRecord marcRecord = MarcRecord.fromIndex("oai:aleph-nkp.cz:SKC01-000912766");

        Workflow nznWorkflow = DocumentWorkflowFactory.create(marcRecord, zadost);
        
        boolean switchPossible = nznWorkflow.isSwitchPossible(CuratorItemState.valueOf(zadost.getDesiredItemState()));
        
        System.out.println(switchPossible);

        
        WorkflowState workflowState = nznWorkflow.nextState();
        workflowState.switchState("026a2039-eac6-4afb-a648-c2d4f83673ed", zadost.getUser(), "", null, null);

        System.out.println(marcRecord.dntstav);
    }
}
