/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt.internal.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.alyt.AlytError;
import org.openhab.alyt.AlytEvent;
import org.openhab.alyt.AlytHub;
import org.openhab.alyt.AlytHubImpl;
import org.openhab.alyt.devices.AlytDevice;
import org.openhab.alyt.devices.AlytSwitch;
import org.openhab.alyt.enums.AlytEventType;
import org.openhab.binding.alyt.config.AlytBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AlytBridgeHandler} is the handler for a ALYT Cube and connects it to
 * the framework. All {@link MaxDevicesHandler}s use the
 * {@link AlytBridgeHandler} to execute the actual commands.
 * 
 * @author Marcel Verpaalen - Initial contribution OH2 version
 * @author Andreas Heil (info@aheil.de) - OH1 version
 * @author Bernd Michael Helm (bernd.helm at helmundwalter.de) - Exclusive mode
 * 
 */
public class AlytBridgeHandler extends BaseBridgeHandler {
	// TODO: optional configuration to get the actual temperature on a
	// configured interval by changing the valve / temp setting

	public AlytBridgeHandler(Bridge br) {
		super(br);
	}

	private Logger logger = LoggerFactory.getLogger(AlytBridgeHandler.class);

	/** The refresh interval which is used to poll given ALYTCube */
	private long refreshInterval = 10000;
	private AlytHub theHub = null;
	ScheduledFuture<?> refreshJob;

	private List<AlytDevice> devices = new ArrayList<AlytDevice>();
	private HashSet<String> lastActiveDevices = new HashSet<String>();

	// private ArrayList<DeviceConfiguration> configurations = new
	// ArrayList<DeviceConfiguration>();

	/** maximum queue size that we're allowing */
	// private ArrayBlockingQueue<SendCommand> commandQueue = new
	// ArrayBlockingQueue<SendCommand>(MAX_COMMANDS);

	private boolean connectionEstablished = false;

	// private SendCommand lastCommandId = null;

	private String ipAddress;
	private int port;
	private String password;
	private String name;
	private int maxRequestsPerConnection;

	/**
	 * connection socket and reader/writer for execute method
	 */
	private boolean previousOnline = false;

	private List<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArrayList<>();

	private ScheduledFuture<?> pollingJob;
	private Runnable pollingRunnable = new Runnable() {
		@Override
		public void run() {
			refreshData();
		}
	};

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (command instanceof RefreshType) {
			logger.debug("Refresh command received.");
			getInitialData();
		} else
			logger.warn("No bridge commands defined.");
	}

	@Override
	public void dispose() {
		logger.debug("Handler disposed.");
		if (pollingJob != null && !pollingJob.isCancelled()) {
			pollingJob.cancel(true);
			pollingJob = null;
		}

		clearDeviceList();
		connectionEstablished = false;

		super.dispose();
	}

	@Override
	public void initialize() {
		logger.debug("Initializing ALYT bridge handler.");

		AlytBridgeConfiguration configuration = getConfigAs(AlytBridgeConfiguration.class);
		port = configuration.port;
		ipAddress = configuration.ipAddress;
		refreshInterval = configuration.refreshInterval;
		password = configuration.password;
		logger.debug("Alyt IP         {}.", ipAddress);
		logger.debug("Port            {}.", port);
		logger.debug("RefreshInterval {}.", refreshInterval);
		logger.debug("Password  {}.", password);
		logger.debug("Max Requests    {}.", maxRequestsPerConnection);

		startAutomaticRefresh();
		getInitialData();
		// workaround for issue #92: getHandler() returns NULL after
		// configuration update. :
		getThing().setHandler(this);
	}

	private synchronized void startAutomaticRefresh() {
		if (pollingJob == null || pollingJob.isCancelled()) {
			pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0,
					refreshInterval, TimeUnit.MILLISECONDS);
		}
	}

	public void handleStateChange(String alytId, Command command) {
		try {
			reconnectHub();
			if (!connectionEstablished) {
				logger.warn("Cannot handle command: ALYT Hub not Connected.",
						this.ipAddress);
				return;
			}
			if (getThing().getStatus() != ThingStatus.ONLINE) {
				logger.warn("Cannot handle command: No connection to LIFX network.");
				return;
			}
			AlytSwitch device = (AlytSwitch) getDevice(alytId);
			if (command instanceof HSBType) {
				// handleHSBCommand((HSBType) command);
			} else if (command instanceof PercentType) {
				// handlePercentCommand((PercentType) command);
			} else if (command instanceof OnOffType) {
				handleOnOffCommand(device, (OnOffType) command);
			} else if (command instanceof IncreaseDecreaseType) {
				// handleIncreaseDecreaseCommand((IncreaseDecreaseType)
				// command);
			}
		} catch (Exception ex) {
			logger.error("Error while updating light.", ex);
		}
	}

	private void handleOnOffCommand(AlytSwitch device, OnOffType command)
			throws AlytError {
		if (command == OnOffType.ON) {
			device.turnOn();
		} else {
			device.turnOff();
		}

	}

	private synchronized void getInitialData() {
		try {
			reconnectHub();
			if (connectionEstablished) {
				updateStatus(ThingStatus.ONLINE);
				previousOnline = true;
				getConfig().put("id", theHub.getID());
				devices = theHub.getDevices();
				handleNewDevices();
			}
		} catch (AlytError e) {
			logger.error("AlytError occurred during execution: {}",
					e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Exception occurred during execution: {}",
					e.getMessage(), e);
		} catch (Throwable e) {
			logger.error("Throwable occurred during execution: {}",
					e.getMessage(), e);
		}
	}

	/**
	 * initiates read data from the maxCube bridge
	 */
	private synchronized void refreshData() {

		try {
			reconnectHub();

			if (connectionEstablished) {
				updateStatus(ThingStatus.ONLINE);
				previousOnline = true;
				List<AlytEvent> events = theHub
						.getNewEvents((int) (this.refreshInterval / 1000));
				handleEvents(events);
			} else if (previousOnline)
				onConnectionLost();

		} catch (AlytError e) {
			logger.error("AlytError occurred during execution: {}",
					e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Exception occurred during execution: {}",
					e.getMessage(), e);
		} catch (Throwable e) {
			logger.error("Throwable occurred during execution: {}",
					e.getMessage(), e);
		}
	}

	private void handleNewDevices() {
		for (AlytDevice di : devices) {
			if (lastActiveDevices != null
					&& lastActiveDevices.contains(di.getAlytID())) {
				for (DeviceStatusListener deviceStatusListener : deviceStatusListeners) {
					try {
						deviceStatusListener.onDeviceStateChanged(getThing()
								.getUID(), di);
					} catch (Exception e) {
						logger.error(
								"An exception occurred while calling the DeviceStatusListener",
								e);
					}
				}
			}
			// New device, not seen before, pass to Discovery
			else {
				for (DeviceStatusListener deviceStatusListener : deviceStatusListeners) {
					try {
						deviceStatusListener.onDeviceAdded(getThing(), di);
						deviceStatusListener.onDeviceStateChanged(getThing()
								.getUID(), di);
					} catch (Exception e) {
						logger.error(
								"An exception occurred while calling the DeviceStatusListener",
								e);
					}
					lastActiveDevices.add(di.getAlytID());
				}
			}
		}
	}

	private void handleEvents(List<AlytEvent> events) {
		for (AlytEvent event : events) {
			if (event.getEventType() == AlytEventType.ACTIVATION
					|| event.getEventType() == AlytEventType.DEACTIVATION ) {
				
				if (lastActiveDevices != null
						&& lastActiveDevices.contains(Integer.toString(event.getEventTarget()))) {
					AlytSwitch device = (AlytSwitch)getDevice(event.getEventTarget());
					updateState(event, device);
					for (DeviceStatusListener deviceStatusListener : deviceStatusListeners) {
						try {
							deviceStatusListener.onDeviceStateChanged(
									getThing().getUID(),device);
						} catch (Exception e) {
							logger.error(
									"An exception occurred while calling the DeviceStatusListener",
									e);
						}
					}
				} else {// New device, not seen before, pass to Discovery
					getInitialData();
				}
			}
		}
	}

	private void updateState(AlytEvent event, AlytSwitch device) {
		device.setState(event.getEventType() == AlytEventType.ACTIVATION);
	}

	public void onConnectionLost() {
		logger.info("Bridge connection lost. Updating thing status to OFFLINE.");
		previousOnline = false;
		updateStatus(ThingStatus.OFFLINE);
	}

	public void onConnection() {
		logger.info("Bridge connected. Updating thing status to ONLINE.");
		updateStatus(ThingStatus.ONLINE);
	}

	public boolean registerDeviceStatusListener(
			DeviceStatusListener deviceStatusListener) {
		if (deviceStatusListener == null) {
			throw new NullPointerException(
					"It's not allowed to pass a null deviceStatusListener.");
		}
		boolean result = deviceStatusListeners.add(deviceStatusListener);
		if (result) {
			// onUpdate();
		}
		return result;
	}

	public boolean unregisterDeviceStatusListener(
			DeviceStatusListener deviceStatusListener) {
		boolean result = deviceStatusListeners.remove(deviceStatusListener);
		if (result) {
			// onUpdate();
		}
		return result;
	}

	public void clearDeviceList() {
		lastActiveDevices = new HashSet<String>();
	}

	/**
	 * Processes device command and sends it to the ALYTCube Lan Gateway.
	 * 
	 * @param serialNumber
	 *            the serial number of the device as String
	 * @param channelUID
	 *            the ChannelUID used to send the command
	 * @param command
	 *            the command data
	 */

	/**
	 * Connects to the ALYT Lan gateway, reads and decodes the message this
	 * updates device information for each connected ALYTCube device
	 * 
	 * @throws AlytError
	 */
	private void reconnectHub() throws AlytError {
		if (theHub == null && this.password != null) {
			theHub = new AlytHubImpl(this.name, this.ipAddress, this.port,
					this.password);
		}
		if (theHub != null) {
			theHub.initialize();
			this.connectionEstablished = true;
		}
	}

	private AlytDevice getDevice(String id, List<AlytDevice> devices) {
		for (AlytDevice device : devices) {
			if (device.getAlytID().equals(id)) {
				return device;
			}
		}
		return null;
	}

	/**
	 * Returns the ALYT Device decoded during the last refreshData
	 * 
	 * @param serialNumber
	 *            the serial number of the device as String
	 * @return device the {@link Device} information decoded in last refreshData
	 */

	public AlytDevice getDevice(String id) {
		return getDevice(id, devices);
	}

	public AlytDevice getDevice(int id) {
		return getDevice(Integer.toString(id));
	}
}
