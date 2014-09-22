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

package btrplace.solver.choco.constraint.mttr;

import btrplace.model.Mapping;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.solver.choco.ReconfigurationProblem;
import gnu.trove.map.hash.TObjectIntHashMap;
import memory.IEnvironment;
import memory.IStateInt;
import solver.search.strategy.selectors.VariableSelector;
import solver.variables.IntVar;

import java.util.Map;


/**
 * A variable selector that focuses on the VMs that will be running
 * necessarily on a new node as their current location is disallowed.
 *
 * @author Fabien Hermenier
 */
public class MovingVMs implements VariableSelector<IntVar> {

    /**
     * The demanding slices to consider.
     */
    private TObjectIntHashMap<IntVar> initHost;
    private IStateInt idx;

    /**
     * Make a new heuristic.
     * By default, the heuristic doesn't touch the scheduling constraints.
     *
     * @param env  the environment
     * @param initHost the mapping between the D-slice variables and the initial location ids
     */
    public MovingVMs(IEnvironment env, TObjectIntHashMap<IntVar> initHost) {
        this.initHost = initHost;
        this.idx = env.makeInt(0);
    }

    @Override
    public IntVar getVariable(IntVar[] scopes) {
        for (int i = idx.get(); i < scopes.length; i++) {
            IntVar h = scopes[i];
            if (!h.isInstantiated()) {
                assert initHost.contains(h);
                int nId = initHost.get(h);
                if (nId != -1 && !h.contains(nId)) {
                    idx.set(i);
                    return h;
                }
            }
            i++;
        }
        return null;
    }
}
