/**
 * Copyright (c)2016 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos.discovery;

import static org.openhab.binding.heos.HeosBindingConstants.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.heos.handler.DeviceStatusListener;
import org.openhab.binding.heos.handler.HeosBridgeHandler;
import org.openhab.binding.heos.internal.messages.HeosPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HeosDiscoveryService} class is used to discover Tellstick
 * devices that are connected to the Lan gateway.
 *
 * @author Jarle Hjortland - Initial contribution
 */
public class HeosDiscoveryService extends AbstractDiscoveryService implements DeviceStatusListener {
    private static final long DEFAULT_TTL = 60 * 60; // 1 Hour

    public HeosDiscoveryService(int timeout) throws IllegalArgumentException {
        super(timeout);
        // TODO Auto-generated constructor stub
    }

    private final static Logger logger = LoggerFactory.getLogger(HeosDiscoveryService.class);

    private List<HeosBridgeHandler> heosBridgeHandlers = new Vector<HeosBridgeHandler>();

    public HeosDiscoveryService(HeosBridgeHandler HeosBridgeHandler) {
        super(Collections.singleton(HEOSBRIDGE_THING_TYPE_UID), 10, true);
        this.heosBridgeHandlers.add(HeosBridgeHandler);
    }

    public void activate() {
        for (HeosBridgeHandler heosBridgeHandler : heosBridgeHandlers) {
            heosBridgeHandler.registerDeviceStatusListener(this);
        }
    }

    @Override
    public void deactivate() {
        for (HeosBridgeHandler heosBridgeHandler : heosBridgeHandlers) {
            heosBridgeHandler.unregisterDeviceStatusListener(this);
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return Collections.singleton(HEOSBRIDGE_THING_TYPE_UID);
    }

    // @Override
    @Override
    public void onDeviceAdded(Bridge bridge, HeosPlayer device) {
        logger.info("Adding new Player! {} with id '{}' to smarthome inbox", device.getModel(), device.getPId());
        ThingUID thingUID = getThingUID(bridge, device);
        if (thingUID != null) {
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getUID())
                    .withTTL(DEFAULT_TTL).withProperty(PLAYER_PID, device.getPId())
                    .withProperty(PLAYER_MODEL, device.getModel())
                    .withLabel(device.getModel() + ": " + device.getName()).build();
            thingDiscovered(discoveryResult);

        } else {
            logger.warn("Discovered Tellstick! device is unsupported: type '{}' with id '{}'", device.getModel(),
                    device.getPId());
        }
    }

    @Override
    protected void startScan() {
        for (HeosBridgeHandler heosBridgeHandler : heosBridgeHandlers) {
            heosBridgeHandler.rescanHeosPlayers();
        }
    }

    @Override
    public void onDeviceRemoved(Bridge bridge, HeosPlayer device) {
        ThingUID thingUID = getThingUID(bridge, device);
        if (thingUID != null) {
            thingRemoved(thingUID);
        } else {
            logger.warn("Removed Heos ! device is unsupported: type '{}' with id '{}'", device.getModel(),
                    device.getPId());
        }
    }

    private ThingUID getThingUID(Bridge bridge, HeosPlayer device) {
        ThingUID thingUID = new ThingUID(HEOSPLAYER_THING_TYPE_UID, bridge.getUID(), device.getPId());
        return thingUID;
    }

    public void addBridgeHandler(HeosBridgeHandler tellstickBridgeHandler) {
        heosBridgeHandlers.add(tellstickBridgeHandler);
        tellstickBridgeHandler.registerDeviceStatusListener(this);
    }

    @Override
    public void onDeviceStateChanged(Bridge bridge, HeosPlayer device) {
        // TODO Auto-generated method stub

    }
}
