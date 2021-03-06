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

package btrplace.solver.choco.durationEvaluator;

import btrplace.model.Attributes;
import btrplace.model.DefaultModel;
import btrplace.model.Model;
import btrplace.model.VM;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ActionDurationFromOptionalAttribute}.
 *
 * @author Fabien Hermenier
 */
public class DurationFromOptionalAttributeTest {

    @Test
    public void test() {


        Model mo = new DefaultModel();
        Attributes attrs = mo.getAttributes();
        ActionDurationEvaluator<VM> parent = new ConstantActionDuration<>(15);
        VM vm1 = mo.newVM();
        ActionDurationFromOptionalAttribute<VM> dev = new ActionDurationFromOptionalAttribute<>("boot", parent);
        Assert.assertEquals(parent, dev.getParent());
        Assert.assertEquals("boot", dev.getAttributeKey());
        Assert.assertEquals(15, dev.evaluate(mo, vm1));

        attrs.put(vm1, "boot", 7);
        Assert.assertEquals(7, dev.evaluate(mo, vm1));

        parent = new ConstantActionDuration<>(2);
        dev.setParent(parent);
        attrs.clear();
        Assert.assertEquals(2, dev.evaluate(mo, vm1));
        Assert.assertEquals(parent, dev.getParent());
        Assert.assertFalse(dev.toString().contains("null"));

    }
}
