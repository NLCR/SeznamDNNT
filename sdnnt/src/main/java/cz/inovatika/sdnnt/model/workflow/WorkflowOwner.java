package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import java.util.Date;

/**
 * Nositel workflow; zadost nebo dokument
 */
public interface WorkflowOwner {

    /**
     * Aktualni stav pro prepnuti; u zadosti je to desired stav u dokumentu je standardni stav
     * @return
     */
    public CuratorItemState getWorkflowState();

    public PublicItemState getCurrentPublicState();

    public Date getWorkflowDate();

    // prepnuti stavu
    public void switchWorkflowState(CuratorItemState itm, String license, boolean changingLicenseState, Period period, String originator, String user, String poznamka);

    public boolean isSwitchToNextStatePossible(Period period);

    public void setWorkflowDate(Date date);

    public void setPeriodBetweenStates(Period period);

    public void setLicense(String l);

    public String getLicense();

}
