package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.*;

public class ZadostWorkflowFactory {

    private ZadostWorkflowFactory() { }

    /**
     * Podle typu zadosti vytvari workflow
     * @param zadost
     * @return
     */
    public static Workflow create(Zadost zadost) {
        //Options instance = Options.getInstance();

        String navrh = zadost.getNavrh();
        if (navrh != null && ZadostTypNavrh.find(navrh) != null) {
            switch (ZadostTypNavrh.find(navrh)) {
                case NZN: return new NZNWorkflow(new ZadostProxy(zadost));
                case VN: return new VNWorkflow(new ZadostProxy(zadost));
                case VNZ: return new VNZWorkflow(new ZadostProxy(zadost));
                case VNL: return new VNLWorkflow(new ZadostProxy(zadost));
                // zadosti generovane systemem
                case PXN: return new PXWorkflow(new ZadostProxy(zadost));
                default: new NZNWorkflow(new ZadostProxy(zadost));
            }
        }
        return null;
    }
}
