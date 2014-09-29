package btrplace.solver.choco.constraint.mttr;

import btrplace.model.VM;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.Slice;
import btrplace.solver.choco.view.VectorPacking;
import memory.IStateInt;
import solver.exception.ContradictionException;
import solver.search.strategy.assignments.DecisionOperator;
import solver.search.strategy.decision.Decision;
import solver.search.strategy.decision.fast.FastDecision;
import solver.search.strategy.strategy.AbstractStrategy;
import solver.variables.IntVar;
import util.PoolManager;
import util.iterators.DisposableValueIterator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/*
 * Created on 22/09/14.
 *
 * @author Sophie Demassey
 */
public class WorstFitDecreasingStrategy extends AbstractStrategy<IntVar> {
    private ReconfigurationProblem rp;

    private IStateInt firstFreeVar;
    private VectorPacking packingCtr;
    // object recycling management
    PoolManager<FastDecision> decisionPool;
    private Integer[] sortedIndex;
    private int[] initHost;
    private Random rnd;
    private int[] tieBins;



    public WorstFitDecreasingStrategy(ReconfigurationProblem rp, VectorPacking packing) {
        super(packing.getPackingVars());
        this.rp = rp;
        this.firstFreeVar = vars[0].getSolver().getEnvironment().makeInt(0);
        this.packingCtr = packing;
        this.decisionPool = new PoolManager<>();
        this.sortedIndex = null;
        this.initHost = null;
        this.rnd = new Random();
        this.tieBins = new int[rp.getNodes().length];
    }

    @Override
    public void init() throws ContradictionException {
            sortItems();
    }

    @Override
    public Decision<IntVar> getDecision() {
        int idx = selectVariable();
        return (idx < 0) ? null : computeDecision(idx);
    }

    public Decision<IntVar> computeDecision(int varIdx) {
        int value = selectValue(varIdx);
        FastDecision d = decisionPool.getE();
        if (d == null) {
            d = new FastDecision(decisionPool);
        }
        d.set(vars[varIdx], value, DecisionOperator.int_eq);
        return d;
    }


    private int selectVariable() {
        int idx = firstFreeVar.get();
        if (!vars[sortedIndex[idx]].isInstantiated()) {
            return sortedIndex[idx];
        }
        for (idx++; idx < sortedIndex.length; idx++) {
            if (!vars[sortedIndex[idx]].isInstantiated()) {
                firstFreeVar.set(idx);
                return sortedIndex[idx];
            }
        }
        return -1;
    }
    private int selectValue(int idx) {
        assert !vars[idx].isInstantiated();
        int bin = initHost[idx];
        if (bin >= 0 && vars[idx].contains(bin)) {
            return bin;
        }
        int maxRest = -1;
        int nbTies = 0;
        DisposableValueIterator it = vars[idx].getValueIterator(true);
        try {
            while (it.hasNext()) {
                bin = it.next();
                int rest = packingCtr.getRest(bin);
                if (rest > maxRest) {
                    maxRest = rest;
                    nbTies = 1;
                    tieBins[0] = bin;
                } else if (rest == maxRest) {
                    tieBins[nbTies++] = bin;
                }
            }
        } finally {
            it.dispose();
        }
        assert nbTies > 0;
        return tieBins[rnd.nextInt(nbTies)];
    }


    private void sortItems() {
        initHost = new int[vars.length];
        sortedIndex = new Integer[vars.length];
        int idx = 0;
        for (VM vm : rp.getVMs()) {
            Slice slice = rp.getVMAction(vm).getDSlice();
            if (slice != null) {
                IntVar var = slice.getHoster();
                assert var == vars[idx];
                initHost[idx] = rp.getNode(rp.getSourceModel().getMapping().getVMLocation(vm));
                sortedIndex[idx] = idx;
                idx++;
            }
        }
        Arrays.sort(sortedIndex, new SizeItemComparator());

    }

    private class SizeItemComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            int diff = 0;
            for (int d = 0; d < packingCtr.getDimension() && diff == 0; d++) {
                diff = packingCtr.getSize(d,o2) - packingCtr.getSize(d,o1);
            }
            return diff;
        }
    }

}