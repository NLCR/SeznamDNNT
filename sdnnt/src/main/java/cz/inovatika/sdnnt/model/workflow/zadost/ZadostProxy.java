package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.TransitionType;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;

import java.util.Date;

public class ZadostProxy implements WorkflowOwner  {

    private Zadost zadost;

    public ZadostProxy(Zadost zadost) {
        this.zadost = zadost;
    }

    // Workflow owner methods
    @Override
    public CuratorItemState getWorkflowState() {
        return  zadost.getDesiredItemState() != null ? CuratorItemState.valueOf(zadost.getDesiredItemState()) : null;
    }

    @Override
    public void switchWorkflowState(CuratorItemState itm, String license, boolean changingLicenseState, Period period, String originator, String user, String poznamka) {
        this.zadost.setDesiredItemState(itm.name());
        if (changingLicenseState)  this.zadost.setDesiredLicense(license);
        // zadost, nova deadline
        zadost.setDeadline(new Date());
    }

    @Override
    public boolean isSwitchToNextStatePossible(Period period) {
        if (period == null) return true;
        if (period.getTransitionType().equals(TransitionType.kurator)) {
            return true;
        } else {
            Date workflowDate = getWorkflowDate();
            if (this.zadost.getDeadline() == null) {
                // mozne pouze pokud je nynejsi datum za workflowdate na ktery se navic aplikuje perioda
                Date deadlineDay = period.defineDeadline(workflowDate);
                return deadlineDay.before(new Date());
            } else {
                return this.zadost.getDeadline().before(new Date());
            }
        }
    }


    @Override
    public void setLicense(String l) {
        this.zadost.setDesiredLicense(l);
    }

    @Override
    public String getLicense() {
        return this.zadost.getDesiredLicense();
    }

    @Override
    public Date getWorkflowDate() {
        // jeste nedefinovana deadline nebo zadost nema stav
        if (zadost.getDeadline() == null || zadost.getState() == null) {
            return zadost.getDatumZadani();
        }  else if (zadost.getState() != null && zadost.getState().equals("open") && zadost.getDatumVyrizeni() != null) {
            return zadost.getDatumVyrizeni();
        }   else {
            return this.zadost.getDeadline();
        }
    }

    @Override
    public void setWorkflowDate(Date date) {
        this.zadost.setDeadline(date);
    }

    @Override
    public void setPeriodBetweenStates(Period period) {
        Date wflDate = this.getWorkflowDate();
        if (period != null ) {
            setWorkflowDate(period.defineDeadline(wflDate));
            this.zadost.setTransitionType(period.getTransitionType().name());
        } else {
            setWorkflowDate(null);
            this.zadost.setTransitionType(null);
        }
    }

    @Override
    public String toString() {
        return "ZadostProxy{" +
                "zadost=" + zadost +
                '}';
    }
}
