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

package btrplace.solver.choco;

import btrplace.solver.choco.chocoUtil.AliasedCumulatives;
import choco.cp.solver.CPSolver;
import choco.kernel.solver.variables.integer.IntDomainVar;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder to create constraints where slices have to be placed on nodes
 * with regards to the slice and the nodes capacity.
 * <p/>
 * It differs from {@link btrplace.solver.choco.SliceSchedulerBuilder} as
 * a resource may in fact be an alias to another one. This allows
 * to create a fake resource that aggregate the capacity of each of
 * the aliased resources.
 *
 * @author Fabien Hermenier
 */
public class AliasedCumulativesBuilder extends SchedulingConstraintBuilder {

    private TIntArrayList capacities;

    private List<int[]> aliases;

    /**
     * Make a new builder.
     *
     * @param p the associated problem
     */
    public AliasedCumulativesBuilder(ReconfigurationProblem p) {
        super(p);
        capacities = new TIntArrayList();
        aliases = new ArrayList<>();

    }

    /**
     * Add a constraint
     *
     * @param capas the cumulated capacity of the aliased resources
     * @param cUse  the usage of each of the c-slices
     * @param dUse  the usage of each of the d-slices
     * @param alias the resource identifiers that compose the alias
     */
    public void add(int capas, int[] cUse, IntDomainVar[] dUse, int[] alias) {
        capacities.add(capas);
        cUsages.add(cUse);
        dUsages.add(dUse);
        aliases.add(alias);
    }

    /**
     * Get the generated constraints.
     *
     * @return a list of constraint that may be empty.
     */
    public List<AliasedCumulatives> getConstraints() {
        CPSolver s = rp.getSolver();
        List<AliasedCumulatives> cstrs = new ArrayList<>();


        for (int i = 0; i < aliases.size(); i++) {
            int capa = capacities.get(i);
            int[] alias = aliases.get(i);
            int[] cUse = cUsages.get(i);
            int[] dUses = new int[dUsages.get(i).length];
            for (IntDomainVar dUseDim : dUsages.get(i)) {
                dUses[i++] = dUseDim.getInf();
            }
            cstrs.add(new AliasedCumulatives(s.getEnvironment(),
                    alias,
                    new int[]{capa},
                    cHosters, new int[][]{cUse}, cEnds,
                    dHosters, new int[][]{dUses}, dStarts,
                    associations));

        }
        return cstrs;
    }
}
