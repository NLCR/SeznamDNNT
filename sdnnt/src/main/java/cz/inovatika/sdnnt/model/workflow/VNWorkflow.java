package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;


/**
 * Workflow pro vyrazeni ze seznamu
 */
public class VNWorkflow extends Workflow {


    public VNWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        Period period = getPeriod(currentState);

        if (owner.getWorkflowState() == null ||owner.getPublicState() == PublicItemState.A || owner.getPublicState() == PublicItemState.PA ||  owner.getPublicState() == PublicItemState.NL) {
            return new WorkflowState(this.owner, N,null,/*owner.getWorkflowDate(),*/ period, true,true,  true);
        }
        return null;
    }

    @Override
    public boolean isSwitchPossible() {
        CuratorItemState cstate = this.getOwner().getWorkflowState();
        return (cstate == null || !cstate.equals(N));
    }


    @Override
    public WorkflowState nextAlternativeState(String alternative, SwitchStateOptions options) {
        CuratorItemState currentState = owner.getWorkflowState();
        Period period = getPeriod(currentState);

        if (alternative != null && License.dnntt.name().equals(alternative)) {
            return new WorkflowState(this.owner, owner.getWorkflowState(),License.dnntt,/*owner.getWorkflowDate(),*/ period, true,true,  true);

        }
        return null;
    }


    @Override
    public boolean isAlternativeSwitchPossible(String alternative) {
        return License.dnntt.name().equals(alternative);
    }

    @Override
    public boolean isClosableState() {
        return this.getOwner().getWorkflowState() != null &&
                this.getOwner().getWorkflowState().equals(N);
    }



    private Period getPeriod(CuratorItemState state) {
        boolean debug = false;
        if (Options.getInstance().getJSONObject("workflow").has("periods") && Options.getInstance().getJSONObject("workflow").getJSONObject("periods").has("debug")) {
            debug = Options.getInstance().getJSONObject("workflow").getJSONObject("periods").getBoolean("debug");
        }
        if (state == null) return debug ? debug_vn_0_28d : period_vn_0_28d;
        else {
            switch(state) {
                case PX: return debug ? debug_vn_0_28d : period_vn_0_28d;
                case X: return debug ? debug_vn_0_28d : period_vn_0_28d;
                case A: return debug ? debug_vn_0_28d : period_vn_0_28d;
                case PA: return debug ? debug_vn_0_28d : period_vn_0_28d;
                case NL: return debug ? debug_vn_0_28d : period_vn_0_28d;
                case NLX: return debug ? debug_vn_0_28d : period_vn_0_28d;
                default: return null;
            }
        }
    }

    @Override
    public boolean userDefinedWorkflow() {
        return true;
    }
}
