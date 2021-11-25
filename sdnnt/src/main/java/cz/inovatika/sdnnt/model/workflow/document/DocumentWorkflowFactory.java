package cz.inovatika.sdnnt.model.workflow.document;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.*;

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


    public static Workflow create(MarcRecord record, Zadost zadost) {
        List<String> kuratorstav = record.kuratorstav;
        String navrh = zadost.getNavrh();
        if (navrh != null && ZadostType.find(navrh) != null) {

            switch (ZadostType.find(navrh)) {
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
                default:  return null;
            }
        }
        return null;
    }

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
                kuratorstav.contains(CuratorItemState.NL.name());
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