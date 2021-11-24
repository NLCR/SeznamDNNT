package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ZadostWorkflowTest {

    @Test
    public void testNZNWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        zadost.setNavrh("NZN");
        zadost.setDatumZadani(new Date());
        //WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;

        List<String> expectedStates = new ArrayList<>(Arrays.asList("NPA","PA","A"));

        Workflow workflow = ZadostWorkflowFactory.create(zadost);

        while((state = workflow.nextState()) != null && !state.isFinalSate()) {

            state.switchState("mojeid", zadost.getUser(), zadost.getId());


            Assert.assertTrue(state.getCuratorState().name().equals(expectedStates.remove(0)));
            Assert.assertTrue(state.getLicense() == null || state.getLicense().name().equals("dnnto"));
        }

        Assert.assertTrue(state.isFinalSate());

        if (state.isFinalSate()) {


            state.switchState("mojeid", zadost.getUser(), zadost.getId());

            Assert.assertTrue(state.getCuratorState().name().equals(expectedStates.remove(0)));
            Assert.assertTrue(state.getLicense().name().equals("dnnto"));
        }
    }

    @Test
    public void testVNWorkflow() {
        System.out.println(" VN >> ");
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;
        VNWorkflow workflow = new VNWorkflow(owner);

        while((state = workflow.nextState()) != null && !state.isFinalSate()) {

            System.out.println("Chteny stav "+state.getCuratorState());
            System.out.println("Chtena licence: "+state.getLicense());
            System.out.println("Deadline pro licenci: "+state.getPeriod());

            System.out.println("Period: "+ state.getPeriod());
            System.out.println("Typ prechodu: "+state.getPeriod().getTransitionType());


            owner.switchWorkflowState(state.getCuratorState(),null ,false , state.getPeriod(), "mojeid", zadost.getUser(), zadost.getId());
            System.out.println("======== -------- ==========");
        }
        System.out.println("======== Konecny stav ==========");

        if (state.isFinalSate()) {

            System.out.println("Chteny stav "+zadost.getDesiredItemState());
            System.out.println("Chtena licence "+state.getLicense());
            System.out.println("Lhuta "+state.getPeriod());
            System.out.println("Typ prechodu: "+state.getPeriod().getTransitionType());
            System.out.println("Deadline "+zadost.getDeadline());
        }

    }

    @Test
    public void testVNNWorkflow() {
        System.out.println(" VNN >> ");
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;
        // send
        VNLWorkflow workflow = new VNLWorkflow(owner);


        while((state = workflow.nextState()) != null && !state.isFinalSate()) {

            System.out.println("Chteny stav "+state.getCuratorState());
            System.out.println("Chtena licence: "+state.getLicense());
            System.out.println("Deadline pro licenci: "+state.getPeriod());

            System.out.println("Period: "+ state.getPeriod());
            System.out.println("Typ prechodu: "+state.getPeriod().getTransitionType());

            //System.out.println(state.getPeriod().getTransitionType());

            owner.switchWorkflowState(state.getCuratorState(), state.getLicense().name(), true, state.getPeriod(), "mojeid", zadost.getUser(), zadost.getId());
            System.out.println("======== -------- ==========");
        }
        System.out.println("======== Konecny stav ==========");

        if (state.isFinalSate()) {
            System.out.println("Setting workflow owner state to "+state.getCuratorState());
            System.out.println("Setting licence to "+state.getLicense());
            System.out.println("Lhuta "+state.getPeriod());
            System.out.println("Typ prechodu: "+state.getPeriod().getTransitionType());

            System.out.println("Setting deadline "+state.getDate());
            owner.switchWorkflowState(state.getCuratorState(),null ,false , state.getPeriod(), "mojeid", zadost.getUser(), zadost.getId());
        }

    }

    @Test
    public void testVNZWorkflow() {
        System.out.println(" VNZ >> ");
        Zadost zadost = new Zadost("mojeid");
        WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;
        // send
        VNZWorkflow workflow = new VNZWorkflow(owner);


        while((state = workflow.nextState()) != null && !state.isFinalSate()) {

            System.out.println("Chteny stav "+state.getCuratorState());
            System.out.println("Chtena licence: "+state.getLicense());
            System.out.println("Deadline pro licenci: "+state.getPeriod());

            System.out.println("Period: "+ state.getPeriod());
            System.out.println("Typ prechodu: "+state.getPeriod().getTransitionType());

            //System.out.println(state.getPeriod().getTransitionType());

            owner.switchWorkflowState(state.getCuratorState(), null, false , state.getPeriod(),"mojeid" , zadost.getUser(), zadost.getId());
            System.out.println("======== -------- ==========");
        }
        System.out.println("======== Konecny stav ==========");

        if (state.isFinalSate()) {
            System.out.println("Setting workflow owner state to "+state.getCuratorState());
            System.out.println("Setting licence to "+state.getLicense());
            System.out.println("Lhuta "+state.getPeriod());
            System.out.println("Typ prechodu: "+state.getPeriod().getTransitionType());

            System.out.println("Setting deadline "+state.getDate());
            owner.switchWorkflowState(state.getCuratorState(), null, false, state.getPeriod(), "mojeid", zadost.getUser(), zadost.getId());
        }

    }

}
