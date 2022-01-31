package cz.inovatika.sdnnt.model.workflow.document;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.*;

import java.util.ArrayList;
import java.util.List;


public class DocumentWorkflowFactory {

    private DocumentWorkflowFactory() {}


    public static Workflow create(MarcRecord record) {
        List<String> kuratorstav = record.kuratorstav;
        if (kuratorstav != null && !kuratorstav.isEmpty()) {
            if (nznDocument(kuratorstav)) return new NZNWorkflow(new DocumentProxy(record));
            if (vnlDocument(kuratorstav)) return new VNLWorkflow(new DocumentProxy(record));
        }
        return null;
    }

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
        if(nznDocument(kuratorstav)) {
            NZNWorkflow nznWorkflow = new NZNWorkflow(checkProxy);
            WorkflowState workflowState = nznWorkflow.nextState();
            if (workflowState != null) {
                zadostTypNavrhs.add(ZadostTypNavrh.NZN);
            }
        }
        if (vnlDocument(kuratorstav)) {
            VNLWorkflow vnlWorkflow = new VNLWorkflow(checkProxy);
            WorkflowState workflowState = vnlWorkflow.nextState();
            if (workflowState != null) {
                zadostTypNavrhs.add(ZadostTypNavrh.VNL);
            }
        }
        if (vnzDocument(kuratorstav)) {
            VNZWorkflow vnzWorkflow = new VNZWorkflow(checkProxy);
            WorkflowState workflowState = vnzWorkflow.nextState();
            if (workflowState != null) {
                zadostTypNavrhs.add(ZadostTypNavrh.VNZ);
            }
        }
        if (vnDocument(kuratorstav)) {
            VNWorkflow pxWorkflow = new VNWorkflow(checkProxy);
            WorkflowState workflowState = pxWorkflow.nextState();
            if (workflowState != null) {
                zadostTypNavrhs.add(ZadostTypNavrh.VN);
            }
        }
        return zadostTypNavrhs;
   }

    public static Workflow create(MarcRecord record, Zadost zadost) {
        List<String> kuratorstav = record.kuratorstav;
        String navrh = zadost.getNavrh();
        if (navrh != null && ZadostTypNavrh.find(navrh) != null) {

            switch (ZadostTypNavrh.find(navrh)) {
                case NZN: {
                    if (nznDocument(kuratorstav)) { return new NZNWorkflow(new DocumentProxy(record)); }
                    else return null;
                }
                case VNL: {
                    if (vnlDocument(kuratorstav)) { return new VNLWorkflow(new DocumentProxy(record));}
                    else return null;
                }
                case VNZ: {
                    if (vnzDocument(kuratorstav)) { return new VNZWorkflow(new DocumentProxy(record));}
                    else return null;
                }
                case VN: {
                    if (vnDocument(kuratorstav)) { return new VNWorkflow(new DocumentProxy(record));}
                    else return null;
                }
                case PXN: {
                    if (pxnDocument(kuratorstav)) { return new PXWorkflow(new DocumentProxy(record));}
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

    private static boolean vnzDocument(List<String> kuratorstav) {
        return  kuratorstav.contains(CuratorItemState.A.name()) ||
                kuratorstav.contains(CuratorItemState.PA.name());
    }

    private static boolean vnDocument(List<String> kuratorstav) {
        return  kuratorstav.contains(CuratorItemState.A.name()) ||
                kuratorstav.contains(CuratorItemState.PA.name()) ||
                kuratorstav.contains(CuratorItemState.NL.name()) ||
                kuratorstav.contains(CuratorItemState.NLX.name());
    }

    private static boolean pxnDocument(List<String> kuratorstav) {
        return  !kuratorstav.contains(CuratorItemState.X.name()) &&
                !kuratorstav.contains(CuratorItemState.PX.name());
    }

    private static boolean nznDocument(List<String> kuratorstav) {
        if (kuratorstav.isEmpty()) return true;
        else {
            if (kuratorstav.contains(CuratorItemState.N.name()) ||
                kuratorstav.contains(CuratorItemState.NPA.name()) ||
                kuratorstav.contains(CuratorItemState.PA.name())) {
                return true;
            }
            return false;

        }
    }
}
