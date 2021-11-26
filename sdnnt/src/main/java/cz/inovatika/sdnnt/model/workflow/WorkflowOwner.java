package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import java.util.Date;

/**
 * Nositel workflow; zadost nebo dokument
 */
public interface WorkflowOwner {

    // Workflow stav
    public CuratorItemState getWorkflowState();
    // datum prepnuti workflow stavu
    public Date getWorkflowDate();

    // Verejny stav
    public PublicItemState getPublicState();
    // datum prepnuti verejneho stavu
    public Date getPublicStateDate();


    public void switchWorkflowState(CuratorItemState itm, String license, boolean changingLicenseState, Period period, String originator, String user, String poznamka);

    public boolean isSwitchToNextStatePossible(Date date, Period period);

    public void setWorkflowDate(Date date);

    public void setPeriodBetweenStates(Period period);

    public void setLicense(String l);

    public String getLicense();

}
