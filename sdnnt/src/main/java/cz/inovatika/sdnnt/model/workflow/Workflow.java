package cz.inovatika.sdnnt.model.workflow;


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


    public abstract WorkflowState nextAlternativeState(String alternative);
    public abstract  boolean isAlternativeSwitchPossible(String alternative);

}
