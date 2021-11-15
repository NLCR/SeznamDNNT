package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.period_3;
import static cz.inovatika.sdnnt.model.Period.period_4;

/**
 * Workflow pro omezeni na terminal s lhutou 18 mesicu
 */
public class VNLWorkflow extends AbstractZadostWorkflow {

    public VNLWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        if (owner.getWorkflowState() == null || owner.getWorkflowState() == A || owner.getWorkflowState() == PA) {
            return new WorkflowState(this.owner, NL, License.dnntt,owner.getWorkflowDate(), period_3, true, false);
        } else if (owner.getWorkflowState()== NL) {
            return new WorkflowState(this.owner, A, License.dnnto, owner.getWorkflowDate(), period_4, true, true);
        }
        return null;
    }
}
