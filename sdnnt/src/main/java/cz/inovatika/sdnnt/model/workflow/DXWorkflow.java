package cz.inovatika.sdnnt.model.workflow;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.debug_px_0_5wd;
import static cz.inovatika.sdnnt.model.Period.period_px_0_5wd;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

public class DXWorkflow extends Workflow {

    public DXWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        Period period = getPeriod(currentState);
        if (currentState == null || (currentState == DX) ) {
            return new WorkflowState(this.owner, D, null,/*owner.getWorkflowDate(),*/ period, false,true, true);
        }
        return null;
    }

    @Override
    public boolean isClosableState() {
        return this.getOwner().getWorkflowState() != null &&
                this.getOwner().getWorkflowState().equals(D);
    }

    @Override
    public boolean isSwitchPossible() {
        return true;
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
    
    
    
    @Override
    public boolean isSwitchPossible(CuratorItemState desiredState) {
        if (desiredState != null && desiredState.equals(D)) {
            return true;
        } else return false;
    }

    @Override
    public boolean isAlternativeSwitchPossible(String alternative, CuratorItemState desiredState) {
        // TODO Auto-generated method stub
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
