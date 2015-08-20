/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tellstick.internal.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.tellstick.TellstickBindingConstants;
//import org.openhab.binding.max.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.JNA;
import org.tellstick.device.TellstickController;
import org.tellstick.device.TellstickDevice;

import com.sun.jna.Platform;

/**
 * The {@link TellstickBridgeDiscovery} is responsible for discovering new MAX!
 * Cube LAN gateway devices on the network
 *
 * @author Marcel Verpaalen - Initial contribution
 *
 */
public class TellstickBridgeDiscovery extends AbstractDiscoveryService {

    private final static Logger logger = LoggerFactory.getLogger(TellstickBridgeDiscovery.class);

    static boolean discoveryRunning = false;
    static boolean initilized = false;

    public TellstickBridgeDiscovery() {
        super(TellstickBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS, 15);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return TellstickBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        discoverController();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.AbstractDiscoveryService#
     * startBackgroundDiscovery()
     */

    @Override
    protected void startBackgroundDiscovery() {
        discoverController();
    }

    @Override
    public boolean isBackgroundDiscoveryEnabled() {
        return true;
    }

    private synchronized void discoverController() {
        if (!discoveryRunning) {
            discoveryRunning = true;
            listBridge();
        }
    }

    private void listBridge() {

        try {

            String libraryPath = init();
            List<TellstickController> cntrls = TellstickController.getControllers();
            for (TellstickController contrl : cntrls) {
                discoveryResultSubmission(contrl, libraryPath);
            }
        } catch (UnsatisfiedLinkError e) {
            logger.error(
                    "Could not load telldus core, please make sure Telldus is installed and correct 32/64 bit java. ",
                    e);
        } finally {
            // Close the port!
            discoveryRunning = false;
        }
    }

    private String init() {
        String libraryPath = null;
        if (!initilized) {
            if (Platform.isWindows()) {
                libraryPath = "C:/Program Files/Telldus/;C:/Program Files (x86)/Telldus/";
            }

            if (libraryPath != null) {
                logger.info("Loading " + JNA.library + " from " + libraryPath);
                System.setProperty("jna.library.path", libraryPath);
            } else {
                logger.info("Loading " + JNA.library + " from system default paths");
            }
            TellstickDevice.setSupportedMethods(JNA.CLibrary.TELLSTICK_BELL | JNA.CLibrary.TELLSTICK_TURNOFF
                    | JNA.CLibrary.TELLSTICK_TURNON | JNA.CLibrary.TELLSTICK_DIM | JNA.CLibrary.TELLSTICK_STOP);
            JNA.CLibrary.INSTANCE.tdInit();
            initilized = true;
        }
        return libraryPath;
    }

    private void discoveryResultSubmission(TellstickController controller, String configPath) {
        if (controller != null && controller.isOnline()) {
            logger.trace("Adding new Telldus Controller  {}", controller);
            Map<String, Object> properties = new HashMap<>(2);
            properties.put(TellstickBindingConstants.CONFIGPATH_ID, configPath);
            ThingUID uid = new ThingUID(TellstickBindingConstants.TELLDUSCOREBRIDGE_THING_TYPE,
                    Integer.toString(controller.getId()));
            if (uid != null) {
                DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                        .withLabel(controller.getType().name() + ": " + controller.getName()).build();
                thingDiscovered(result);
            }
        }
    }

}
