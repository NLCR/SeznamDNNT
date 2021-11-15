package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * Workflow pro omezeni na terminal
 */
public class VNZWorkflow extends AbstractZadostWorkflow {

    public VNZWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        if (owner.getWorkflowState() == null || owner.getWorkflowState() == A || owner.getWorkflowState() == PA) {
            return new WorkflowState(this.owner, A, License.dnntt,owner.getWorkflowDate(), period_3, true,  true);
        }
        return null;
    }
}
