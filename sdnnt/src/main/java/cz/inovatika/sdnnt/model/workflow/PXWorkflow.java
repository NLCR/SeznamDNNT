package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * Z nezarazenych
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
            return new WorkflowState(this.owner, X, null,owner.getWorkflowDate(), period, false, true);
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
        CuratorItemState cstate = this.getOwner().getWorkflowState();
        return (cstate == null  || (cstate != X && cstate != PX));
    }

    @Override
    public WorkflowState nextAlternativeState(String alternative) {
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
