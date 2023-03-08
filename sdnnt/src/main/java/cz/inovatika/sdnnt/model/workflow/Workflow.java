package cz.inovatika.sdnnt.model.workflow;


import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;

/**
 * This represents workflow
 */
public abstract class Workflow {
    
    // zadost nebo document
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


    /**
     * Vraci true, pokud je mozne se prepnout do dalsiho stavu <br/>
     * Ridi se pouze vlastnikem workflow, moznymi prechody
     * @return
     */
    public abstract  boolean isSwitchPossible();


    /**
     * Vraci true, pokud existuje prechod do pozadovaneho stavu
     * @param Cilovy stav prepnuti
     * @return
     */
    public abstract  boolean isSwitchPossible(CuratorItemState desiredState);

    
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
     * @param options TODO
     * @return
     */
    public abstract WorkflowState nextAlternativeState(String alternative, SwitchStateOptions options);

    /**
     * Vraci true, pokud existuje alternativni prechod
     * @param alternative
     * @return
     */
    public abstract  boolean isAlternativeSwitchPossible(String alternative);

    /**
     * Vraci true, pokud existuje alternativni prechod s cilovym stavem
     * @param alternative
     * @param desiredState
     * @return
     */
    public abstract  boolean isAlternativeSwitchPossible(String alternative,CuratorItemState desiredState);

    
    
    /**
     * Vraci true, pokud je workflow vztazeno k uzivatelske akci (zaradit, vyradit atd..) 
     * V pripade, ze je vysledkem procesu (volne dilo v digitalni knihovne, vyrazeni, duplicita, atd..) vraci false
     * @return
     */
    public abstract boolean userDefinedWorkflow();

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
