/**
 * Copyright (c)2016 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tellstick.internal.discovery;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.smarthome.config.discovery.DiscoveryListener;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.junit.Test;

public class TellstickBridgeDiscoveryTest {

    @Test
    public void testStartScan() {
        TellstickBridgeDiscovery bridgeDiscover = new TellstickBridgeDiscovery();
        final List<DiscoveryResult> res = new ArrayList<DiscoveryResult>();
        bridgeDiscover.addDiscoveryListener(new DiscoveryListener() {

            @Override
            public void thingRemoved(DiscoveryService source, ThingUID thingUID) {
            }

            @Override
            public void thingDiscovered(DiscoveryService source, DiscoveryResult result) {
                res.add(result);
            }

            @Override
            public Collection<ThingUID> removeOlderResults(DiscoveryService source, long timestamp,
                    Collection<ThingTypeUID> thingTypeUIDs) {
                return null;
            }
        });
        bridgeDiscover.startScan();
        assertEquals("Wrong number of controllers", 0, res.size());
    }

}
