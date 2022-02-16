package cz.inovatika.sdnnt.model.workflow.document;

import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.workflow.ZadostTypNavrh;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class DocumentWorkflowFactoryTest {


    @Test
    public void testDocumentFactory() {
        //List<String> kuratorstav, List<String> stav, String license
        List<ZadostTypNavrh> zadostTypNavrhs = DocumentWorkflowFactory.canBePartOfZadost(Arrays.asList("PA"), Arrays.asList("PA"), null);
        Assert.assertNotNull(zadostTypNavrhs.size() == 3);
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VNL));
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VNZ));
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VN));

        zadostTypNavrhs = DocumentWorkflowFactory.canBePartOfZadost(Arrays.asList("A"), Arrays.asList("A"), null);
        Assert.assertNotNull(zadostTypNavrhs.size() == 3);
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VNL));
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VNZ));
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VN));

        zadostTypNavrhs = DocumentWorkflowFactory.canBePartOfZadost(Arrays.asList("A"), Arrays.asList("A"), License.dnnto.name());
        Assert.assertNotNull(zadostTypNavrhs.size() == 3);
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VNL));
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VNZ));
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VN));

        zadostTypNavrhs = DocumentWorkflowFactory.canBePartOfZadost(Arrays.asList("A"), Arrays.asList("A"), License.dnntt.name());
        Assert.assertNotNull(zadostTypNavrhs.size() == 1);
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VN));

        zadostTypNavrhs = DocumentWorkflowFactory.canBePartOfZadost(Arrays.asList("NL"), Arrays.asList("NL"), License.dnntt.name());
        Assert.assertNotNull(zadostTypNavrhs.size() == 1);
        Assert.assertNotNull(zadostTypNavrhs.contains(ZadostTypNavrh.VNZ));

    }

}
