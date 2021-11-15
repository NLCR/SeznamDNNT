package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.CuratorItemState;
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
    public void setWorkflowState(CuratorItemState itm) {
        this.zadost.setDesiredItemState(itm.name());
    }

    @Override
    public Date getWorkflowDate() {
        if (zadost.getDeadline() == null) {
            return zadost.getDatumZadani();
        } else {
            return this.zadost.getDeadline();
        }
    }

    @Override
    public void setWorkflowDate(Date date) {
        this.zadost.setDeadline(date);
    }

    @Override
    public void applyPeriod(Period period) {
        Date wflDate = this.getWorkflowDate();
        if (period != null ) {
            setWorkflowDate(period.defineDeadline(wflDate));
            this.zadost.setTypeOfDeadline(period.getDeadlineType().name());
        } else {
            setWorkflowDate(null);
            this.zadost.setTypeOfDeadline(null);
        }
    }

    @Override
    public String toString() {
        return "ZadostProxy{" +
                "zadost=" + zadost +
                '}';
    }
}
