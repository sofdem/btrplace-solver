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

package btrplace.model;

/**
 * A procedure to use on a set of contiguous elements that
 * belong to the same partition.
 *
 * @author Fabien Hermenier
 */
public interface IterateProcedure<E extends Element> {

    /**
     * The method to execute.
     *
     * @param index the splittable set to rely on
     * @param key   the partition key
     * @param from  the value lower bound
     * @param to    the value upper bound (exclusive)
     */
    boolean extract(SplittableElementSet<E> index, int key, int from, int to);
}
