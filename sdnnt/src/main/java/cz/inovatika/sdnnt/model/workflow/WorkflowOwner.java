package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;

import java.util.Date;

/**
 * Owner of the workflow. It could be:
 * <ul>
 *     <li>Zadost</li>
 *     <li>Document</li>
 *     <li>part of document</li>
 * </ul>
 * @see cz.inovatika.sdnnt.model.Zadost
 * @see cz.inovatika.sdnnt.indexer.models.MarcRecord
 * @see cz.inovatika.sdnnt.indexer.models.MarcRecord
 */
public interface WorkflowOwner {

    /**
     * Returns current curator workflow state
     * @return current curator states
     * @see CuratorItemState
     */
    public CuratorItemState getWorkflowState();

    /**
     * Returns date of change of curator state
     * @return Date of curator state
     * @see Date
     */
    public Date getWorkflowDate();

    /**
     * Returns public state
     * @return
     */
    public PublicItemState getPublicState();

    /**
     * Returns date of change of public state
     * @return
     */
    public Date getPublicStateDate();

    // implementace zmeny stavu pro vlastnika worfklow pri zmene (kuratorske nebo z planovace)
    public void switchWorkflowState(CuratorItemState itm, String license, boolean changingLicenseState, Period period, String originator, String user, String poznamka);

    // vraci priznak, zda jsou splneny podminky pro zmenu stavu
    public boolean isSwitchToNextStatePossible(Date date, Period period);

    /**
     * Sets date of change of curator state
     * @param date Date of change
     */
    public void setWorkflowDate(Date date);

    /**
     * Sets period between states
     * @param period Period
     * @see Period
     */
    public void setPeriodBetweenStates(Period period);

    /**
     * Sets license for workflow owner
     * @param l License
     */
    public void setLicense(String l);

    /**
     * Returns license
     * @return
     */
    public String getLicense();

}
