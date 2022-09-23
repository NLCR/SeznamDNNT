package cz.inovatika.sdnnt.model.workflow.document;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.SwitchStateOptions;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrInputDocument;

public class DocumentCheckProxy implements WorkflowOwner {

    private CuratorItemState curatorItemState;
    private PublicItemState publicItemState;
    private String license;

    public DocumentCheckProxy(CuratorItemState curatorItemState, PublicItemState publicItemState, String license) {
        this.curatorItemState = curatorItemState;
        this.publicItemState = publicItemState;
        this.license = license;
    }

    @Override
    public CuratorItemState getWorkflowState() {
        return this.curatorItemState;
    }

    @Override
    public Date getWorkflowDate() {
        return null;
    }



    @Override
    public PublicItemState getPublicState() {
        return this.publicItemState;
    }

    @Override
    public Date getPublicStateDate() {
        return null;
    }

    @Override
    public void switchWorkflowState(SwitchStateOptions options, CuratorItemState itm, String license, boolean changingLicenseState, Period period, String originator, String user, String poznamka) {
        // class is only for checking
    }

    @Override
    public boolean isSwitchToNextStatePossible(Date date, Period period) {
        return false;
    }

    @Override
    public void setWorkflowDate(Date date) {

    }

    @Override
    public void setPeriodBetweenStates(Period period) {

    }

    @Override
    public void setLicense(String l) {

    }

    @Override
    public String getLicense() {
        return this.license;
    }

    @Override
    public List<Pair<String, SolrInputDocument>> getStateToSave(SwitchStateOptions options) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasRejectableWorkload() {
        return false;
    }

    @Override
    public void rejectWorkflowState(String originator, String user, String poznamka) {
    }
    
}
