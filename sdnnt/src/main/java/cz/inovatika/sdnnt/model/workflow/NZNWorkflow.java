package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

import java.util.Date;

/**
 * Pokryva scenar navrhu na zarazeni dila, kontroluje lhuty a prepina stav
 * <p>
 * </p>
 */
public class NZNWorkflow extends Workflow {

    public NZNWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        PublicItemState pState = owner.getPublicState();
        Period period = getPeriod(currentState);
        if (currentState == null) {
            return new WorkflowState(owner, NPA,null, /*owner.getWorkflowDate(),*/ period,true,  true, false);
        } else {
            switch(currentState) {
                case N:
                    return new WorkflowState(owner,NPA, null, /*owner.getWorkflowDate(),*/ period, false,true, false);
                case NPA:
                    return new WorkflowState(owner, PA, License.dnnto, /*owner.getWorkflowDate(),*/period, true ,false,  false);
                case PA:
                    return new WorkflowState(owner, A,  owner.getLicense() != null ? License.valueOf(owner.getLicense()) : License.dnnto, /*owner.getWorkflowDate(),*/ period, false,false,  true);
                case NL:
                case NLX:
                    if (pState != null && pState.equals(PublicItemState.PA)) {
                        return new WorkflowState(owner, A, License.valueOf(owner.getLicense()), /*owner.getWorkflowDate(),*/ period, false,false,  true);
                    } else return null;
                default:
                    return null;

            }
        }
    }

    @Override
    public WorkflowState nextAlternativeState(String stateHint, SwitchStateOptions options) {
        return null;
    }

    @Override
    public boolean isAlternativeSwitchPossible(String alternative) {
        return false;
    }

    @Override
    public boolean isSwitchPossible() {
        if (this.getOwner().getPublicState() != null && this.owner.getPublicState().equals(PublicItemState.PA)) {
            // je tam datum prepnuti, prepinaji se vsichni
//            Date dDate = getOwner().getDeadlineDate();
//            return getOwner().isSwitchToNextStatePossible(dDate, getPeriod(getOwner().getWorkflowState()));
            return true;
        } else if (this.owner.getWorkflowState() != null && !this.getOwner().getWorkflowState().equals(A)) {
//            Date dDate = getOwner().getDeadlineDate();
//            return getOwner().isSwitchToNextStatePossible(dDate, getPeriod(getOwner().getWorkflowState()));
            return true;
        } else if (this.owner.getWorkflowState() == null) {
            return true;
        } else return false;
    }

    private Period getPeriod(CuratorItemState state) {
        boolean debug = false;
        if (Options.getInstance().getJSONObject("workflow").has("periods") && Options.getInstance().getJSONObject("workflow").getJSONObject("periods").has("debug")) {
            debug = Options.getInstance().getJSONObject("workflow").getJSONObject("periods").getBoolean("debug");
        }
        if (state == null) return debug ? debug_nzn_0_5wd : period_nzn_0_5wd;
        else {
            switch(state) {
                case N: return debug? debug_nzn_0_5wd : period_nzn_0_5wd;
                case NPA: return debug ? debug_nzn_1_12_18 : period_nzn_1_12_18;
                case PA: return debug ? debug_nzn_2_6m : period_nzn_2_6m;
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

    @Override
    public boolean userDefinedWorkflow() {
        return true;
    }
}
