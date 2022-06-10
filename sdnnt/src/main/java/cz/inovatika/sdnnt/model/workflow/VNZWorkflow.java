package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import static cz.inovatika.sdnnt.model.CuratorItemState.*;
import static cz.inovatika.sdnnt.model.Period.*;

/**
 * Workflow pro omezeni na terminal
 */
public class VNZWorkflow extends Workflow {

    public VNZWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        Period period = getPeriod(owner.getWorkflowState(), owner.getLicense() != null ? License.valueOf(owner.getLicense()) : null);
        if ((owner.getWorkflowState() == null || owner.getWorkflowState() == A || owner.getWorkflowState() == PA) && (owner.getLicense() ==null || owner.getLicense().equals(License.dnnto.name()))) {
            if (owner.getWorkflowState() == null) {
                return new WorkflowState(this.owner, A, License.dnntt,owner.getWorkflowDate(), period, true, true, true);
            } else {
                return new WorkflowState(this.owner, getOwner().getWorkflowState(), License.dnntt,owner.getWorkflowDate(), period, true, true,  true);

            }
        }
        return null;
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
        if ((owner.getWorkflowState() != null) &&  (owner.getWorkflowState() == A || owner.getWorkflowState() == PA) && (owner.getLicense() != null && owner.getLicense().equals(License.dnnto.name()))) {
            return true;
        } else return false;
    }

    @Override
    public boolean isClosableState() {
        return this.getOwner().getLicense() != null && this.getOwner().getLicense().equals(License.dnntt.name());
    }

    private Period getPeriod(CuratorItemState state, License license) {
        boolean debug = false;
        if (Options.getInstance().getJSONObject("workflow").has("periods") && Options.getInstance().getJSONObject("workflow").getJSONObject("periods").has("debug")) {
            debug = Options.getInstance().getJSONObject("workflow").getJSONObject("periods").getBoolean("debug");
        }
        if (license == null) return debug ? debug_vnl_0_5wd : period_vln_0_5wd;
        else {
            switch(state) {
                case A: return debug ? debug_vnl_0_5wd : period_vln_0_5wd;
                case PA: return debug ? debug_vnl_0_5wd : period_vln_0_5wd;
                default: return null;
            }
        }
    }

    @Override
    public boolean userDefinedWorkflow() {
        return true;
    }
}
