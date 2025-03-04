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
 */
public class NZNWorkflow extends Workflow {

    public NZNWorkflow(WorkflowOwner owner) {
        super(owner);
    }

    @Override
    public WorkflowState nextState() {
        CuratorItemState currentState = owner.getWorkflowState();
        PublicItemState pState = owner.getPublicState();
        Period period = getPeriod(currentState, pState);
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
                    

                // dilo je predmetem zadosti o volna dila nebo zadosti o duplicitu
                case PN:
                case PX:
                case DX:
                    if (pState != null) {
                        // pravdepodobne volne dilo v zadosti...  N, bez stavu, 
                        switch(pState) {
                            case N:
                                return new WorkflowState(owner,NPA, null, /*owner.getWorkflowDate(),*/ period, false,true, false);
                            case PA:
                                return new WorkflowState(owner, currentState, PublicItemState.A,  owner.getLicense() != null ? License.valueOf(owner.getLicense()) : License.dnnto, /*owner.getWorkflowDate(),*/ period, false,false,  true);
                            case NL:
                                return new WorkflowState(owner, currentState, PublicItemState.A, License.valueOf(owner.getLicense()), /*owner.getWorkflowDate(),*/ period, false,false,  true);
                            default:
                                break;
                        }

                    } else {
                        return new WorkflowState(owner,NPA, null, /*owner.getWorkflowDate(),*/ period, false,true, false);
                    }
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
    public boolean isSwitchPossible(CuratorItemState desiredState) {
        
        
        
        // NPA -> PA
        if (this.getOwner().getWorkflowState() != null && this.owner.getWorkflowState().equals(NPA)) {
            if (desiredState != null) {
                return desiredState.equals(PA);
            } else return true;

        // PA -> A
        } else if (this.getOwner().getWorkflowState() != null && this.owner.getWorkflowState().equals(PA)) {
            
            if (desiredState != null) {
                return desiredState.equals(A);
            } else return true;
            
        } else if (this.getOwner().getWorkflowState() != null && (this.owner.getWorkflowState().equals(PX) || this.owner.getWorkflowState().equals(DX) || this.owner.getWorkflowState().equals(PN))) {

            PublicItemState publicState = this.owner.getPublicState();
            if (publicState != null) {
                switch(publicState) {
                    case N: 
                        if (desiredState != null) {
                            return desiredState.equals(PA);
                        } else return true;
                    case PA: 
                        if (desiredState != null) {
                            return desiredState.equals(A);
                        } else return true;
                    default:
                        return false;
                }
            }
            
            return false;

        // N state  || null
        } else if (this.owner.getWorkflowState() != null && this.getOwner().getWorkflowState().equals(N)) {
        
            if (desiredState != null) {
                return desiredState.equals(PA);
            } else return true;
 
        } else if (this.owner.getWorkflowState() == null) {
            return true;
        } else return false;

    }

    @Override
    public boolean isSwitchPossible() {
        return isSwitchPossible(null);
    }

    @Override
    public boolean isAlternativeSwitchPossible(String alternative, CuratorItemState desiredState) {
        // TODO Auto-generated method stub
        return false;
    }

    private Period getPeriod(CuratorItemState state, PublicItemState publicItemState) {
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
                case PN:
                case PX:
                case DX:
                    if (publicItemState != null) {
                        switch(publicItemState) {
                            case N: return debug? debug_nzn_0_5wd : period_nzn_0_5wd;
                            case PA: return debug ? debug_nzn_2_6m : period_nzn_2_6m;
                            default: return null;
                        }
                    } else return null;
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
