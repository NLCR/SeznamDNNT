package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * PXN Workflow
 */
public class PXWorkflow extends Workflow {

    public PXWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        Period period = getPeriod(currentState);
        if (owner.getWorkflowState() == null || (currentState != X && currentState != PX) ) {
            return new WorkflowState(this.owner, X, null,/*owner.getWorkflowDate(),*/ period, false,true
                    , true);
        }
        return null;
    }

    @Override
    public boolean isClosableState() {
        return this.getOwner().getWorkflowState() != null &&
                this.getOwner().getWorkflowState().equals(X);
    }

    @Override
    public boolean isSwitchPossible() {
        return isSwitchPossible(null);
    }

    
    
    @Override
    public boolean isSwitchPossible(CuratorItemState desiredState) {
        CuratorItemState cstate = this.getOwner().getWorkflowState();
        if ((cstate == null  || (cstate != X && cstate != PX))) {
            if (desiredState != null) {
                return desiredState.equals(X);
            } else return true;
        } else return false;
    }

    @Override
    public boolean isAlternativeSwitchPossible(String alternative, CuratorItemState desiredState) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public WorkflowState nextAlternativeState(String alternative, SwitchStateOptions options) {
        return null;
    }

    @Override
    public boolean isAlternativeSwitchPossible(String alternative) {
        return false;
    }

    @Override
    public boolean userDefinedWorkflow() {
        return false;
    }

    private Period getPeriod(CuratorItemState state) {
        boolean debug = false;
        if (Options.getInstance().getJSONObject("workflow").has("periods") && Options.getInstance().getJSONObject("workflow").getJSONObject("periods").has("debug")) {
            debug = Options.getInstance().getJSONObject("workflow").getJSONObject("periods").getBoolean("debug");
        }
        return debug ? debug_px_0_5wd : period_px_0_5wd;
    }

}
