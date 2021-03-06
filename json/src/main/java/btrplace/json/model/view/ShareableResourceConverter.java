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

package btrplace.json.model.view;

import btrplace.json.JSONConverterException;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.view.ShareableResource;
import net.minidev.json.JSONObject;

import java.util.Set;


/**
 * Serialize/Un-serialize an {@link btrplace.model.view.ShareableResource}.
 *
 * @author Fabien Hermenier
 */
public class ShareableResourceConverter extends ModelViewConverter<ShareableResource> {

    /**
     * JSON label for default VM consumption.
     */
    public static final String DEFAULT_CONSUMPTION = "defConsumption";

    /**
     * JSON label for default node capacity.
     */
    public static final String DEFAULT_CAPACITY = "defCapacity";

    @Override
    public Class<ShareableResource> getSupportedView() {
        return ShareableResource.class;
    }

    @Override
    public String getJSONId() {
        return "shareableResource";
    }

    @Override
    public JSONObject toJSON(ShareableResource rc) {
        JSONObject o = new JSONObject();
        o.put("id", getJSONId());
        o.put(DEFAULT_CONSUMPTION, rc.getDefaultConsumption());
        o.put(DEFAULT_CAPACITY, rc.getDefaultCapacity());
        o.put("rcId", rc.getResourceIdentifier());

        Set<VM> elems = rc.getDefinedVMs();
        JSONObject values = new JSONObject();
        for (VM u : elems) {
            values.put(Integer.toString(u.id()), rc.getConsumption(u));
        }
        o.put("vms", values);

        Set<Node> nodes = rc.getDefinedNodes();
        values = new JSONObject();
        for (Node u : nodes) {
            values.put(Integer.toString(u.id()), rc.getCapacity(u));
        }
        o.put("nodes", values);

        return o;
    }

    @Override
    public ShareableResource fromJSON(JSONObject o) throws JSONConverterException {
        if (!o.containsKey("id") || !o.containsKey("vms") || !o.containsKey("nodes")
                || !o.containsKey("rcId") || !o.containsKey(DEFAULT_CONSUMPTION) || !o.containsKey(DEFAULT_CAPACITY)) {
            return null;
        }
        String id = o.get("id").toString();
        if (!id.equals(getJSONId())) {
            return null;
        }

        String rcId = o.get("rcId").toString();
        Object dc = o.get(DEFAULT_CONSUMPTION);
        if (!(dc instanceof Integer)) {
            throw new JSONConverterException("Integer expected for key '" + DEFAULT_CONSUMPTION + "' but got '" + dc.getClass().getName() + "'");
        }
        int defConsumption = (Integer) o.get(DEFAULT_CONSUMPTION);
        dc = o.get(DEFAULT_CAPACITY);
        if (!(dc instanceof Integer)) {
            throw new JSONConverterException("Integer expected for key '" + DEFAULT_CAPACITY + "' but got '" + dc.getClass().getName() + "'");
        }
        int defCapacity = (Integer) o.get(DEFAULT_CAPACITY);

        ShareableResource rc = new ShareableResource(rcId, defCapacity, defConsumption);

        parseVMs(rc, o.get("vms"));
        parseNodes(rc, o.get("nodes"));

        return rc;
    }

    private void parseVMs(ShareableResource rc, Object o) throws JSONConverterException {
        if (o != null) {
            try {
                JSONObject values = (JSONObject) o;
                for (String k : values.keySet()) {
                    VM u = getOrMakeVM(Integer.parseInt(k));
                    int v = Integer.parseInt(values.get(k).toString());
                    rc.setConsumption(u, v);
                }
            } catch (ClassCastException cc) {
                throw new JSONConverterException("Unable to read the VMs at key 'vms'. Expect a JSONObject but got a '" + o.getClass().getName() + "'", cc);
            }
        }
    }

    private void parseNodes(ShareableResource rc, Object o) throws JSONConverterException {
        if (o != null) {
            try {
                JSONObject values = (JSONObject) o;
                for (String k : values.keySet()) {
                    Node u = getOrMakeNode(Integer.parseInt(k));
                    int v = Integer.parseInt(values.get(k).toString());
                    rc.setCapacity(u, v);
                }
            } catch (ClassCastException cc) {
                throw new JSONConverterException("Unable to read the nodes at key 'nodes'. Expect a JSONObject but got a '" + o.getClass().getName() + "'", cc);
            }
        }
    }
}
