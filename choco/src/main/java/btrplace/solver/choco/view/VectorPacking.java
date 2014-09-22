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

package btrplace.solver.choco.view;

import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.Slice;
import btrplace.solver.choco.extensions.pack.VectorPackingPropagator;
import solver.Cause;
import solver.Solver;
import solver.constraints.Constraint;
import solver.exception.ContradictionException;
import solver.variables.IntVar;

import java.util.*;

import static util.tools.ArrayUtils.linspace;
import static util.tools.ArrayUtils.sort;


/*
 * Created on 17/09/14.
 *
 * @author Sophie Demassey
 */
public class VectorPacking extends Packing {

    private ReconfigurationProblem rp;

    private List<IntVar[]> loads;

    private List<IntVar[]> bins;

    private List<IntVar[]> sizes;

    private List<String> names;

    private int dim;

    private IntVar[] sortedPackingVars;
    private int[][] sortedSizes;
    private int[] initHosts;
    private VectorPackingPropagator propag;

    /**
     * A new constraint.
     *
     * @param p the associated problem
     */
    public VectorPacking(ReconfigurationProblem p) {
        loads = new ArrayList<>();
        bins = new ArrayList<>();
        sizes = new ArrayList<>();
        names = new ArrayList<>();
        sortedPackingVars = null;
        sortedSizes = null;
        initHosts = null;
        propag = null;
        rp = p;
        dim = 0;
    }

    @Override
    public void addDim(String name, IntVar[] l, IntVar[] s, IntVar[] b) {
        this.loads.add(l);
        this.sizes.add(s);
        this.bins.add(b);
        this.names.add(name);
        this.dim++;
    }

    @Override
    public boolean beforeSolve(ReconfigurationProblem p) {
        Solver solver = rp.getSolver();
        int[][] aSizes = new int[dim][];
        IntVar[][] aLoads = new IntVar[dim][];
        String[] aNames = new String[dim];
        for (int d = 0; d < dim; d++) { // todo: for (int d : sortedDimensions)
            aLoads[d] = Arrays.copyOf(loads.get(d), loads.get(d).length);
            aNames[d] = names.get(d);
            if (sortedPackingVars == null) {
                assert bins.get(d).length == 0 || bins.get(d)[0].equals(bins.get(0)[0]);
                assert bins.get(d).length == sizes.get(d).length;
                aSizes[d] = new int[sizes.get(d).length];
                IntVar[] s = sizes.get(d);
                int x = 0;
                for (IntVar ss : s) {
                    aSizes[d][x++] = ss.getLB();
                    try {
                        ss.instantiateTo(ss.getLB(), Cause.Null);
                    } catch (ContradictionException ex) {
                        rp.getLogger().error("Unable post the vector packing constraint");
                        return false;
                    }
                }
            }
        }
        if (sortedPackingVars == null) {
            sortedPackingVars = bins.get(0);
            aSizes = sortedSizes;
        }
        if (!rp.getFutureRunningVMs().isEmpty()) {
            propag = new VectorPackingPropagator(aNames, aLoads, aSizes, sortedPackingVars, true, true);
            solver.post(new Constraint("VectorPacking", propag));
            //IntConstraintFactory.bin_packing(bins.get(0), iSizes[i], loads.get(i), 0));
        }
        return true;
    }

    @Override
    public boolean insertActions(ReconfigurationProblem pb, ReconfigurationPlan p) {
        return true;
    }

    @Override
    public boolean cloneVM(VM vm, VM clone) {
        return true;
    }



    /**
     * Builder associated to this constraint.
     */
    public static class Builder extends SolverViewBuilder {

        @Override
        public String getKey() {
            return Packing.VIEW_ID;
        }

        @Override
        public Packing build(ReconfigurationProblem p) {
            return new VectorPacking(p);
        }

        @Override
        public List<String> getDependencies() {
            return Collections.emptyList();
        }

    }

    public IntVar[] getSortedPackingVars() {
        sortItems();
        return sortedPackingVars;
    }

    public int[][] getSortedSizes() {
        assert sortedSizes != null;
        return sortedSizes;
    }

    public int getInitHost(int idx) {
        assert initHosts != null;
        return initHosts[idx];
    }

    private void sortItems() {
        assert bins.get(0).length == sizes.get(0).length;
        Integer[] sortedItems = new Integer[sortedPackingVars.length];
        int[] hosts = new int[sortedPackingVars.length];
        int idx = 0;
        for (VM vm : rp.getVMs()) {
            Slice slice = rp.getVMAction(vm).getDSlice();
            if (slice != null) {
                IntVar var = slice.getHoster();
                assert var == bins.get(0)[idx];
                Node host = rp.getSourceModel().getMapping().getVMLocation(vm);
                hosts[idx] = (host == null) ? -1 : host.id(); // todo: is it sure that host ids correspond to the load variable indices and to the bin variable values ???
                sortedItems[idx] = idx;
                idx++;
            }
        }
        Arrays.sort(sortedItems, new SizeItemComparator());

        sortedPackingVars = new IntVar[sortedPackingVars.length];
        sortedSizes = new int[dim][sortedPackingVars.length];
        initHosts = new int[sortedPackingVars.length];
        for (int i=0; i<sortedItems.length; i++) {
            initHosts[i] = hosts[sortedItems[i]];
            sortedPackingVars[i] = bins.get(0)[sortedItems[i]];
            for (int d = 0; d < dim; d++) {
                sortedSizes[d][i] = sizes.get(d)[sortedItems[i]].getLB();
            }
        }
    }

    private class SizeItemComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            int diff = 0;
            for (int d = 0; d < dim && diff == 0; d++) {
                diff = sizes.get(d)[o1].getLB() - sizes.get(d)[o2].getLB();
            }
            return diff;
        }
    }

    public int getRest(int bin) {
        return propag.getRest(0, bin);
    }


}
