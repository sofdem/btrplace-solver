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

import btrplace.model.VM;
import btrplace.plan.ReconfigurationPlan;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.extensions.pack.VectorPackingPropagator;
import solver.Cause;
import solver.Solver;
import solver.constraints.Constraint;
import solver.exception.ContradictionException;
import solver.variables.IntVar;

import java.util.*;


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
        for (int d = 0; d < dim; d++) {
            aLoads[d] = Arrays.copyOf(loads.get(d), loads.get(d).length);
            aNames[d] = names.get(d);
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

        if (!rp.getFutureRunningVMs().isEmpty()) {
            propag = new VectorPackingPropagator(aNames, aLoads, aSizes, bins.get(0), true, true);
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

    public int getSize(int dim, int vm) {
        return sizes.get(dim)[vm].getLB();
    }

    public int getRest(int bin) {
        return propag.getRest(0, bin);
    }

    public int getDimension() {
        return dim;
    }

    public IntVar[] getPackingVars() {
        return bins.get(0);
    }


}
