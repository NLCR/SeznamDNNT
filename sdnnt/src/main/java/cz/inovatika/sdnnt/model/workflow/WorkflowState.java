package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

import java.util.Date;
import java.util.logging.Logger;

/**
 * This class represents one state in workflow
 */
public class WorkflowState {

    public static final Logger LOGGER = Logger.getLogger(WorkflowState.class.getName());

    /** Curator state */
    private CuratorItemState curatorState;

    /** workflow owner - Dokument, Zadost */
    private WorkflowOwner workflowOwner;

    /** Period between bound to transition betewwn previous and this state*/
    private Period period;

    /** Calculated date between transition */
    private Date date;

    /** License associated with the state*/
    private License license;

    /** Flag - true if this workflow transition is changing license*/
    private boolean changingLicence;

    /** Flag - true if this workflow state is final state */
    private boolean finalSate;

    /** Flag - true if this workflow state is first state - first transition */
    private boolean firstTransition;

    public WorkflowState(WorkflowOwner workflowOwner, CuratorItemState cstate, License license, Date date, Period period, boolean changingLicense, boolean startTransition, boolean finalSate/*, String transitionName*/) {
        this.workflowOwner = workflowOwner;
        this.curatorState = cstate;
        this.date = date;
        this.period = period;
        this.finalSate = finalSate;
        this.firstTransition = startTransition;
        this.changingLicence = changingLicense;
        this.license = license;
    }

    public Date getDate() {
        return date;
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

    public void switchState(String originator, String user, String poznamka) {

        boolean shouldChangeLicense = false;
        License expectingLicense = getLicense();
        if (expectingLicense == null && this.workflowOwner.getLicense() != null) {
            shouldChangeLicense = true;
        } else if (expectingLicense != null && this.workflowOwner.getLicense() == null) {
            shouldChangeLicense = true;
        } else if (expectingLicense != null && this.workflowOwner.getLicense() != null &&  !expectingLicense.name().equals(this.workflowOwner.getLicense())) {
            shouldChangeLicense = true;
        }

        this.workflowOwner.switchWorkflowState(getCuratorState(), getLicense() != null ? getLicense().name() : null, shouldChangeLicense, this. getPeriod(),originator , user, poznamka);
        this.workflowOwner.setPeriodBetweenStates(getPeriod());
    }
}
