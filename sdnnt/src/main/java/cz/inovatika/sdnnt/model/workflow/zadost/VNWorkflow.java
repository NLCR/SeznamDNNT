package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;


/**
 * Workflow pro vyrazeni ze seznamu
 */
public class VNWorkflow extends AbstractZadostWorkflow {


    public VNWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        if (owner.getWorkflowState() == null || owner.getWorkflowState() == A || owner.getWorkflowState() == PA) {
            return new WorkflowState(this.owner, N, null,owner.getWorkflowDate(), period_2, true, true);
        }
        return null;
    }
}
