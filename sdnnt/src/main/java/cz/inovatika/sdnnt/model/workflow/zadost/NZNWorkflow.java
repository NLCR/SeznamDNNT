package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * Workflow pro zadost typu NZN - navrh na zarazeni
 */
public class NZNWorkflow extends AbstractZadostWorkflow {

    public NZNWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        if (currentState == null) {
            return new WorkflowState(owner, NPA, License.dnnto, owner.getWorkflowDate(), period_0,true,   false);
        } else {
            switch(currentState) {
                case N:
                    return new WorkflowState(owner, NPA, License.dnnto, owner.getWorkflowDate(), period_0, true,  false);
                case NPA:
                    return new WorkflowState(owner, PA, License.dnnto, owner.getWorkflowDate() ,period_1, false ,  false);
                case PA:
                    return new WorkflowState(owner, A, License.dnnto, owner.getWorkflowDate(), null, false,  true);
                default:
                    return null;

            }
        }
    }

    @Override
    public WorkflowOwner getOwner() {
        return owner;
    }
}
