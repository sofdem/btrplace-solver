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

import btrplace.model.VM;
import choco.kernel.solver.variables.integer.IntDomainVar;


/**
 * Model a period where an element is hosted on a node.
 * {@link SliceBuilder} may be used to ease the creation of Slices.
 * <p/>
 * See {@link SliceUtils} to extract components of Slices.
 *
 * @author Fabien Hermenier
 * @see SliceUtils
 */
public class Slice {

    private IntDomainVar hoster;

    private IntDomainVar start;

    private IntDomainVar end;

    private IntDomainVar duration;

    private VM subject;

    /**
     * Make a new slice.
     *
     * @param s   the VM associated to the slice
     * @param st  the moment the slice starts
     * @param ed  the moment the slice ends
     * @param dur the slice duration
     * @param h   the slice host
     */
    public Slice(VM s, IntDomainVar st, IntDomainVar ed, IntDomainVar dur, IntDomainVar h) {

        this.start = st;
        this.end = ed;
        this.subject = s;
        this.hoster = h;
        this.duration = dur;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(subject).append("{from=").append(printValue(getStart()));
        b.append(", to=").append(printValue(getEnd()));
        b.append(", on=").append(printValue(getHoster()));
        return b.append('}').toString();
    }

    private String printValue(IntDomainVar v) {
        if (v.isInstantiated()) {
            return Integer.toString(v.getVal());
        }
        return new StringBuilder("[").append(v.getInf()).append(':').append(v.getSup()).append(']').toString();
    }

    /**
     * Get the moment the slice starts.
     *
     * @return a variable denoting the moment
     */
    public IntDomainVar getStart() {
        return start;
    }

    /**
     * Get the moment the slice ends.
     *
     * @return a variable denoting the moment
     */
    public IntDomainVar getEnd() {
        return end;
    }

    /**
     * Get the duration of the slice.
     *
     * @return a variable denoting the moment
     */
    public IntDomainVar getDuration() {
        return duration;
    }

    /**
     * Get the slice hoster.
     *
     * @return a variable indicating the node index
     */
    public IntDomainVar getHoster() {
        return hoster;
    }

    /**
     * Get the VM associated to the slice.
     *
     * @return the VM identifier
     */
    public VM getSubject() {
        return subject;
    }
}
