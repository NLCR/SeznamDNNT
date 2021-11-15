package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Reprezentuje workflow stav
 *
 */
public class WorkflowState {

    public static final Logger LOGGER = Logger.getLogger(WorkflowState.class.getName());

    // Kuratorsky stav
    private CuratorItemState curatorState;

    // workflow owner -
    private WorkflowOwner workflowOwner;

    private Period period;
    private Date date;
    private License license;

    private boolean changingLicence;
    private boolean finalSate;

    public WorkflowState(WorkflowOwner workflowOwner, CuratorItemState cstate, License license, Date date,  Period period, boolean changingLicense, boolean finalSate) {
        this.workflowOwner = workflowOwner;
        this.curatorState = cstate;
        this.date = date;
        this.period = period;
        this.finalSate = finalSate;
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




    public void applyState() {
        LOGGER.info(String.format("Switching to state %s with period %s, deadline %s", getCuratorState().name(), getPeriod(), getDate()));
        this.workflowOwner.setWorkflowState(getCuratorState());
        this.workflowOwner.applyPeriod(getPeriod());
    }
}
