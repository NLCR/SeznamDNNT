package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.*;
import cz.inovatika.sdnnt.model.workflow.SwitchStateOptions;
import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;
import cz.inovatika.sdnnt.services.impl.HistoryImpl;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ZadostProxy implements WorkflowOwner  {

    private Zadost zadost;

    public ZadostProxy(Zadost zadost) {
        this.zadost = zadost;
    }

    @Override
    public CuratorItemState getWorkflowState() {
        return  zadost.getDesiredItemState() != null ? CuratorItemState.valueOf(zadost.getDesiredItemState()) : null;
    }

    @Override
    public Date getPublicStateDate() {
        // u zadodsti je to to same
        return getWorkflowDate();
    }

    @Override
    public PublicItemState getPublicState() {
        return null;
    }

    @Override
    public void switchWorkflowState(SwitchStateOptions options, CuratorItemState itm, String license,  boolean changingLicenseState, Period period, String originator, String user, String poznamka) {
        this.zadost.setDesiredItemState(itm.name());
        if (changingLicenseState)  this.zadost.setDesiredLicense(license);
        // zadost, nova deadline
        zadost.setDeadline(new Date());

        // prapagate rejected state
        List<String> identifiers = zadost.getIdentifiers();
        if (identifiers != null) {
            Map<String, ZadostProcess> process = zadost.getProcess();
            identifiers.stream().forEach(identifier-> {
                if (process != null) {
                    ZadostProcess zp = zadost.getProcess().get(identifier);
                    if (zp != null) {
                        // now in workflow and it is not accessible from here
                        String currentStateName = zadost.getDesiredItemState() != null ? zadost.getDesiredItemState() : "_";
                        String currentLicenseName = zadost.getDesiredLicense() != null ? zadost.getDesiredLicense() : "_";
                        String transitionName = String.format("(%s,%s)", currentStateName, currentLicenseName);

                        ZadostProcess zpCopy = new ZadostProcess();
                        zpCopy.setTransitionName(transitionName);
                        zpCopy.setUser(zp.getUser());
                        zpCopy.setReason(zp.getReason());
                        zpCopy.setState(zp.getState());
                        zpCopy.setDate(zp.getDate());

                        if (zpCopy.getState() != null && zpCopy.getState().equals("rejected")) {
                            zadost.addProcess(identifier, zpCopy);
                        }
                    }
                    
                }
            });
        }
    }
    
    

    @Override
    public void switchWorkflowState(SwitchStateOptions options, CuratorItemState itm,
            PublicItemState expectingPublicState, String license, boolean changingLicenseState, Period period,
            String originator, String user, String poznamka) {
        throw new UnsupportedOperationException("Unsupported for 'Zadost'");
    }

    @Override
    public boolean isSwitchToNextStatePossible(Date date, Period period) {
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
    public List<Pair<String, SolrInputDocument>> getStateToSave(SwitchStateOptions options) {
        Pair<String, SolrInputDocument> pair =Pair.of(DataCollections.zadost.name(), this.zadost.toSolrInputDocument());
        return Arrays.asList(pair);
    }

    
    
    @Override
    public boolean hasRejectableWorkload() {
        return false;
    }

    @Override
    public void rejectWorkflowState(String originator, String user, String poznamka) {
    }

    @Override
    public String toString() {
        return "ZadostProxy{" +
                "zadost=" + zadost +
                '}';
    }
}
