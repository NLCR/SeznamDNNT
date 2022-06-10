package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.*;
import cz.inovatika.sdnnt.model.workflow.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class ZadostWorkflowTest {

    @Test
    public void testNZNWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        zadost.setNavrh("NZN");
        zadost.setDatumZadani(new Date());
        WorkflowState state = null;
        List<String> expectedStates = new ArrayList<>(Arrays.asList("NPA","PA","A"));
        Workflow workflow = ZadostWorkflowFactory.create(zadost);
        while((state = workflow.nextState()) != null && !state.isFinalSate()) {
            state.switchState("mojeid", zadost.getUser(), zadost.getId(), null);
            Assert.assertTrue(state.getCuratorState().name().equals(expectedStates.remove(0)));
            Assert.assertTrue(state.getLicense() == null || state.getLicense().name().equals("dnnto"));
        }
        Assert.assertTrue(state.isFinalSate());
        if (state.isFinalSate()) {
            state.switchState("mojeid", zadost.getUser(), zadost.getId(), null);
            Assert.assertTrue(state.getCuratorState().name().equals(expectedStates.remove(0)));
            Assert.assertTrue(state.getLicense().name().equals("dnnto"));
        }
    }

    @Test
    public void testVNWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;
        VNWorkflow workflow = new VNWorkflow(owner);
        while((state = workflow.nextState()) != null && !state.isFinalSate()) {
            Assert.fail("Should't be here");
        }
        if (state.isFinalSate()) {
            Assert.assertTrue(state.getCuratorState().equals(CuratorItemState.N));
            Assert.assertNull(state.getLicense());
        }
    }

    @Test
    public void testVNLWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;
        VNLWorkflow workflow = new VNLWorkflow(owner);
        List<String> expectedStates = new ArrayList<>(Arrays.asList("NL", "NLX", "A"));

        Map<String, String> map = new HashMap<>();
        map.put("NL", "dnntt");
        map.put("A", "dnnto");

        while((state = workflow.nextState()) != null && !state.isFinalSate()) {
            String remove = expectedStates.remove(0);
            Assert.assertEquals(CuratorItemState.valueOf(remove), state.getCuratorState());
            if (map.containsKey(remove)) {
                Assert.assertEquals(state.getLicense(), License.valueOf(map.get(remove)));
            }
            owner.switchWorkflowState(null, state.getCuratorState(), state.getLicense().name(),  true, state.getPeriod(), "mojeid", zadost.getUser(), zadost.getId());
        }

        if (state.isFinalSate()) {
            String remove = expectedStates.remove(0);
            Assert.assertEquals(CuratorItemState.valueOf(remove), state.getCuratorState());
            if (map.containsKey(remove)) {
                Assert.assertEquals(state.getLicense(), License.valueOf(map.get(remove)));
            }
        }
    }

    @Test
    public void testVNZWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;
        // send
        VNZWorkflow workflow = new VNZWorkflow(owner);
        while((state = workflow.nextState()) != null && !state.isFinalSate()) {
            Assert.fail("Cannot be here");
            owner.switchWorkflowState(null, state.getCuratorState(), null ,  false,state.getPeriod() , "mojeid", zadost.getUser(), zadost.getId());
        }

        if (state.isFinalSate()) {
            Assert.assertEquals(state.getCuratorState(), CuratorItemState.A);
            Assert.assertEquals(state.getLicense(), License.dnntt);
            Assert.assertTrue(state.getPeriod().equals(Period.debug_vnl_0_5wd) || state.getPeriod().equals(Period.period_vln_0_5wd));
        }
    }

    @Test
    public void testPXWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;

        PXWorkflow workflow = new PXWorkflow(owner);
        while((state = workflow.nextState()) != null && !state.isFinalSate()) {
            Assert.fail("Cannot be here");
            owner.switchWorkflowState(null, state.getCuratorState(), null ,  false,state.getPeriod() , "mojeid", zadost.getUser(), zadost.getId());
        }

        if (state.isFinalSate()) {
            Assert.assertEquals(state.getCuratorState(), CuratorItemState.X);
            Assert.assertTrue(state.getLicense() == null);
        }
    }
    
    @Test
    public void testDXWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;
        // send
        DXWorkflow workflow = new DXWorkflow(owner);
        while((state = workflow.nextState()) != null && !state.isFinalSate()) {
            Assert.fail("Cannot be here");
            owner.switchWorkflowState(null, state.getCuratorState(), null ,  false,state.getPeriod() , "mojeid", zadost.getUser(), zadost.getId());
        }
        if (state.isFinalSate()) {
            System.out.println(state.getCuratorState());
            Assert.assertEquals(state.getCuratorState(), CuratorItemState.D);
            Assert.assertTrue(state.getLicense() == null);
        }
    }
    
}
