/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.solver.choco.runner.staticPartitioning;

import btrplace.model.*;
import btrplace.model.constraint.MinMTTR;
import btrplace.model.constraint.Running;
import btrplace.model.constraint.SatConstraint;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.ChocoReconfigurationAlgorithmParams;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithmParams;
import btrplace.solver.choco.runner.InstanceResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link FixedSizePartitioning}.
 *
 * @author Fabien Hermenier
 */
public class FixedSizePartitioningTest {

    private static ChocoReconfigurationAlgorithmParams params = new DefaultChocoReconfigurationAlgorithmParams();

    @Test
    public void basicTesting() {
        FixedSizePartitioning f = new FixedSizePartitioning(1000);
        f.setWorkersCount(5);
        Assert.assertEquals(f.getWorkersCount(), 5);
        Assert.assertEquals(f.getSize(), 1000);
        ChocoReconfigurationAlgorithm cra = new DefaultChocoReconfigurationAlgorithm();
        cra.setInstanceSolver(f);
        Assert.assertEquals(cra.getInstanceSolver(), f);
        f.setSize(300);
        Assert.assertEquals(f.getSize(), 300);
        Assert.assertEquals(f.randomPickUp(), false);
    }

    private static Instance makeInstance() {
        Model mo = new DefaultModel();
        for (int i = 0; i < 13; i++) {
            Node n = mo.newNode();
            mo.getMapping().addOnlineNode(n);
            for (int j = 0; j < 3; j++) {
                VM v = mo.newVM();
                mo.getMapping().addRunningVM(v, n);
            }
        }
        for (int i = 0; i < 5; i++) {
            mo.getMapping().addReadyVM(mo.newVM());
        }
        return new Instance(mo, Collections.<SatConstraint>singleton(new Running(mo.getMapping().getAllVMs())), new MinMTTR());
    }

    @Test
    public void testLinearSplit() throws SolverException {
        Instance i = makeInstance();
        FixedSizePartitioning f = new FixedSizePartitioning(5);
        List<Instance> partitions = f.split(params, i);
        checkCorrectness(partitions);

        //Get a solution, the ready VMs must have been launched
        InstanceResult res = f.solve(params, i);
        Assert.assertEquals(res.getPlan().getSize(), 5);

    }

    @Test
    public void testRandomSplit() throws SolverException {
        Instance origin = makeInstance();
        FixedSizePartitioning f = new FixedSizePartitioning(5);
        f.randomPickUp(true);
        Assert.assertEquals(f.randomPickUp(), true);
        List<Instance> l1 = f.split(params, origin);
        Assert.assertEquals(l1.size(), 3);
        checkCorrectness(l1);

        List<Instance> l2 = f.split(params, origin);
        Assert.assertEquals(l2.size(), 3);
        checkCorrectness(l2);

/*      //Just check the list of nodes are different
        for (int i = 0; i < l1.size(); i++) {
            for (int j = 0; j < l2.size(); j++) {
                Assert.assertFalse(l1.get(i).getModel().getMapping().getAllNodes().equals(
                        l2.get(j).getModel().getMapping().getAllNodes()), "l1:" + l1.get(i).getModel().getMapping().getAllNodes()
                                                                        + " l2:" + l2.get(i).getModel().getMapping().getAllNodes());
            }
        }*/
        //Get a solution, the ready VMs must have been launched
        InstanceResult res = f.solve(params, origin);
        Assert.assertEquals(res.getPlan().getSize(), 5);

    }

    private void checkCorrectness(List<Instance> partitions) {
        //Number of elements are correct
        Assert.assertEquals(partitions.size(), 3);
        for (int i = 0; i < partitions.size(); i++) {
            Instance inst = partitions.get(i);
            if (i == 2) {
                Assert.assertEquals(inst.getModel().getMapping().getAllNodes().size(), 3);
                Assert.assertEquals(inst.getModel().getMapping().getRunningVMs().size(), 9);
            } else {
                Assert.assertEquals(inst.getModel().getMapping().getAllNodes().size(), 5);
                Assert.assertEquals(inst.getModel().getMapping().getRunningVMs().size(), 15);
            }
        }

        //Check the VMs are the nodes IDs are all registered to be substitution proof
        for (int i = 0; i < 39; i++) {
            if (i < 13) {
                Assert.assertTrue(partitions.get(0).getModel().contains(new Node(i)));
                Assert.assertTrue(partitions.get(1).getModel().contains(new Node(i)));
            }
            Assert.assertTrue(partitions.get(0).getModel().contains(new VM(i)));
            Assert.assertTrue(partitions.get(1).getModel().contains(new VM(i)));
        }
    }
}
