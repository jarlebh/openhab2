/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt.internal.discovery;

import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.alyt.devices.AlytDevice;
import org.openhab.binding.alyt.AlytBindingConstants;
import org.openhab.binding.alyt.internal.handler.AlytBridgeHandler;
import org.openhab.binding.alyt.internal.handler.DeviceStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AlytDeviceDiscoveryService} class is used to discover ALYT Cube
 * devices that are connected to the Lan gateway.
 * 
 * @author Marcel Verpaalen - Initial contribution
 */
public class AlytDeviceDiscoveryService extends AbstractDiscoveryService
		implements DeviceStatusListener {

	private final static Logger logger = LoggerFactory
			.getLogger(AlytDeviceDiscoveryService.class);

	private AlytBridgeHandler alytBridgeHandler;

	public AlytDeviceDiscoveryService(AlytBridgeHandler alytBridgeHandler) {
		super(AlytBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS, 10, true);
		this.alytBridgeHandler = alytBridgeHandler;
	}

	public void activate() {
		alytBridgeHandler.registerDeviceStatusListener(this);
	}

	public void deactivate() {
		alytBridgeHandler.unregisterDeviceStatusListener(this);
	}

	public Set<ThingTypeUID> getSupportedThingTypes() {
		return AlytBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS;
	}

	public void onDeviceAdded(Bridge bridge, AlytDevice device) {
		logger.trace(
				"Adding new Alyt Device! {} with id '{}' to smarthome inbox",
				device.getDeviceType(), device.getAlytID());
		ThingUID thingUID = null;
		switch (device.getDeviceType()) {

		case GENERIC_SWITCH:
		case WALL_PLUG:
			thingUID = new ThingUID(AlytBindingConstants.SWITCH_THING_TYPE,
					bridge.getUID(), device.getAlytID());
			break;
		default:
			break;
		}
		if (thingUID != null) {
			DiscoveryResult discoveryResult = DiscoveryResultBuilder
					.create(thingUID)
					.withProperty(AlytBindingConstants.ALYT_ID,
							device.getAlytID())
					.withBridge(bridge.getUID())
					.withLabel(
							device.getDeviceType().name() + ": "
									+ device.getDeviceName()).build();
			thingDiscovered(discoveryResult);
		} else {
			logger.info(
					"Discovered Alyt device is unsupported: type '{}' with id '{}'",
					device.getDeviceType(), device.getAlytID());
		}
	}

	@Override
	protected void startScan() {
		// this can be ignored here as we discover via the bridge
	}

	public void onDeviceStateChanged(ThingUID bridge, AlytDevice device) {
		// this can be ignored here
	}

	public void onDeviceRemoved(AlytBridgeHandler bridge, AlytDevice device) {
		// this can be ignored here
	}
}
