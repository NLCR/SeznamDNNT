package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.document.DocumentProxy;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateProxy;
import cz.inovatika.sdnnt.model.workflow.zadost.ZadostProxy;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrInputDocument;

/**
 * Vlastnik workflow - objekt nad kterym je provazen prechod mezi stavy
 * Muze byt 
 * <ul>
 *     <li>Zadost</li>
 *     <li>Document</li>
 *     <li>Document, List&lt;Document&gt;</li>
 *     <li>part of document</li>
 * </ul>
 * 
 * @see DocumentProxy
 * @see ZadostProxy
 * @see DuplicateProxy
 * 
 * @see cz.inovatika.sdnnt.model.Zadost
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

    public void switchWorkflowState(SwitchStateOptions options, CuratorItemState itm, String license, boolean changingLicenseState, Period period, String originator, String user, String poznamka);

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


    /**
     * Returns documents to save
     * @param options TODO
     * @return
     */
    public List<Pair<String, SolrInputDocument>> getStateToSave(SwitchStateOptions options);

    
    public boolean hasRejectableWorkload();
    
    
    public void rejectWorkflowState(String originator, String user, String poznamka);
}
