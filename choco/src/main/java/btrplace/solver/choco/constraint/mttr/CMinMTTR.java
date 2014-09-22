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

import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.MinMTTR;
import btrplace.model.view.ShareableResource;
import btrplace.model.view.VMConsumptionComparator;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.Slice;
import btrplace.solver.choco.SliceUtils;
import btrplace.solver.choco.constraint.ChocoConstraintBuilder;
import btrplace.solver.choco.transition.Transition;
import btrplace.solver.choco.transition.TransitionUtils;
import btrplace.solver.choco.view.CShareableResource;
import btrplace.solver.choco.view.ChocoView;
import btrplace.solver.choco.view.Packing;
import btrplace.solver.choco.view.VectorPacking;
import gnu.trove.map.hash.TObjectIntHashMap;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.IntConstraintFactory;
import solver.search.limits.BacktrackCounter;
import solver.search.loop.monitors.SMF;
import solver.search.strategy.selectors.values.IntDomainMin;
import solver.search.strategy.selectors.variables.InputOrder;
import solver.search.strategy.strategy.AbstractStrategy;
import solver.search.strategy.strategy.IntStrategy;
import solver.variables.IntVar;
import solver.variables.VariableFactory;

import java.io.Serializable;
import java.util.*;

/**
 * An objective that minimizes the time to repair a non-viable model.
 *
 * @author Fabien Hermenier
 */
public class CMinMTTR implements btrplace.solver.choco.constraint.CObjective {

    private List<Constraint> costConstraints;

    private boolean costActivated = false;

    private ReconfigurationProblem rp;

    /**
     * Make a new objective.
     */
    public CMinMTTR() {
        costConstraints = new ArrayList<>();
    }

    @Override
    public boolean inject(ReconfigurationProblem p) throws SolverException {
        this.rp = p;
        costActivated = false;
        List<IntVar> mttrs = new ArrayList<>();
        for (Transition m : rp.getVMActions()) {
            mttrs.add(m.getEnd());
        }
        for (Transition m : rp.getNodeActions()) {
            mttrs.add(m.getEnd());
        }
        IntVar[] costs = mttrs.toArray(new IntVar[mttrs.size()]);
        IntVar cost = VariableFactory.bounded(rp.makeVarLabel("globalCost"), 0, Integer.MAX_VALUE / 100, rp.getSolver());

        Constraint costConstraint = IntConstraintFactory.sum(costs, cost);
        costConstraints.clear();
        costConstraints.add(costConstraint);

        rp.setObjective(true, cost);

        //We set a restart limit by default, this may be useful especially with very small infrastructure
        //as the risk of cyclic dependencies increase and their is no solution for the moment to detect cycle
        //in the scheduling part
        //Restart limit = 2 * number of VMs in the DC.
        if (rp.getVMs().length > 0) {
            SMF.geometrical(rp.getSolver(), 1, 1.5d, new BacktrackCounter(rp.getVMs().length * 2), Integer.MAX_VALUE);
        }

        // set the solver heuristics : placement, scheduling then cost
        injectHeuristic(cost);

        postCostConstraints();
        return true;
    }

    private void injectHeuristic(IntVar cost) throws SolverException {
        List<AbstractStrategy> strategies = new ArrayList<>();
        OnStableNodeFirst schedHeuristic = new OnStableNodeFirst(rp, this);

        // assign the D-Slice (placement) variables
        //injectPlacementHeuristic(strategies);
        injectWorstFitPackingHeuristic(strategies);

        // schedule the node actions
        if (rp.getNodeActions().length > 0) {
            strategies.add(new IntStrategy(TransitionUtils.getStarts(rp.getNodeActions()), new InputOrder<>(), new IntDomainMin()));
        }
        // void strategy that prepare scheduling once all VMs are placed
        strategies.add(new PreSchedulingStrategy(schedHeuristic));

        ///SCHEDULING PROBLEM
        MovementGraph gr = new MovementGraph(rp);
        strategies.add(new IntStrategy(SliceUtils.extractStarts(TransitionUtils.getDSlices(rp.getVMActions())), new StartOnLeafNodes(rp, gr), new IntDomainMin()));
        strategies.add(new IntStrategy(schedHeuristic.getScope(), schedHeuristic, new IntDomainMin()));

        strategies.add(new IntStrategy(new IntVar[]{rp.getEnd(), cost}, new InputOrder<>(), new IntDomainMin()));
        rp.getSolver().set(strategies.toArray(new AbstractStrategy[strategies.size()]));

    }

    private void injectPlacementHeuristic(List<AbstractStrategy> strategies) {
        List<IntVar> vmToMigrate = new LinkedList<>();
        List<IntVar> vmThatMayStay = new LinkedList<>();
        List<IntVar> vmOthers = new LinkedList<>();
        TObjectIntHashMap<IntVar> initHost = new TObjectIntHashMap<>(rp.getFutureRunningVMs().size(), 0.5f, -1);
        for (Iterator<VM> ite = rp.getManageableVMs().iterator(); ite.hasNext(); ) {
            VM vm = ite.next();
            Slice slice = rp.getVMAction(vm).getDSlice();
            if (slice != null) {
                IntVar var = slice.getHoster();
                Node host = rp.getSourceModel().getMapping().getVMLocation(vm);
                if (host != null) {
                    initHost.put(var, rp.getNode(host));
                    if (rp.getSourceModel().getMapping().isRunning(vm)) {
                        if (rp.getFutureRunningVMs().contains(vm) && !var.contains(rp.getNode(host))) {
                            vmToMigrate.add(var);
                            continue;
                        }
                        if (rp.getSourceModel().getMapping().isOnline(host)) {
                            vmThatMayStay.add(var);
                            continue;
                        }
                    }
                }
                vmOthers.add(var);
            }
        }
        vmToMigrate.addAll(vmThatMayStay);
        vmToMigrate.addAll(vmOthers);

        if (vmToMigrate.size() > 0) {
            IntVar[] scope = vmToMigrate.toArray(new IntVar[vmToMigrate.size()]);
            strategies.add(new IntStrategy(scope, new InputOrder<>(), new RandomVMPlacement(initHost)));
        }
    }


    private void injectWorstFitPackingHeuristic(List<AbstractStrategy> strategies) throws SolverException {

        ChocoView v = rp.getView(Packing.VIEW_ID);
        if (v == null) {
            throw new SolverException(rp.getSourceModel(), "View '" + Packing.VIEW_ID + "' is required but missing");
        }
        strategies.add(new WorstFitDecreasingStrategy((VectorPacking) v));
    }

/*
    private void injectPackingHeuristic(List<AbstractStrategy> strategies) {
        Set<VM> manageableVMs = rp.getManageableVMs();
        if (manageableVMs.size() == 0) {
            return;
        }

        CShareableResource viewRam = null;
        for (String vid : rp.getViews()) {
            if (vid.startsWith("ShareableResource")) {
                if (vid.startsWith("ShareableResource.ram")) {
                    viewRam = (CShareableResource) rp.getView(vid);
                    break;
                }
                viewRam = (CShareableResource) rp.getView(vid);
            }
        }
        if (viewRam == null) {
            injectPlacementHeuristic(strategies);
            return;
        }
        VMConsumptionComparator resComp = new VMConsumptionComparator(viewRam.getSourceResource(), false);
        for (String vid : rp.getViews()) {
            if (viewRam.getIdentifier() != vid && vid.startsWith("ShareableResource")) {
                resComp.append(((CShareableResource) rp.getView(vid)).getSourceResource(), false);
            }
            viewRam = (CShareableResource) rp.getView(vid);
        }

        List<IntVar> vms = new ArrayList<>(manageableVMs.size());
        TObjectIntHashMap<IntVar> vmIds = new TObjectIntHashMap<>(manageableVMs.size(), 0.5f, -1);
        TObjectIntHashMap<IntVar> initHost = new TObjectIntHashMap<>(rp.getFutureRunningVMs().size(), 0.5f, -1);
        for (Iterator<VM> ite = rp.getManageableVMs().iterator(); ite.hasNext(); ) {
            VM vm = ite.next();
            Slice slice = rp.getVMAction(vm).getDSlice();
            if (slice != null) {
                IntVar var = slice.getHoster();
                Node host = rp.getSourceModel().getMapping().getVMLocation(vm);
                if (host != null) {
                    initHost.put(var, rp.getNode(host));
                }
                vms.add(var);
                vmIds.put(var, vm.id());
            }
        }
        vms.sort(new VMVarConsumptionComparator(resComp, vmIds));
        IntVar[] scope = vms.toArray(new IntVar[vms.size()]);

        int j = 0;
        int[] capas = new int[viewRam.getVirtualUsage().length];
        for (IntVar v : view.getVirtualUsage()) {
            capas[j++] = v.getValue();
        }

        strategies.add(new IntStrategy(scope, new InputOrder<>(), new RandomVMPlacement(initHost)));
    }


    class VMVarConsumptionComparator  implements Comparator<IntVar>, Serializable {
        VMConsumptionComparator vmComp;
        TObjectIntHashMap<IntVar> vmIds;

        public VMVarConsumptionComparator(VMConsumptionComparator vmComp, TObjectIntHashMap<IntVar> vmIds) {
            this.vmComp = vmComp;
            this.vmIds = vmIds;
        }

        @Override
        public int compare(IntVar o1, IntVar o2) {
            return vmComp.compare(rp.getVM(vmIds.get(o1)), rp.getVM(vmIds.get(o2)));
        }
    }
*/

        @Override
    public Set<VM> getMisPlacedVMs(Model m) {
        return Collections.emptySet();
    }

    /**
     * Post the constraints related to the objective.
     */
    @Override
    public void postCostConstraints() {
        //TODO: Delay insertion
        if (!costActivated) {
            rp.getLogger().debug("Post the cost-oriented constraints");
            costActivated = true;
            Solver s = rp.getSolver();
            for (Constraint c : costConstraints) {
                s.post(c);
            }
            /*try {
                s.propagate();
            } catch (ContradictionException e) {
                s.setFeasible(ESat.FALSE);
                //s.setFeasible(false);
                s.post(IntConstraintFactory.FALSE(s));
            } */
        }
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends btrplace.model.constraint.Constraint> getKey() {
            return MinMTTR.class;
        }

        @Override
        public CMinMTTR build(btrplace.model.constraint.Constraint cstr) {
            return new CMinMTTR();
        }
    }
}
