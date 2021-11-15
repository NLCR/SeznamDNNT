package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;

/**
 * Reprezentuje workflow nad zadosti
 */
public abstract class AbstractZadostWorkflow extends Workflow {

    public AbstractZadostWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    /**
     * Podle typu zadosti vytvari workflow
     * @param zadost
     * @return
     */
    public static AbstractZadostWorkflow create(Zadost zadost) {
        String navrh = zadost.getNavrh();
        if (navrh != null) {
            switch (navrh.toLowerCase()) {
                case "nzn": return new NZNWorkflow(new ZadostProxy(zadost));
                case "vn": return new VNWorkflow(new ZadostProxy(zadost));
                case "vnz": return new VNZWorkflow(new ZadostProxy(zadost));
                case "vnl": return new VNLWorkflow(new ZadostProxy(zadost));
                default: new NZNWorkflow(new ZadostProxy(zadost));
            }
        }
        return null;
    }

}
