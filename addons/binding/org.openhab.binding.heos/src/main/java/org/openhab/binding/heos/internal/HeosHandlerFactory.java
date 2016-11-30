/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos.internal;

import static org.openhab.binding.heos.HeosBindingConstants.*;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceRegistry;
import org.eclipse.smarthome.core.audio.AudioHTTPServer;
import org.eclipse.smarthome.core.audio.AudioSink;
import org.eclipse.smarthome.core.net.HttpServiceUtil;
import org.eclipse.smarthome.core.net.NetUtil;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.io.transport.upnp.UpnpIOService;
import org.openhab.binding.heos.discovery.HeosDiscoveryService;
import org.openhab.binding.heos.handler.HeosBridgeHandler;
import org.openhab.binding.heos.handler.HeosPlayerHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * The {@link HeosHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Karel Goderis - Initial contribution
 */
public class HeosHandlerFactory extends BaseThingHandlerFactory {

    private Logger logger = LoggerFactory.getLogger(HeosHandlerFactory.class);

    private DiscoveryServiceRegistry discoveryServiceRegistry;
    private HeosDiscoveryService heosDiscoveryService;
    // optional OPML URL that can be configured through configuration admin
    private String opmlUrl = null;
    private UpnpIOService upnpIOService;
    private AudioHTTPServer audioHTTPServer;

    // url (scheme+server+port) to use for playing notification sounds
    private String callbackUrl = null;

    private Map<String, ServiceRegistration<AudioSink>> audioSinkRegistrations = new ConcurrentHashMap<>();

    private final static Collection<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Lists
            .newArrayList(HEOSPLAYER_THING_TYPE_UID, HEOSBRIDGE_THING_TYPE_UID);

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<String, Object> properties = componentContext.getProperties();
        opmlUrl = (String) properties.get("opmlUrl");
    };

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {

        if (HEOSBRIDGE_THING_TYPE_UID.equals(thingTypeUID)) {
            ThingUID heosBridgePID = getPlayerUID(thingTypeUID, thingUID, configuration);
            logger.debug("Creating a heos zone player thing with ID '{}'", heosBridgePID);
            return super.createThing(thingTypeUID, configuration, heosBridgePID, null);
        } else if (HEOSPLAYER_THING_TYPE_UID.equals(thingTypeUID)) {
            ThingUID heosPlayerUID = getPlayerUID(thingTypeUID, thingUID, configuration);
            logger.debug("Creating a heos zone player thing with ID '{}'", heosPlayerUID);
            return super.createThing(thingTypeUID, configuration, heosPlayerUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the heos binding.");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(HEOSBRIDGE_THING_TYPE_UID)) {
            logger.debug("Creating a HeosBridge for thing '{}' with UDN '{}'", thing.getUID(),
                    thing.getConfiguration().get(PLAYER_PID));
            HeosBridgeHandler heosBridgeHandler = new HeosBridgeHandler((Bridge) thing);
            registerDeviceDiscoveryService(heosBridgeHandler);
            return heosBridgeHandler;
        } else if (thingTypeUID.equals(HEOSPLAYER_THING_TYPE_UID)) {
            logger.debug("Creating a HeosPlayer for thing '{}' with UDN '{}'", thing.getUID(),
                    thing.getConfiguration().get(PLAYER_PID));
            String callbackUrl = createCallbackUrl();

            HeosPlayerHandler handler = new HeosPlayerHandler(thing, thing.getUID().getId());
            HeosAudioSink audioSink = new HeosAudioSink(handler, audioHTTPServer, callbackUrl);
            @SuppressWarnings("unchecked")
            ServiceRegistration<AudioSink> reg = (ServiceRegistration<AudioSink>) bundleContext
                    .registerService(AudioSink.class.getName(), audioSink, new Hashtable<String, Object>());
            audioSinkRegistrations.put(thing.getUID().toString(), reg);
            return handler;
        }

        return null;
    }

    private ThingUID getPlayerUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {

        String udn = (String) configuration.get(PLAYER_PID);

        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, udn);
        }

        return thingUID;
    }

    protected void setDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        this.discoveryServiceRegistry = discoveryServiceRegistry;
    }

    private void registerDeviceDiscoveryService(HeosBridgeHandler heosBridgeHandler) {
        if (heosDiscoveryService == null) {
            heosDiscoveryService = new HeosDiscoveryService(heosBridgeHandler);
            heosDiscoveryService.activate();
            bundleContext.registerService(DiscoveryService.class.getName(), heosDiscoveryService,
                    new Hashtable<String, Object>());
        } else {
            heosDiscoveryService.addBridgeHandler(heosBridgeHandler);
        }

    }

    protected void unsetDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        this.discoveryServiceRegistry = null;
    }

    private String createCallbackUrl() {
        if (callbackUrl != null) {
            return callbackUrl;
        } else {
            final String ipAddress = NetUtil.getLocalIpv4HostAddress();
            if (ipAddress == null) {
                logger.warn("No network interface could be found.");
                return null;
            }

            // we do not use SSL as it can cause certificate validation issues.
            final int port = HttpServiceUtil.getHttpServicePort(bundleContext);
            if (port == -1) {
                logger.warn("Cannot find port of the http service.");
                return null;
            }

            return "http://" + ipAddress + ":" + port;
        }
    }

    protected void setUpnpIOService(UpnpIOService upnpIOService) {
        this.upnpIOService = upnpIOService;
    }

    protected void unsetUpnpIOService(UpnpIOService upnpIOService) {
        this.upnpIOService = null;
    }

    protected void setAudioHTTPServer(AudioHTTPServer audioHTTPServer) {
        this.audioHTTPServer = audioHTTPServer;
    }

    protected void unsetAudioHTTPServer(AudioHTTPServer audioHTTPServer) {
        this.audioHTTPServer = null;
    }

}
