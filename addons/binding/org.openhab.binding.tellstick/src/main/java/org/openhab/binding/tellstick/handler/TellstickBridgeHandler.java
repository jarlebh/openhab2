/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tellstick.handler;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.tellstick.conf.TellstickBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.device.SupportedMethodsException;
import org.tellstick.device.TellsticEventHandler;
import org.tellstick.device.TellstickDevice;
import org.tellstick.device.TellstickDeviceEvent;
import org.tellstick.device.TellstickSensor;
import org.tellstick.device.TellstickSensorEvent;
import org.tellstick.device.iface.Device;
import org.tellstick.device.iface.DeviceChangeListener;
import org.tellstick.device.iface.SensorListener;
import org.tellstick.enums.ChangeType;
import org.tellstick.enums.DataType;

/**
 * {@link TellstickBridgeHandler} is the handler for a MAX! Cube and connects it
 * to the framework. All {@link MaxDevicesHandler}s use the
 * {@link TellstickBridgeHandler} to execute the actual commands.
 *
 * @author Marcel Verpaalen - Initial contribution OH2 version
 * @author Andreas Heil (info@aheil.de) - OH1 version
 * @author Bernd Michael Helm (bernd.helm at helmundwalter.de) - Exclusive mode
 *
 */
public class TellstickBridgeHandler extends BaseBridgeHandler implements DeviceChangeListener, SensorListener {
    // TODO: optional configuration to get the actual temperature on a
    // configured interval by changing the valve / temp setting

    public TellstickBridgeHandler(Bridge br) {
        super(br);
    }

    private Logger logger = LoggerFactory.getLogger(TellstickBridgeHandler.class);
    private TellstickDeviceController deviceController = null;
    private List<TellstickDevice> deviceList = new Vector<TellstickDevice>();
    private List<TellstickSensor> sensorList = new Vector<TellstickSensor>();
    private TellsticEventHandler eventHandler;

    private List<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArrayList<>();

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Refresh command received.");
            rescanTelldusDevices();
        } else
            logger.warn("No bridge commands defined.");
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        eventHandler.remove();
        clearDeviceList();

        super.dispose();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Tellstick bridge handler.");
        TellstickBridgeConfiguration configuration = getConfigAs(TellstickBridgeConfiguration.class);
        // workaround for issue #92: getHandler() returns NULL after
        // configuration update. :
        getThing().setHandler(this);

        rescanTelldusDevices();
        setupListeners();
        setupDeviceController(configuration);
        updateStatus(ThingStatus.ONLINE);
    }

    private void setupDeviceController(TellstickBridgeConfiguration configuration) {
        deviceController = new TellstickDeviceController(configuration.resendInterval);
    }

    public void rescanTelldusDevices() {
        try {
            deviceList = Collections.synchronizedList(TellstickDevice.getDevices());
            for (TellstickDevice device : deviceList) {
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceAdded(getThing(), device);
                }
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceStateChanged(getThing(), device,
                            new TellstickDeviceEvent(device, null, null, null, System.currentTimeMillis()));
                }
            }

            sensorList = Collections.synchronizedList(TellstickSensor.getAllSensors());
            for (TellstickSensor sensor : sensorList) {
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceAdded(getThing(), sensor);
                }
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    for (DataType type : sensor.getData().keySet()) {
                        listener.onDeviceStateChanged(getThing(), sensor,
                                new TellstickSensorEvent(sensor.getId(), sensor.getData(type), type,
                                        sensor.getProtocol(), sensor.getModel(), System.currentTimeMillis()));
                    }

                }
            }

        } catch (SupportedMethodsException e) {
            logger.error("Failed to get devices", e);
        }
    }

    private synchronized void setupListeners() {
        eventHandler = new TellsticEventHandler(deviceList);
        eventHandler.addListener(this);

    }

    public void onConnectionLost() {
        logger.info("Bridge connection lost. Updating thing status to OFFLINE.");
        updateStatus(ThingStatus.OFFLINE);
    }

    public void onConnection() {
        logger.info("Bridge connected. Updating thing status to ONLINE.");
        updateStatus(ThingStatus.ONLINE);
    }

    public boolean registerDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null deviceStatusListener.");
        }
        boolean result = deviceStatusListeners.add(deviceStatusListener);
        if (result) {
            // onUpdate();
        }
        return result;
    }

    public boolean unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        boolean result = deviceStatusListeners.remove(deviceStatusListener);
        if (result) {
            // onUpdate();
        }
        return result;
    }

    public void clearDeviceList() {
    }

    private Device getDevice(String id, List<TellstickDevice> devices) {
        for (Device device : devices) {
            if (device.getId() == Integer.valueOf(id)) {
                return device;
            }
        }
        return null;
    }

    /**
     * Returns the MAX! Device decoded during the last refreshData
     *
     * @param serialNumber
     *            the serial number of the device as String
     * @return device the {@link Device} information decoded in last refreshData
     */

    public Device getDevice(String serialNumber) {
        return getDevice(serialNumber, deviceList);
    }

    @Override
    public void onRequest(TellstickSensorEvent newEvent) {
        logger.info("Sensor Event ", newEvent.getData());
        TellstickSensor sensor = new TellstickSensor(newEvent.getSensorId(), newEvent.getProtocol(),
                newEvent.getModel());
        if (!sensorList.contains(sensor)) {
            sensor.setData(newEvent.getDataType(), newEvent.getData());
            sensorList.add(sensor);
            for (DeviceStatusListener listener : deviceStatusListeners) {
                listener.onDeviceAdded(getThing(), sensor);
            }
        } else {
            TellstickSensor useSensor = sensorList.get(sensorList.indexOf(sensor));
            useSensor.setData(newEvent.getDataType(), newEvent.getData());
            for (DeviceStatusListener listener : deviceStatusListeners) {
                listener.onDeviceStateChanged(getThing(), useSensor, newEvent);
            }

        }
    }

    @Override
    public void onRequest(TellstickDeviceEvent newEvent) {
        if (newEvent.getChangeType() == ChangeType.ADDED) {
            for (DeviceStatusListener listener : deviceStatusListeners) {
                listener.onDeviceAdded(getThing(), newEvent.getDevice());
            }
        } else if (newEvent.getChangeType() == ChangeType.REMOVED) {
            for (DeviceStatusListener listener : deviceStatusListeners) {
                listener.onDeviceRemoved(getThing(), newEvent.getDevice());
            }
        } else {
            for (DeviceStatusListener listener : deviceStatusListeners) {
                listener.onDeviceStateChanged(getThing(), newEvent.getDevice(), newEvent);
            }
        }
    }

    public Device getSensor(String deviceUUId) {
        for (Device device : sensorList) {
            if (device.getUUId().equals(deviceUUId)) {
                return device;
            }
        }
        return null;
    }

    public TellstickDeviceController getController() {
        return this.deviceController;
    }

}
