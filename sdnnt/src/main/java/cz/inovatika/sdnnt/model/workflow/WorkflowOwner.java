package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Period;

import java.util.Date;

public interface WorkflowOwner {

    public CuratorItemState getWorkflowState();

    public void setWorkflowState(CuratorItemState itm);

    public Date getWorkflowDate();


    public void setWorkflowDate(Date date);


    //public void setWorkflowDate


    public void applyPeriod(Period period);
}
