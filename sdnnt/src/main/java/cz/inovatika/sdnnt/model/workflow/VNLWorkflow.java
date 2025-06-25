package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import java.util.Arrays;
import java.util.Date;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * Workflow for terminal
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
        if ((
                owner.getWorkflowState() == null ||
                (owner.getWorkflowState() == PN && owner.getPublicState() == PublicItemState.A) ||
                (owner.getWorkflowState() == PN && owner.getPublicState() == PublicItemState.PA) ||
                owner.getWorkflowState() == A ||
                owner.getWorkflowState() == PA) &&
                (owner.getLicense() == null || owner.getLicense().equals(License.dnnto.name()))){
            return new WorkflowState(this.owner, NL, License.dnntt, /*owner.getWorkflowDate(),*/ period, true,true,  false);
        } else if (owner.getWorkflowState()== NL) {
            return new WorkflowState(this.owner, NLX, License.dnntt, /*owner.getWorkflowDate(), */period, false, false,false);
        } else if (owner.getWorkflowState()== NLX) {
            //this.getOwner().getWorkflowState()
            return new WorkflowState(this.owner, A, License.dnnto, /*owner.getWorkflowDate(),*/ period, true,false,  true);
        }
        return null;
    }

    
    
    @Override
    public boolean isSwitchPossible(CuratorItemState desiredState) {
        if (this.owner.getWorkflowState() != null) {
            if (this.getOwner().getWorkflowState().equals(NL)) {
                return true;
            } else if (this.getOwner().getWorkflowState().equals(NLX)) {
                return true;
            } else {
                CuratorItemState cstate = this.getOwner().getWorkflowState();
                boolean flag = Arrays.asList(A, PA, PX, X).contains(cstate);
                if (flag) return flag;
                else {
                    if (cstate == PN) {
                        return Arrays.asList(PublicItemState.A, PublicItemState.PA).contains(this.getOwner().getPublicState());
                    }
                    return false;
                }
            }
        } else return false;
    }

    @Override
    public boolean isAlternativeSwitchPossible(String alternative, CuratorItemState desiredState) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSwitchPossible() {
        if (this.owner.getWorkflowState() != null) {
            if (this.getOwner().getWorkflowState().equals(NL)) {
                return true;
            } else if (this.getOwner().getWorkflowState().equals(NLX)) {
                return true;
            } else {
                CuratorItemState cstate = this.getOwner().getWorkflowState();
                boolean flag = Arrays.asList(A, PA, PX, X).contains(cstate);
                if (flag) return flag;
                else {
                    if (cstate == PN) {
                        return Arrays.asList(PublicItemState.A, PublicItemState.PA).contains(this.getOwner().getPublicState());
                    }
                    return false;
                }
            }
        } else return false;
    }

    @Override
    public WorkflowState nextAlternativeState(String stateHint, SwitchStateOptions options) {
        if (this.getOwner().getWorkflowState().equals(NLX) && stateHint != null && TITLE_RELEASED.equals(stateHint)) {
            return new WorkflowState(this.owner, A, License.dnntt, /*owner.getWorkflowDate(),*/ null, true,  false, true);
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
        if (state == null) return debug ? debug_vnl_0_5wd : period_vln_0_5wd;
        else {
            switch(state) {
                case NPA: return debug ? debug_vnl_0_5wd : period_vln_0_5wd;
                case PA: return debug ? debug_vnl_0_5wd : period_vln_0_5wd;
                case PN: return debug ? debug_vnl_0_5wd : period_vln_0_5wd;
                case NLX: return debug ? debug_vnl_3_5wd : period_vln_3_5wd;
                case NL: return debug ? debug_vnl_2_18m : period_vln_2_18m;
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

    @Override
    public boolean userDefinedWorkflow() {
        return true;
    }
}
