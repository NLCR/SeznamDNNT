package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import java.util.Date;
import java.util.logging.Logger;

/**
 * This class represents one state in workflow
 */
public class WorkflowState {

    public static final Logger LOGGER = Logger.getLogger(WorkflowState.class.getName());

    /** Curator state */
    private CuratorItemState curatorState;
    
    // verejny stav v pripade, ze neni zrejmy z kuratorskeho stavu DX, PX
    private PublicItemState expectingPublicState;
    

    /** workflow owner - Dokument, Zadost */
    private WorkflowOwner workflowOwner;

    /** Period between bound to transition betewwn previous and this state*/
    private Period period;

    /** Calculated date between transition */
    //private Date date;

    /** License associated with the state*/
    private License license;

    /** Flag - true if this workflow transition is changing license*/
    private boolean changingLicence;

    /** Flag - true if this workflow state is final state */
    private boolean finalSate;

    /** Flag - true if this workflow state is first state - first transition */
    private boolean firstTransition;

    
    public WorkflowState(WorkflowOwner workflowOwner, CuratorItemState cstate, License license, /*Date date,*/ Period period, boolean changingLicense, boolean startTransition, boolean finalSate/*, String transitionName*/) {
        this.workflowOwner = workflowOwner;
        this.curatorState = cstate;
        this.period = period;
        this.finalSate = finalSate;
        this.firstTransition = startTransition;
        this.changingLicence = changingLicense;
        this.license = license;
    }

    public WorkflowState(WorkflowOwner workflowOwner, CuratorItemState cstate, PublicItemState expPublicItemState, License license, /*Date date,*/ Period period, boolean changingLicense, boolean startTransition, boolean finalSate/*, String transitionName*/) {
        this.workflowOwner = workflowOwner;
        this.curatorState = cstate;
        this.period = period;
        this.finalSate = finalSate;
        this.firstTransition = startTransition;
        this.changingLicence = changingLicense;
        this.license = license;
        this.expectingPublicState = expPublicItemState;
    }
    

    public CuratorItemState getCuratorState() {
        return curatorState;
    }

    public Period getPeriod() {
        return period;
    }

    public License getLicense() {
        return license;
    }

    public boolean isChangingLicence() {
        return changingLicence;
    }

    public boolean isFinalSate() {
        return finalSate;
    }


    public boolean isFirstTransition() {
        return this.firstTransition;
    }

    public void switchState(String originator, String user, String poznamka, SwitchStateOptions options, MarcRecordDependencyStore depStore) {

        boolean shouldChangeLicense = false;
        License expectingLicense = getLicense();
        if (expectingLicense == null && this.workflowOwner.getLicense() != null) {
            shouldChangeLicense = true;
        } else if (expectingLicense != null && this.workflowOwner.getLicense() == null) {
            shouldChangeLicense = true;
        } else if (expectingLicense != null && this.workflowOwner.getLicense() != null &&  !expectingLicense.name().equals(this.workflowOwner.getLicense())) {
            shouldChangeLicense = true;
        }
        // pokud workflow ocekava public state, prepinani NZN pro kuratorske stavy PX a DX
        if (this.expectingPublicState != null) {
            this.workflowOwner.switchWorkflowState(depStore, options,getCuratorState(), expectingPublicState, getLicense() != null ? getLicense().name() : null,shouldChangeLicense , this. getPeriod(), originator, user, poznamka);
        } else {
            this.workflowOwner.switchWorkflowState(depStore, options, getCuratorState(), getLicense() != null ? getLicense().name() : null,shouldChangeLicense , this. getPeriod(), originator, user, poznamka);
        }
        this.workflowOwner.setPeriodBetweenStates(getPeriod());
        
    }
}
