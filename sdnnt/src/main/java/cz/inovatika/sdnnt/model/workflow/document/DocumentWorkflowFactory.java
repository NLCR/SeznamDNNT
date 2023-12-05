package cz.inovatika.sdnnt.model.workflow.document;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.*;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateProxy;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.IndexService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.IndexServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONObject;


public class DocumentWorkflowFactory {

    private DocumentWorkflowFactory() {}

    /**
     * @param kuratorstav
     * @param stav
     * @param license
     * @return
     */
    public static List<ZadostTypNavrh> canBePartOfZadost(List<String> kuratorstav, List<String> stav, String license) {
        CuratorItemState curatorItemState = null;
        if (kuratorstav != null && !kuratorstav.isEmpty()) {
            curatorItemState = CuratorItemState.valueOf(kuratorstav.get(0));
        }

        PublicItemState publicItemState = null;
        if (stav != null && !stav.isEmpty()) {
            publicItemState = PublicItemState.valueOf(stav.get(0));
        }

        DocumentCheckProxy checkProxy = new DocumentCheckProxy(curatorItemState, publicItemState, license);

        List<ZadostTypNavrh> zadostTypNavrhs = new ArrayList<>();
        if(nznDocument(kuratorstav, stav)) {
            NZNWorkflow nznWorkflow = new NZNWorkflow(checkProxy);
            WorkflowState workflowState = nznWorkflow.nextState();
            if (workflowState != null && workflowState.isFirstTransition()) {
                zadostTypNavrhs.add(ZadostTypNavrh.NZN);
            }
        }
        if (vnlDocument(kuratorstav)) {
            VNLWorkflow vnlWorkflow = new VNLWorkflow(checkProxy);
            WorkflowState workflowState = vnlWorkflow.nextState();
            if (workflowState != null && workflowState.isFirstTransition()) {
                zadostTypNavrhs.add(ZadostTypNavrh.VNL);
            }
        }
        if (vnzDocument(kuratorstav, license, stav)) {
            VNZWorkflow vnzWorkflow = new VNZWorkflow(checkProxy);
            WorkflowState workflowState = vnzWorkflow.nextState();
            if (workflowState != null && workflowState.isFirstTransition()) {
                zadostTypNavrhs.add(ZadostTypNavrh.VNZ);
            }
        }
        if (vnDocument(kuratorstav)) {
            VNWorkflow pxWorkflow = new VNWorkflow(checkProxy);
            WorkflowState workflowState = pxWorkflow.nextState();
            if (workflowState != null && workflowState.isFirstTransition()) {
                zadostTypNavrhs.add(ZadostTypNavrh.VN);
            }
        }
        return zadostTypNavrhs;
   }

    public static Workflow create(MarcRecord record, Zadost zadost) throws DocumentProxyException {
        List<String> publicstav = record.dntstav;
        List<String> kuratorstav = record.kuratorstav;
        String license = record.license;
        String navrh = zadost.getNavrh();
        if (navrh != null && ZadostTypNavrh.find(navrh) != null) {

            switch (ZadostTypNavrh.find(navrh)) {
                case NZN: {
                    if (nznDocument(kuratorstav,publicstav)) { return new NZNWorkflow(new DocumentProxy(record, zadost)); }
                    else return null;
                }
                case VNL: {
                    if (vnlDocument(kuratorstav)) { return new VNLWorkflow(new DocumentProxy(record, zadost));}
                    else return null;
                }
                case VNZ: {
                    if (vnzDocument(kuratorstav, license, publicstav)) { return new VNZWorkflow(new DocumentProxy(record, zadost));}
                    else return null;
                }
                case VN: {
                    if (vnDocument(kuratorstav)) { return new VNWorkflow(new DocumentProxy(record, zadost));}
                    else return null;
                }
                case PXN: {
                    if (pxnDocument(kuratorstav)) { return new PXWorkflow(new DocumentProxy(record, zadost));}
                    else return null;
                }
                case DXN : {
                    if (dxnDocument(kuratorstav)) { 
                        try {
                            // musim najit nastupce - Uz mam v dokumentu; vzdy
                            AccountService accountService = new AccountServiceImpl();
                            // pokud ma nejake nasledovniky
                            List<String> navrhy = Arrays.asList("NZN","VN","VNL","VNZ","PXN");
                            List<JSONObject> foundRequests = accountService.findAllRequestForGivenIds(null, navrhy, null,  Arrays.asList(record.identifier));
                            List<Zadost> allRequests = foundRequests.stream().map(Object::toString).map(Zadost::fromJSON).collect(Collectors.toList());
                            
                            IndexService indexService = new IndexServiceImpl();
                            List<MarcRecord> followers = indexService.findById(record.followers);
                            // vsechny pozadavky
                            return new DXWorkflow(new DuplicateProxy(record,zadost, followers, allRequests));
                        } catch (AccountException | IOException | SolrServerException e) {
                            throw new DocumentProxyException(e);
                        } 
                    }
                    else return null;
                }
                    
                default:  return null;
            }
        }
        return null;
    }

    // jakykoliv stav ve workflow
    private static boolean vnlDocument(List<String> kuratorstav) {

        return  kuratorstav.contains(CuratorItemState.NL.name()) ||
                kuratorstav.contains(CuratorItemState.NLX.name()) ||
                kuratorstav.contains(CuratorItemState.A.name()) ||
                kuratorstav.contains(CuratorItemState.PA.name());
    }

    private static boolean vnzDocument(List<String> kuratorstav, String license, List<String> publicstav) {
        boolean inStateAPA =  kuratorstav.contains(CuratorItemState.A.name()) ||
                kuratorstav.contains(CuratorItemState.PA.name());

        boolean inCurratorStavDXPX = kuratorstav.contains(CuratorItemState.DX.name()) || kuratorstav.contains(CuratorItemState.PX.name());
        
        if (inStateAPA) {
            boolean isCorrectLicense =  license != null && License.dnnto.name().equals(license);
            return inStateAPA && isCorrectLicense;
        } else if (inCurratorStavDXPX) {
            
            boolean isCorrectLicense =  license != null && License.dnnto.name().equals(license);

            boolean inPublicStateAPA =  publicstav.contains(PublicItemState.A.name()) ||
                    publicstav.contains(PublicItemState.PA.name());

            return inPublicStateAPA && isCorrectLicense;
        }
        
        return false;
    }

    private static boolean vnDocument(List<String> kuratorstav) {
        return  kuratorstav.contains(CuratorItemState.A.name()) ||
                kuratorstav.contains(CuratorItemState.PA.name()) ||
                kuratorstav.contains(CuratorItemState.NL.name()) ||
                kuratorstav.contains(CuratorItemState.NLX.name()) ||
                // zda muze vyrazovat ?? 
                kuratorstav.contains(CuratorItemState.DX.name()) ||
                kuratorstav.contains(CuratorItemState.PX.name());
                
    }

    private static boolean pxnDocument(List<String> kuratorstav) {
        return  !kuratorstav.contains(CuratorItemState.X.name()) &&
                !kuratorstav.contains(CuratorItemState.PX.name());
    }

    private static boolean dxnDocument(List<String> kuratorstav) {
        return  kuratorstav.contains(CuratorItemState.DX.name());
    }
    
    private static boolean nznDocument(List<String> kuratorstav, List<String> publicstav) {
        if (kuratorstav.isEmpty()) return true;
        else {
            // kurator stav PX, DX
            if (kuratorstav.contains(CuratorItemState.N.name()) ||
                kuratorstav.contains(CuratorItemState.NPA.name()) ||
                kuratorstav.contains(CuratorItemState.PA.name())) {
                return true;
            }
            
            if (publicstav != null) {
                return publicstav.contains(PublicItemState.N.name()) || publicstav.contains(PublicItemState.PA.name());
            }
            
            return false;

        }
    }
}
