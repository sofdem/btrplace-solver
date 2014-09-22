package btrplace.solver.choco.constraint.mttr;

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

/*
 * Created on 22/09/14.
 *
 * @author Sophie Demassey
 */
public class WorstFitDecreasingStrategy extends AbstractStrategy<IntVar> {
    private IStateInt firstFreeVar;
    private VectorPacking packingCtr;
    // object recycling management
    PoolManager<FastDecision> decisionPool;


    public WorstFitDecreasingStrategy(VectorPacking packing) {
        super(packing.getSortedPackingVars());
        this.firstFreeVar = vars[0].getSolver().getEnvironment().makeInt(0);
        this.packingCtr = packing;
        this.decisionPool = new PoolManager<>();

    }

    @Override
    public void init() throws ContradictionException {}

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
        if (!vars[idx].isInstantiated()) {
            return idx;
        }
        for (idx++; idx < vars.length; idx++) {
            if (!vars[idx].isInstantiated()) {
                firstFreeVar.set(idx);
                return idx;
            }
        }
        return -1;
    }
    private int selectValue(int idx) {
        assert !vars[idx].isInstantiated();
        int bin = packingCtr.getInitHost(idx);
        if (bin >= 0 && vars[idx].contains(bin)) {
            return bin;
        }
        int maxBin = -1;
        int maxRest = -1;
        DisposableValueIterator it = vars[idx].getValueIterator(true);
        try {
            while (it.hasNext()) {
                bin = it.next();
                int rest = packingCtr.getRest(bin);
                if (rest > maxRest) {
                    maxBin = bin;
                    maxRest = rest;
                }
            }
        } finally {
            it.dispose();
        }
        assert maxBin >= 0;
        return maxBin;
    }
}