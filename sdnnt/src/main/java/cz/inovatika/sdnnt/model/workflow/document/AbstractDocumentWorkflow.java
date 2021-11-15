package cz.inovatika.sdnnt.model.workflow.document;

import cz.inovatika.sdnnt.model.workflow.Workflow;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;

public abstract class AbstractDocumentWorkflow extends Workflow {

    public AbstractDocumentWorkflow(WorkflowOwner owner) {
        super(owner);
    }
}
