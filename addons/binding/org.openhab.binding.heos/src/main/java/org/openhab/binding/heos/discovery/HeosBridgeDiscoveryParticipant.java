/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos.discovery;

import static org.openhab.binding.heos.HeosBindingConstants.HEOSBRIDGE_THING_TYPE_UID;
import static org.openhab.binding.heos.config.ZonePlayerConfiguration.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.net.telnet.TelnetClient;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.model.meta.RemoteDevice;
import org.openhab.binding.heos.internal.HeosListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HeosBridgeDiscoveryParticipant} is responsible processing the
 * results of searches for UPNP devices
 *
 * @author Karel Goderis - Initial contribution
 */
public class HeosBridgeDiscoveryParticipant implements UpnpDiscoveryParticipant {

    private Logger logger = LoggerFactory.getLogger(HeosBridgeDiscoveryParticipant.class);
    private Boolean foundBridge = false;

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(HEOSBRIDGE_THING_TYPE_UID);
    }

    @Override
    public DiscoveryResult createResult(RemoteDevice device) {
        ThingUID uid = getThingUID(device);
        DiscoveryResult result = null;
        synchronized (foundBridge) {
            if (uid != null) {
                Map<String, Object> properties = new HashMap<>(3);
                String label = "Heos Bridge";
                String ip = device.getIdentity().getDescriptorURL().getHost();
                if (testIP(ip)) {
                    properties.put(UDN, device.getIdentity().getUdn().getIdentifierString());
                    properties.put(IP_ADDRESS, ip);

                    result = DiscoveryResultBuilder.create(uid).withProperties(properties).withLabel(label).build();
                    foundBridge = true;
                    logger.debug("Created a DiscoveryResult for device '{}' with {} and UDN '{}'",
                            device.getDetails().getFriendlyName(), ip,
                            device.getIdentity().getUdn().getIdentifierString());
                }
            } else if (uid != null) {
                String ip = device.getIdentity().getDescriptorURL().getHost();

            }
            return result;
        }
    }

    private boolean testIP(String ip) {
        TelnetClient tc = new TelnetClient();
        boolean result = false;
        try {
            tc.connect(ip, HeosListener.HEOS_PORT);
            tc.disconnect();
            result = true;
        } catch (Exception e) {
            logger.error("Failed to connect {} on port {}", ip, HeosListener.HEOS_PORT);
        }
        return result;
    }

    @Override
    public ThingUID getThingUID(RemoteDevice device) {

        if (device != null) {
            logger.debug("getThingUID '{}' {}", device.getIdentity().getUdn().getIdentifierString(),
                    device.getDetails().getManufacturerDetails().getManufacturer());
            if (device.getDetails().getModelDetails().getModelName() != null
                    && device.getDetails().getModelDetails().getModelName().toUpperCase().contains("HEOS")
                    || (device.getDetails().getManufacturerDetails().getManufacturer() != null && device.getDetails()
                            .getManufacturerDetails().getManufacturer().toUpperCase().contains("DENON"))) {
                logger.debug("Discovered a Heos Zone Player thing with UDN '{}'",
                        device.getIdentity().getUdn().getIdentifierString());
                return new ThingUID(HEOSBRIDGE_THING_TYPE_UID, "systembridge");
            }
        }
        return null;
    }
}
