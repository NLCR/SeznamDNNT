package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Reprezentuje workflow stav
 */
public class WorkflowState {

    public static final Logger LOGGER = Logger.getLogger(WorkflowState.class.getName());

    // Kuratorsky stav objektu
    private CuratorItemState curatorState;

    // workflow owner - zadost nebo dokument
    private WorkflowOwner workflowOwner;

    // perioda ktera je mezi stavy
    private Period period;

    // datum vypocitane z periody
    private Date date;

    // licence odpovidajici stavu
    private License license;

    // zda se meni licence
    private boolean changingLicence;

    // priznak zda je stav finalni
    private boolean finalSate;

    private boolean firstTransition;

    //private String transitionName;

    public WorkflowState(WorkflowOwner workflowOwner, CuratorItemState cstate, License license, Date date, Period period, boolean changingLicense, boolean startTransition, boolean finalSate/*, String transitionName*/) {
        this.workflowOwner = workflowOwner;
        this.curatorState = cstate;
        this.date = date;
        this.period = period;
        this.finalSate = finalSate;
        this.firstTransition = startTransition;
        this.changingLicence = changingLicense;
        this.license = license;

        //this.transitionName = transitionName;
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
