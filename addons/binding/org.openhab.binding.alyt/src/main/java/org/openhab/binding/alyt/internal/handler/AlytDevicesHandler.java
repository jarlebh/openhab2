/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt.internal.handler;

import static org.openhab.binding.alyt.AlytBindingConstants.CHANNEL_STATE;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.alyt.devices.AlytDevice;
import org.openhab.alyt.devices.AlytSwitch;
import org.openhab.binding.alyt.AlytBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MaxDevicesHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Marcel Verpaalen - Initial contribution
 */
public class AlytDevicesHandler extends BaseThingHandler implements DeviceStatusListener {

	private Logger logger = LoggerFactory.getLogger(AlytDevicesHandler.class);
	private int refresh = 60; // refresh every minute as default
	ScheduledFuture<?> refreshJob;
	private AlytBridgeHandler bridgeHandler;

	private String alytDeviceID;
	private boolean forceRefresh = true;

	public AlytDevicesHandler(Thing thing) {
		super(thing);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize() {

		Configuration config = getThing().getConfiguration();
		final String configDeviceId = (String) config.get(AlytBindingConstants.ALYT_ID);

		if (configDeviceId != null) {
			alytDeviceID = configDeviceId;
		}
		if (alytDeviceID != null) {
			logger.debug("Initialized ALYT device handler for {}.", alytDeviceID);
		} else {
			logger.debug("Initialized ALYT device missing serialNumber configuration... troubles ahead");
		}
		// until we get an update put the Thing offline
		updateStatus(ThingStatus.OFFLINE);
		deviceOnlineWatchdog();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
	 */
	@Override
	public void dispose() {
		if (refreshJob != null && !refreshJob.isCancelled()) {
			refreshJob.cancel(true);
			refreshJob = null;
		}
		updateStatus(ThingStatus.OFFLINE);
		if (bridgeHandler != null)
			bridgeHandler.clearDeviceList();
		if (bridgeHandler != null)
			bridgeHandler.unregisterDeviceStatusListener(this);
		bridgeHandler = null;
		logger.debug("Thing {} {} disposed.", getThing().getUID(), alytDeviceID);
		super.dispose();
	}

	private void deviceOnlineWatchdog() {
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					AlytBridgeHandler bridgeHandler = getAlytBridgeHandler();
					if (bridgeHandler != null) {
						if (bridgeHandler.getDevice(alytDeviceID) == null) {
							updateStatus(ThingStatus.OFFLINE);
							bridgeHandler = null;
						} else {
							updateStatus(ThingStatus.ONLINE);
						}

					} else {
						logger.debug("Bridge for maxcube device {} not found.", alytDeviceID);
						updateStatus(ThingStatus.OFFLINE);
					}

				} catch (Exception e) {
					logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
					bridgeHandler = null;
				}

			}
		};

		refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, refresh, TimeUnit.SECONDS);
	}

	private synchronized AlytBridgeHandler getAlytBridgeHandler() {

		if (this.bridgeHandler == null) {
			Bridge bridge = getBridge();
			if (bridge == null) {
				logger.debug("Required bridge not defined for device {}.", alytDeviceID);
				return null;
			}
			ThingHandler handler = bridge.getHandler();
			if (handler instanceof AlytBridgeHandler) {
				this.bridgeHandler = (AlytBridgeHandler) handler;
				this.bridgeHandler.registerDeviceStatusListener(this);
			} else {
				logger.debug("No available bridge handler found for {} bridge {} .", alytDeviceID,
						bridge.getUID());
				return null;
			}
		}
		return this.bridgeHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		AlytBridgeHandler alytBridge = getAlytBridgeHandler();
		if (alytBridge == null) {
			logger.warn("ALYT LAN gateway bridge handler not found. Cannot handle command without bridge.");
			return;
		}
		if (command instanceof RefreshType) {
			forceRefresh = true;
			alytBridge.handleCommand(channelUID, command);
			return;
		}
		if (alytDeviceID == null) {
			logger.warn("ALYT ID missing. Can't send command to device '{}'", getThing());
			return;
		}

		if (channelUID.getId().equals(CHANNEL_STATE)) {
			alytBridge.handleStateChange(this.alytDeviceID, command);
		} else {
			logger.warn("Setting of channel {} not possible. Read-only", channelUID);
		}
	}

	@Override
	public void onDeviceStateChanged(ThingUID bridge, AlytDevice device) {
		if (device.getAlytID().equals(alytDeviceID)) {
			updateStatus(ThingStatus.ONLINE);
			if (device.isUpdated() || forceRefresh) {
				forceRefresh = false;
				logger.debug("Updating states of {} {} ({}) id: {}", device.getDeviceType(), device.getDeviceName(),
						device.getAlytID(), getThing().getUID());
				switch (device.getDeviceType()) {
				case WALL_PLUG:
				case GENERIC_SWITCH:
				case PLUG_IN_ON_OFF_MODULE:
					updateState(new ChannelUID(getThing().getUID(), CHANNEL_STATE), getState(device));
					break;
				/*case ShutterContact:
					updateState(new ChannelUID(getThing().getUID(), CHANNEL_CONTACT_STATE),
							(State) ((ShutterContact) device).getShutterState());
					updateState(new ChannelUID(getThing().getUID(), CHANNEL_BATTERY),
							(State) ((ShutterContact) device).getBatteryLow());
					break;
				case EcoSwitch:
					updateState(new ChannelUID(getThing().getUID(), CHANNEL_BATTERY),
							(State) ((EcoSwitch) device).getBatteryLow());
					break;*/
				default:
					logger.debug("Unhandled Device {}.", device.getDeviceType());
					break;

				}
			} else
				logger.debug("No changes for {} {} ({}) id: {}", device.getDeviceType(), device.getDeviceName(),
						device.getAlytID(), getThing().getUID());
		}
	}

	private State getState(AlytDevice device) {
		return (State) (((AlytSwitch) device).isState() ? OnOffType.ON : OnOffType.OFF);
	}

	@Override
	public void onDeviceRemoved(AlytBridgeHandler bridge, AlytDevice device) {
		if (device.getAlytID() == (alytDeviceID)) {
			bridgeHandler.unregisterDeviceStatusListener(this);
			bridgeHandler = null;
			forceRefresh = true;
			getThing().setStatusInfo(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "REMOVED"));
		}
	}

	@Override
	public void onDeviceAdded(Bridge bridge, AlytDevice device) {
		forceRefresh = true;
	}

}
