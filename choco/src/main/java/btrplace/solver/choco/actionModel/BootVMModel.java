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

package btrplace.solver.choco.actionModel;

import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.BootVM;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ReconfigurationProblem;
import btrplace.solver.choco.Slice;
import btrplace.solver.choco.SliceBuilder;
import choco.cp.solver.CPSolver;
import choco.cp.solver.variables.integer.IntDomainVarAddCste;
import choco.kernel.solver.variables.integer.IntDomainVar;


/**
 * Model an action that boot a VM in the ready state.
 * The model must provide an estimation of the action duration through a
 * {@link btrplace.solver.choco.durationEvaluator.ActionDurationEvaluator} accessible from
 * {@link btrplace.solver.choco.ReconfigurationProblem#getDurationEvaluators()} with the key {@code BootVM.class}
 * <p/>
 * If the reconfiguration problem has a solution, a {@link btrplace.plan.event.BootVM} action
 * is inserted into the resulting reconfiguration plan.
 *
 * @author Fabien Hermenier
 */
public class BootVMModel implements VMActionModel {

    private Slice dSlice;

    private IntDomainVar end;

    private IntDomainVar start;

    private IntDomainVar duration;

    private VM vm;

    private ReconfigurationProblem rp;

    private IntDomainVar state;

    /**
     * Make a new model.
     *
     * @param p the RP to use as a basis.
     * @param e the VM managed by the action
     * @throws SolverException if an error occurred
     */
    public BootVMModel(ReconfigurationProblem p, VM e) throws SolverException {
        vm = e;

        int d = p.getDurationEvaluators().evaluate(p.getSourceModel(), BootVM.class, e);
        this.rp = p;
        start = p.makeDuration(p.getEnd().getSup() - d, 0, "bootVM(", e, ").start");
        end = new IntDomainVarAddCste(p.getSolver(), p.makeVarLabel("bootVM(", e, ").end"), start, d);
        duration = p.makeDuration(d, d, "bootVM.duration(", e, ')');
        dSlice = new SliceBuilder(p, e, new StringBuilder("bootVM(").append(e).append(").dSlice").toString()).setStart(start)
                .setDuration(p.makeDuration(p.getEnd().getSup(), d, "bootVM(", e, ").dSlice_duration"))
                .build();
        CPSolver s = p.getSolver();
        s.post(s.leq(start, p.getEnd()));
        s.post(s.leq(duration, p.getEnd()));
        s.post(s.leq(end, p.getEnd()));

        state = s.makeConstantIntVar(1);
    }

    @Override
    public boolean insertActions(ReconfigurationPlan plan) {
        Node node = rp.getNode(dSlice.getHoster().getVal());
        BootVM a = new BootVM(vm, node, start.getVal(), end.getVal());
        plan.add(a);
        return true;
    }

    @Override
    public IntDomainVar getStart() {
        return start;
    }

    @Override
    public IntDomainVar getEnd() {
        return end;
    }

    @Override
    public IntDomainVar getDuration() {
        return duration;
    }

    @Override
    public Slice getCSlice() {
        return null;
    }

    @Override
    public Slice getDSlice() {
        return dSlice;
    }

    @Override
    public IntDomainVar getState() {
        return state;
    }

    @Override
    public VM getVM() {
        return vm;
    }

    @Override
    public void visit(ActionModelVisitor v) {
        v.visit(this);
    }


}
