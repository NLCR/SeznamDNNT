package cz.inovatika.sdnnt.model.workflow;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;

public class NZNWorkflowTest {

    @Test
    public void testWorkflow_10() {
        //PA,PN -> A
        WorkflowOwner owner = EasyMock.createMock(WorkflowOwner.class);
        EasyMock.expect(owner.getLicense()).andReturn("dnnto").anyTimes();
        EasyMock.expect(owner.getPublicState()).andReturn(PublicItemState.PA).anyTimes();
        EasyMock.expect(owner.getWorkflowState()).andReturn(CuratorItemState.PN).anyTimes();
        
        EasyMock.replay(owner);
        
        NZNWorkflow nznWorkflow = new NZNWorkflow(owner);
        
        Assert.assertTrue(nznWorkflow.isSwitchPossible(CuratorItemState.A));
        Assert.assertFalse(nznWorkflow.isSwitchPossible(CuratorItemState.PA));
        
        WorkflowState nextState = nznWorkflow.nextState();
        Assert.assertNotNull(nextState.getPeriod());
        Assert.assertTrue(nextState.getCuratorState().equals(CuratorItemState.PN));
        Assert.assertTrue(nextState.getLicense().equals(License.dnnto));
    }

    
    @Test
    public void testWorkflow_1() {
        //PA,PX -> A
        WorkflowOwner owner = EasyMock.createMock(WorkflowOwner.class);
        EasyMock.expect(owner.getLicense()).andReturn("dnnto").anyTimes();
        EasyMock.expect(owner.getPublicState()).andReturn(PublicItemState.PA).anyTimes();
        EasyMock.expect(owner.getWorkflowState()).andReturn(CuratorItemState.PX).anyTimes();
        
        EasyMock.replay(owner);
        
        NZNWorkflow nznWorkflow = new NZNWorkflow(owner);

        Assert.assertTrue(nznWorkflow.isSwitchPossible(CuratorItemState.A));
        Assert.assertFalse(nznWorkflow.isSwitchPossible(CuratorItemState.PA));
        
        WorkflowState nextState = nznWorkflow.nextState();
        Assert.assertNotNull(nextState.getPeriod());
        Assert.assertTrue(nextState.getCuratorState().equals(CuratorItemState.PX));
        Assert.assertTrue(nextState.getLicense().equals(License.dnnto));
    }
    
    @Test
    public void testWorkflow_2() {
        //PA,DX -> A
        WorkflowOwner owner = EasyMock.createMock(WorkflowOwner.class);
        EasyMock.expect(owner.getLicense()).andReturn("dnnto").anyTimes();
        EasyMock.expect(owner.getPublicState()).andReturn(PublicItemState.PA).anyTimes();
        EasyMock.expect(owner.getWorkflowState()).andReturn(CuratorItemState.PX).anyTimes();
        
        EasyMock.replay(owner);
        
        NZNWorkflow nznWorkflow = new NZNWorkflow(owner);
        
        Assert.assertTrue(nznWorkflow.isSwitchPossible(CuratorItemState.A));
        Assert.assertFalse(nznWorkflow.isSwitchPossible(CuratorItemState.PA));
        
        WorkflowState nextState = nznWorkflow.nextState();
        Assert.assertNotNull(nextState.getPeriod());
        Assert.assertTrue(nextState.getCuratorState().equals(CuratorItemState.PX));
        Assert.assertTrue(nextState.getLicense().equals(License.dnnto));
    }

    @Test
    public void testWorkflow_3() {
        // NPA -> PA
        WorkflowOwner owner = EasyMock.createMock(WorkflowOwner.class);
        EasyMock.expect(owner.getLicense()).andReturn(null).anyTimes();
        EasyMock.expect(owner.getPublicState()).andReturn(PublicItemState.N).anyTimes();
        EasyMock.expect(owner.getWorkflowState()).andReturn(CuratorItemState.NPA).anyTimes();
        
        EasyMock.replay(owner);
        
        NZNWorkflow nznWorkflow = new NZNWorkflow(owner);
        Assert.assertTrue(nznWorkflow.isSwitchPossible(CuratorItemState.PA));
        Assert.assertFalse(nznWorkflow.isSwitchPossible(CuratorItemState.A));
        
        WorkflowState nextState = nznWorkflow.nextState();
        Assert.assertNotNull(nextState.getPeriod());
        Assert.assertTrue(nextState.getCuratorState().equals(CuratorItemState.PA));
    }

    @Test
    public void testWorkflow_4() {

        WorkflowOwner owner = EasyMock.createMock(WorkflowOwner.class);
        EasyMock.expect(owner.getLicense()).andReturn(null).anyTimes();
        EasyMock.expect(owner.getPublicState()).andReturn(null).anyTimes();
        EasyMock.expect(owner.getWorkflowState()).andReturn(CuratorItemState.NPA).anyTimes();
        
        EasyMock.replay(owner);
        
        NZNWorkflow nznWorkflow = new NZNWorkflow(owner);
        Assert.assertTrue(nznWorkflow.isSwitchPossible(CuratorItemState.PA));
        Assert.assertFalse(nznWorkflow.isSwitchPossible(CuratorItemState.A));
        
        WorkflowState nextState = nznWorkflow.nextState();
        Assert.assertNotNull(nextState.getPeriod());
        Assert.assertTrue(nextState.getCuratorState().equals(CuratorItemState.PA));
        
    }

    @Test
    public void testWorkflow_5() {

        WorkflowOwner owner = EasyMock.createMock(WorkflowOwner.class);
        EasyMock.expect(owner.getLicense()).andReturn(null).anyTimes();
        EasyMock.expect(owner.getPublicState()).andReturn(null).anyTimes();
        EasyMock.expect(owner.getWorkflowState()).andReturn(CuratorItemState.PX).anyTimes();
        
        EasyMock.replay(owner);
        
        NZNWorkflow nznWorkflow = new NZNWorkflow(owner);
        Assert.assertFalse(nznWorkflow.isSwitchPossible(CuratorItemState.PA));
        Assert.assertFalse(nznWorkflow.isSwitchPossible(CuratorItemState.A));
        
        WorkflowState nextState = nznWorkflow.nextState();
        //Assert.assertTrue(nextState == null);

    }

}
