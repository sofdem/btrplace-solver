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


import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import solver.search.strategy.selectors.IntValueSelector;
import solver.variables.IntVar;
import util.iterators.DisposableValueIterator;

import java.util.Random;


/**
 * A heuristic to place a VM on a server picked up randomly.
 * It is possible to force the VMs to stay on its current node
 * if it is possible.
 *
 * @author Fabien Hermenier
 */
public class RandomVMPlacement implements IntValueSelector {

    private TObjectIntHashMap<IntVar> initHost;
    private TIntHashSet[] ranks;
    private Random rnd;



    /**
     * Make a new heuristic.
     *
     * @param initHost the mapping between the D-slice variables and the initial location ids or null for full random
     */
    public RandomVMPlacement(TObjectIntHashMap<IntVar> initHost) {
        this(initHost, null);
    }

    /**
     * Make a new heuristic.
     *
     * @param initHost the mapping between the D-slice variables and the initial location ids
     * @param priorities  a list of favorites servers. Servers in rank i will be favored wrt. servers in rank i + 1
//     * @param stayFirst   {@code true} to force an already VM to stay on its current node if possible
     */
    public RandomVMPlacement(TObjectIntHashMap<IntVar> initHost, TIntHashSet[] priorities) {
        this.initHost = initHost;
        this.ranks = priorities;
        rnd = new Random();
    }

    /**
     * Random value but that consider the rank of nodes.
     * So values are picked up from the first rank possible.
     */
    private int randomWithRankedValues(IntVar x) {
        TIntArrayList[] values = new TIntArrayList[ranks.length];

        DisposableValueIterator ite = x.getValueIterator(true);
        try {
            while (ite.hasNext()) {
                int v = ite.next();
                for (int i = 0; i < ranks.length; i++) {
                    if (ranks[i].contains(v)) {
                        if (values[i] == null) {
                            values[i] = new TIntArrayList();
                        }
                        values[i].add(v);
                    }
                }
            }
        } finally {
            ite.dispose();
        }

        //We pick a random value in the first rank that is not empty (aka null here)
        for (TIntArrayList rank : values) {
            if (rank != null) {
                int v = rnd.nextInt(rank.size());
                return rank.get(v);
            }
        }
        return -1;
    }

    /**
     * Pick a random value inside the variable domain.
     */
    private int randomValue(IntVar x) {
        int i = rnd.nextInt(x.getDomainSize());
        DisposableValueIterator ite = x.getValueIterator(true);
        int pos = -1;
        try {
            while (i >= 0) {
                pos = ite.next();
                i--;
            }
        } finally {
            ite.dispose();
        }
        return pos;
    }

    @Override
    public int selectValue(IntVar x) {
        if (initHost != null) {
            int curPos = initHost.get(x);
            if (curPos != -1 && x.contains(curPos))
                return curPos;
        }
        return (ranks != null) ? randomWithRankedValues(x) : randomValue(x);
    }
}