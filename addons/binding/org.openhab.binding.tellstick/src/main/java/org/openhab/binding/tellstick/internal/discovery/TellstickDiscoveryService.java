/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tellstick.internal.discovery;

import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.tellstick.TellstickBindingConstants;
import org.openhab.binding.tellstick.handler.TelldusBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.device.iface.Device;
import org.tellstick.device.iface.DimmableDevice;
import org.tellstick.device.iface.SwitchableDevice;
import org.tellstick.device.iface.TellstickEvent;

/**
 * The {@link TellstickDiscoveryService} class is used to discover MAX! Cube
 * devices that are connected to the Lan gateway.
 *
 * @author Marcel Verpaalen - Initial contribution
 */
public class TellstickDiscoveryService extends AbstractDiscoveryService
        implements org.openhab.binding.tellstick.handler.DeviceStatusListener {

    public TellstickDiscoveryService(int timeout) throws IllegalArgumentException {
        super(timeout);
        // TODO Auto-generated constructor stub
    }

    private final static Logger logger = LoggerFactory.getLogger(TellstickDiscoveryService.class);

    private TelldusBridgeHandler telldusBridgeHandler;

    public TellstickDiscoveryService(TelldusBridgeHandler telldusBridgeHandler) {
        super(TellstickBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS, 10, true);
        this.telldusBridgeHandler = telldusBridgeHandler;
    }

    public void activate() {
        telldusBridgeHandler.registerDeviceStatusListener(this);
    }

    @Override
    public void deactivate() {
        telldusBridgeHandler.unregisterDeviceStatusListener(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return TellstickBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS;
    }

    // @Override
    @Override
    public void onDeviceAdded(Bridge bridge, Device device) {
        logger.info("Adding new TellstickDevice! {} with id '{}' to smarthome inbox", device.getDeviceType(),
                device.getId());
        ThingUID thingUID = getThingUID(bridge, device);
        if (thingUID != null) {
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                    .withProperty(TellstickBindingConstants.DEVICE_ID, device.getUUId()).withBridge(bridge.getUID())
                    .withLabel(device.getDeviceType() + ": " + device.getName()).build();
            thingDiscovered(discoveryResult);

        } else {
            logger.warn("Discovered Tellstick! device is unsupported: type '{}' with id '{}'", device.getDeviceType(),
                    device.getId());
        }
    }

    @Override
    protected void startScan() {
        telldusBridgeHandler.rescanTelldusDevices();
    }

    @Override
    public void onDeviceStateChanged(Bridge bridge, Device device, TellstickEvent event) {
        // this can be ignored here
    }

    @Override
    public void onDeviceRemoved(Bridge bridge, Device device) {
        ThingUID thingUID = getThingUID(bridge, device);
        if (thingUID != null) {
            thingRemoved(thingUID);
        } else {
            logger.warn("Removed Tellstick! device is unsupported: type '{}' with id '{}'", device.getDeviceType(),
                    device.getId());
        }
    }

    private ThingUID getThingUID(Bridge bridge, Device device) {
        ThingUID thingUID = null;
        switch (device.getDeviceType()) {
            case SENSOR:
                thingUID = new ThingUID(TellstickBindingConstants.SENSOR_THING_TYPE, bridge.getUID(), device.getUUId());
                break;
            case DEVICE:
                if (device instanceof DimmableDevice) {
                    thingUID = new ThingUID(TellstickBindingConstants.DIMMER_THING_TYPE, bridge.getUID(),
                            device.getUUId());
                } else if (device instanceof SwitchableDevice) {
                    thingUID = new ThingUID(TellstickBindingConstants.SWITCH_THING_TYPE, bridge.getUID(),
                            device.getUUId());
                }
                break;
            default:
                break;
        }
        return thingUID;
    }
}
