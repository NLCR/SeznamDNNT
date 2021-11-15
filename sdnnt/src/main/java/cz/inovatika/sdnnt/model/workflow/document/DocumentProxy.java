package cz.inovatika.sdnnt.model.workflow.document;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;

import java.util.Date;

public class DocumentProxy implements WorkflowOwner {

    MarcRecord marcRecord;

    public DocumentProxy(MarcRecord record) {
        this.marcRecord = record;
    }

    @Override
    public CuratorItemState getWorkflowState() {
        return !this.marcRecord.kuratorstav.isEmpty() ? CuratorItemState.valueOf(this.marcRecord.kuratorstav.get(0)) : null;
    }

    @Override
    public void setWorkflowState(CuratorItemState itm) {

    }

    @Override
    public Date getWorkflowDate() {
        return null;
    }

    @Override
    public void setWorkflowDate(Date date) {

    }

    @Override
    public void applyPeriod(Period period) {

    }
}
