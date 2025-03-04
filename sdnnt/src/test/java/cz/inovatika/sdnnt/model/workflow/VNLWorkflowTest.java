/*
 * Copyright (C) 2025  Inovatika
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.inovatika.sdnnt.model.workflow;

import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class VNLWorkflowTest {

    @Test
    public void testWorkflow_1() {
        //A,PN(dnnto) -> A, PN(dnntt)
        WorkflowOwner owner = EasyMock.createMock(WorkflowOwner.class);
        EasyMock.expect(owner.getLicense()).andReturn("dnnto").anyTimes();
        EasyMock.expect(owner.getPublicState()).andReturn(PublicItemState.PA).anyTimes();
        EasyMock.expect(owner.getWorkflowState()).andReturn(CuratorItemState.PN).anyTimes();

        EasyMock.replay(owner);

        VNLWorkflow nznWorkflow = new VNLWorkflow(owner);
        Assert.assertTrue(nznWorkflow.isSwitchPossible(CuratorItemState.PN));
        Assert.assertTrue(nznWorkflow.isSwitchPossible());
        WorkflowState nextState = nznWorkflow.nextState();
        Assert.assertTrue(nextState.getCuratorState().equals(CuratorItemState.NL));
        Assert.assertTrue(nextState.getLicense() != null && nextState.getLicense().equals(License.dnntt));
    }


}
