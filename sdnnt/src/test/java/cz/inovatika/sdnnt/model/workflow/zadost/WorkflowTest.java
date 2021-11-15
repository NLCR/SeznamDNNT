package cz.inovatika.sdnnt.model.workflow.zadost;

import cz.inovatika.sdnnt.model.Period;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.WorkflowOwner;
import cz.inovatika.sdnnt.model.workflow.WorkflowState;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class WorkflowTest {

    @Test
    public void testNZNWorkflow() {
        Zadost zadost = new Zadost("mojeid");
        zadost.setNavrh("NZN");
        zadost.setDatumZadani(new Date());
        //WorkflowOwner owner = new ZadostProxy(zadost);
        WorkflowState state = null;

        List<String> expectedStates = new ArrayList<>(Arrays.asList("NPA","PA","A"));
        List<Period> expectedPeriods = new ArrayList<>(Arrays.asList(Period.period_0,Period.period_1,null));
        //List<Boolean> expectedChangingLicensesFlag = new ArrayList<>(Arrays.asList(Period.period_0,Period.period_1,null));


        AbstractZadostWorkflow workflow = AbstractZadostWorkflow.create(zadost);

        while((state = workflow.nextState()) != null && !state.isFinalSate()) {

            state.applyState();

            Assert.assertTrue(state.getCuratorState().name().equals(expectedStates.remove(0)));
            Assert.assertTrue(state.getLicense().name().equals("dnnto"));
            Assert.assertTrue(state.getPeriod().equals(expectedPeriods.remove(0)));
        }

        Assert.assertTrue(state.isFinalSate());

        if (state.isFinalSate()) {
            state.applyState();

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
            System.out.println("Kdo prepina: "+state.getPeriod().getDeadlineType());

            //System.out.println(state.getPeriod().getDeadlineType());

            owner.setWorkflowState(state.getCuratorState());
            System.out.println("======== -------- ==========");
        }
        System.out.println("======== Konecny stav ==========");

        if (state.isFinalSate()) {

            System.out.println("Chteny stav "+zadost.getDesiredItemState());
            System.out.println("Chtena licence "+state.getLicense());
            System.out.println("Lhuta "+state.getPeriod());
            System.out.println("Kdo prepina: "+state.getPeriod().getDeadlineType());
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
            System.out.println("Kdo prepina: "+state.getPeriod().getDeadlineType());

            //System.out.println(state.getPeriod().getDeadlineType());

            owner.setWorkflowState(state.getCuratorState());
            System.out.println("======== -------- ==========");
        }
        System.out.println("======== Konecny stav ==========");

        if (state.isFinalSate()) {
            System.out.println("Setting workflow owner state to "+state.getCuratorState());
            System.out.println("Setting licence to "+state.getLicense());
            System.out.println("Lhuta "+state.getPeriod());
            System.out.println("Kdo prepina: "+state.getPeriod().getDeadlineType());

            System.out.println("Setting deadline "+state.getDate());
        }

    }

    @Test
    public void testVNZWorkflow() {
        System.out.println(" VNN >> ");
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
            System.out.println("Kdo prepina: "+state.getPeriod().getDeadlineType());

            //System.out.println(state.getPeriod().getDeadlineType());

            owner.setWorkflowState(state.getCuratorState());
            System.out.println("======== -------- ==========");
        }
        System.out.println("======== Konecny stav ==========");

        if (state.isFinalSate()) {
            System.out.println("Setting workflow owner state to "+state.getCuratorState());
            System.out.println("Setting licence to "+state.getLicense());
            System.out.println("Lhuta "+state.getPeriod());
            System.out.println("Kdo prepina: "+state.getPeriod().getDeadlineType());

            System.out.println("Setting deadline "+state.getDate());
        }

    }

}
