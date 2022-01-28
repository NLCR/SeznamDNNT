package cz.inovatika.sdnnt.model.workflow;


import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;

/**
 * Reprezentuje workflow
 */
public abstract class Workflow {

    // workflow owner; zadost nebo dokument
    protected WorkflowOwner owner;

    public Workflow(WorkflowOwner owner) {
        this.owner = owner;
    }

    /**
     * Prepnuti do dalsiho stavu
     * @return
     */
    public abstract WorkflowState nextState();

    /**
     * Vraci true pokud muzeme workflow povazovat za uzavrene / dostal se do konencneho stavu
     * @return
     */
    public abstract boolean isClosableState();



    public abstract  boolean isSwitchPossible();

    /**
     * Vlastnik workflow, muze byt bud zadost nebo dokument
     * @return
     */
    public WorkflowOwner getOwner() {
        return this.owner;
    }


    /**
     * Pokud ma alternativni prechod, umozni ho
     * @param alternative Nazev alternativniho prechodu
     * @return
     */
    public abstract WorkflowState nextAlternativeState(String alternative);

    /**
     * Vraci true, pokud existuje alternativni prechod
     * @param alternative
     * @return
     */
    public abstract  boolean isAlternativeSwitchPossible(String alternative);

    /**
     * Vytvari jmeno prechodu, uklada se do historie
     * @param currentState Jmeno prechodu
     * @param currentLicense Jmeno licence
     * @return Formatovane jmeno prechodu
     */
    public String createTransitionName(String currentState, String currentLicense) {

        String currentStateName = currentState != null ? currentState : "_";
        String currentLicenseName = currentLicense != null ? currentLicense : "_";

        return String.format("(%s,%s)", currentStateName, currentLicenseName);
    }
}
