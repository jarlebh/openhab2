/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt.internal.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.alyt.AlytHub;
import org.openhab.alyt.discover.AlytHubDiscovery;
import org.openhab.binding.alyt.AlytBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AlytBridgeDiscovery} is responsible for discovering new ALYT
 * Cube LAN gateway devices on the network
 * 
 * @author Marcel Verpaalen - Initial contribution
 * 
 */
public class AlytBridgeDiscovery extends AbstractDiscoveryService {

	private final static Logger logger = LoggerFactory.getLogger(AlytBridgeDiscovery.class);

	static boolean discoveryRunning = false;

	public AlytBridgeDiscovery() {
		super(AlytBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS, 15);
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return AlytBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS;
	}

	@Override
	public void startScan() {
		discoverAlyt();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.smarthome.config.discovery.AbstractDiscoveryService#
	 * startBackgroundDiscovery()
	 */

	@Override
	protected void startBackgroundDiscovery() {
		discoverAlyt();
	}

	@Override
	public boolean isBackgroundDiscoveryEnabled() {
		return true;
	}

	private synchronized void discoverAlyt() {
		try {
			List<AlytHub> alytHubs = new AlytHubDiscovery().discover();
			for (AlytHub hub : alytHubs) {
				discoveryResultSubmission(hub);
			}
		} catch (Exception e) {
			logger.error("Failed in discover",e);
		}
	}

	
	private void discoveryResultSubmission(AlytHub hub) {
		if (hub != null) {
			logger.trace("Adding new ALYT Lan Gateway on {} with id '{}' to Smarthome inbox", hub.getIPAddr(),
					hub.getName());
			Map<String, Object> properties = new HashMap<>(2);
			properties.put(AlytBindingConstants.IP_ADDRESS, hub.getIPAddr());
			ThingUID uid = new ThingUID(AlytBindingConstants.ALYTBRIDGE_THING_TYPE, hub.getName());
			if (uid != null) {
				DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
						.withLabel("ALYT LAN Gateway").build();
				thingDiscovered(result);
			}
		}
	}

}
