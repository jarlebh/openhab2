/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt.internal;

import static org.openhab.binding.alyt.AlytBindingConstants.ALYTBRIDGE_THING_TYPE;
import static org.openhab.binding.alyt.AlytBindingConstants.ALYT_ID;
import static org.openhab.binding.alyt.AlytBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.Hashtable;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.alyt.internal.discovery.AlytDeviceDiscoveryService;
import org.openhab.binding.alyt.internal.handler.AlytBridgeHandler;
import org.openhab.binding.alyt.internal.handler.AlytDevicesHandler;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AlytHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Jarle Hjortland - Initial contribution
 */
public class AlytHandlerFactory extends BaseThingHandlerFactory {
	private Logger logger = LoggerFactory.getLogger(AlytHandlerFactory.class);
	private ServiceRegistration<?> discoveryServiceReg;
	
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }
    @Override
	protected ThingHandler createHandler(Thing thing) {
		if (thing.getThingTypeUID().equals(ALYTBRIDGE_THING_TYPE)) {
			AlytBridgeHandler handler = new AlytBridgeHandler((Bridge) thing);
			registerDeviceDiscoveryService(handler);
			return handler;
		} else if (supportsThingType(thing.getThingTypeUID())) {
			return new AlytDevicesHandler(thing);
		} else {
			logger.debug("ThingHandler not found for {}", thing.getThingTypeUID());
			return null;
		}
	}
    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
		if (thingUID == null) {
			String uuid = (String) configuration.get(ALYT_ID);
			thingUID = new ThingUID(thingTypeUID, uuid);
		}
		return thingUID;
	}
    private ThingUID getAlytDeviceUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
			ThingUID bridgeUID) {
		String uuid = (String) configuration.get(ALYT_ID);

		if (thingUID == null) {
			thingUID = new ThingUID(thingTypeUID, uuid, bridgeUID.getId());
		}
		return thingUID;
	}
    private void registerDeviceDiscoveryService(AlytBridgeHandler maxCubeBridgeHandler) {
    	AlytDeviceDiscoveryService discoveryService = new AlytDeviceDiscoveryService(maxCubeBridgeHandler);
		discoveryService.activate();
		this.discoveryServiceReg = bundleContext.registerService(DiscoveryService.class.getName(), discoveryService,
				new Hashtable<String, Object>());
	}

	@Override
	protected void removeHandler(ThingHandler thingHandler) {
		if (this.discoveryServiceReg != null) {
			AlytDeviceDiscoveryService service = (AlytDeviceDiscoveryService) bundleContext
					.getService(discoveryServiceReg.getReference());
			service.deactivate();
			discoveryServiceReg.unregister();
			discoveryServiceReg = null;
		}
		super.removeHandler(thingHandler);
	}
    @Override
	public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
			ThingUID bridgeUID) {

        if (ALYTBRIDGE_THING_TYPE.equals(thingTypeUID)) {
			ThingUID cubeBridgeUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
			return super.createThing(thingTypeUID, configuration, cubeBridgeUID, null);
		}
		if (supportsThingType(thingTypeUID)) {
			ThingUID deviceUID = getAlytDeviceUID(thingTypeUID, thingUID, configuration, bridgeUID);
			return super.createThing(thingTypeUID, configuration, deviceUID, bridgeUID);
		}
		throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the binding.");
    }
}

