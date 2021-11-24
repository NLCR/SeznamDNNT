package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

import java.util.Arrays;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * Workflow pro omezeni na terminal s lhutou 18 mesicu
 */
public class VNLWorkflow extends Workflow {

    public static final String TITLE_RELEASED = "title_released";

    public VNLWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        Period period = getPeriod(currentState);
        if (owner.getWorkflowState() == null || owner.getWorkflowState() == A || owner.getWorkflowState() == PA) {
            return new WorkflowState(this.owner, NL, License.dnntt,owner.getWorkflowDate(), period, true, false);
        } else if (owner.getWorkflowState()== NL) {
            return new WorkflowState(this.owner, NLX, License.dnntt, owner.getWorkflowDate(), period, false, false);
        } else if (owner.getWorkflowState()== NLX) {
            return new WorkflowState(this.owner, A, License.dnntt, owner.getWorkflowDate(), period, true, true);
        }
        return null;
    }

    @Override
    public boolean isSwitchPossible() {
        if (this.getOwner().getWorkflowState().equals(NL)) {
            return this.getOwner().isSwitchToNextStatePossible(getPeriod(NL));
        } else if (this.getOwner().getWorkflowState().equals(NLX)) {
            return this.getOwner().isSwitchToNextStatePossible(getPeriod(NLX));
        } else {
            CuratorItemState cstate = this.getOwner().getWorkflowState();
            return Arrays.asList(A, PA, PX, X).contains(cstate);
        }
    }

    @Override
    public WorkflowState nextAlternativeState(String stateHint) {
        if (this.getOwner().getWorkflowState().equals(NLX) && stateHint != null && TITLE_RELEASED.equals(stateHint)) {
            return new WorkflowState(this.owner, N, null, owner.getWorkflowDate(), null, true, true);
        }
        return null;

    }

    @Override
    public boolean isAlternativeSwitchPossible(String stateHint) {
        if (this.getOwner().getWorkflowState().equals(NLX) && stateHint != null && TITLE_RELEASED.equals(stateHint)) {
            return true;
        }  else return false;
    }


    private Period getPeriod(CuratorItemState state) {
        boolean debug = false;
        if (Options.getInstance().getJSONObject("workflow").has("periods") && Options.getInstance().getJSONObject("workflow").getJSONObject("periods").has("debug")) {
            debug = Options.getInstance().getJSONObject("workflow").getJSONObject("periods").getBoolean("debug");
        }
        if (state == null) return debug ? debug_vnl_0_5wd : period_vln_0;
        else {
            switch(state) {
                case NPA: return debug ? debug_vnl_0_5wd : period_vln_0;
                case PA: return debug ? debug_vnl_0_5wd : period_vln_0;
                case NLX: return debug ? debug_vnl_3_5wd : period_vln_3;
                case NL: return debug ? debug_vnl_2_18m : period_vln_2;
                default: return null;
            }
        }
    }


    @Override
    public boolean isClosableState() {
        CuratorItemState workflowState = getOwner().getWorkflowState();
        return workflowState != null &&
                (workflowState.equals(A) || workflowState.equals(PA) || workflowState.equals(N));
    }
}
