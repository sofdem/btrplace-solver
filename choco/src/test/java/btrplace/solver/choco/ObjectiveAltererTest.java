/*
 * Copyright (c) 2014 University Nice Sophia Antipolis
 *
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.solver.choco;

import btrplace.model.DefaultModel;
import btrplace.model.Model;
import btrplace.solver.SolverException;
import org.testng.Assert;
import org.testng.annotations.Test;
import solver.variables.IntVar;
import solver.variables.VF;

/**
 * Unit tests for {@link btrplace.solver.choco.ObjectiveAlterer}.
 *
 * @author Fabien Hermenier
 */
public class ObjectiveAltererTest {

    @Test
    public void testBasic() throws SolverException {
        Model mo = new DefaultModel();
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo).build();
        IntVar obj = VF.bounded("obj", 10, 1000, rp.getSolver());
        //rp.getSolver().setObjective(obj);
        ObjectiveAlterer oa = new ObjectiveAlterer() {
            @Override
            public int newBound(ReconfigurationProblem rp, int currentValue) {
                return currentValue * 2;
            }
        };
        Assert.assertEquals(oa.newBound(rp, 25), 50);
        Assert.assertEquals(oa.newBound(rp, 50), 100);
    }
}
