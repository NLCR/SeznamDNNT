package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

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
        if (owner.getWorkflowState() == null || owner.getWorkflowState() == A || owner.getWorkflowState() == PA) {
            if (owner.getWorkflowState() == null) {
                return new WorkflowState(this.owner, A, License.dnntt,owner.getWorkflowDate(), period_vln_0, true,  true);
            } else {
                return new WorkflowState(this.owner, getOwner().getWorkflowState(), License.dnntt,owner.getWorkflowDate(), period_vln_0, true,  true);

            }
        }
        return null;
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
        return true;
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
        if (license == null) return debug ? debug_vnl_0_5wd : period_vln_0;
        else {
            switch(state) {
                case A: return debug ? debug_vnl_0_5wd : period_vln_0;
                case PA: return debug ? debug_vnl_0_5wd : period_vln_0;
                default: return null;
            }
        }
    }

}
