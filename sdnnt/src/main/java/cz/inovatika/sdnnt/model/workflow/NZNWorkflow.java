package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * Workflow pro zadost typu NZN - navrh na zarazeni
 */
public class NZNWorkflow extends Workflow {

    public NZNWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        Period period = getPeriod(currentState);
        if (currentState == null) {
            return new WorkflowState(owner, NPA,null, owner.getWorkflowDate(), period,true,   false);
        } else {
            switch(currentState) {
                case N:
                    return new WorkflowState(owner,NPA, null, owner.getWorkflowDate(), period, false,  false);
                case NPA:
                    return new WorkflowState(owner, PA, License.dnnto, owner.getWorkflowDate() ,period, true ,  false);
                case PA:
                    return new WorkflowState(owner, A, License.dnnto, owner.getWorkflowDate(), period, false,  true);
                default:
                    return null;

            }
        }
    }

    @Override
    public WorkflowState nextAlternativeState(String stateHint) {
        return null;
    }

    @Override
    public boolean isAlternativeSwitchPossible(String alternative) {
        return false;
    }

    @Override
    public boolean isSwitchPossible() {
        if (this.getOwner().getWorkflowState() == null || !this.getOwner().getWorkflowState().equals(A)) {
            return getOwner().isSwitchToNextStatePossible(getPeriod(getOwner().getWorkflowState()));
        } else {
            return false;
        }
    }

    private Period getPeriod(CuratorItemState state) {
        boolean debug = false;
        if (Options.getInstance().getJSONObject("workflow").has("periods") && Options.getInstance().getJSONObject("workflow").getJSONObject("periods").has("debug")) {
            debug = Options.getInstance().getJSONObject("workflow").getJSONObject("periods").getBoolean("debug");
        }
        if (state == null) return debug ? debug_nzn_0_5wd : period_nzn_0;
        else {
            switch(state) {
                case N: return debug? debug_nzn_0_5wd : period_nzn_0;
                case NPA: return debug ? debug_nzn_1_12_18 : period_nzn_1;
                case PA: return debug ? debug_nzn_2_6m : period_nzn_2;
                default: return null;
            }
        }
    }

    @Override
    public WorkflowOwner getOwner() {
        return owner;
    }

    @Override
    public boolean isClosableState() {
        CuratorItemState workflowState = getOwner().getWorkflowState();
        return workflowState != null && workflowState.equals(A);
    }
}
