package btrplace.solver.choco.constraint.mttr;

import solver.search.strategy.decision.Decision;
import solver.search.strategy.strategy.AbstractStrategy;
import solver.variables.IntVar;

/*
 * A mock strategy that invalidate the scheduling heuristic prior its use.
 * Created on 20/09/14.
 *
 * @author Sophie Demassey
 */
public class PreSchedulingStrategy extends AbstractStrategy<IntVar>
{
    private OnStableNodeFirst schedHeuristic;

    /**
     * Make a new heuristic.
     * By default, the heuristic doesn't touch the scheduling constraints.
     *
     * @param sched the scheduling heuristic to notify when the placement is invalidated
     */
    public PreSchedulingStrategy(OnStableNodeFirst sched) {
        super(new IntVar[]{});
        assert sched != null;
        this.schedHeuristic = sched;
    }

    @Override
    public void init() {}

    @Override
    public Decision<IntVar> getDecision() {
        schedHeuristic.invalidPlacement();
        return null;
    }
}
