/**
 * Copyright (c)2016 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tellstick.handler.live;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.tellstick.conf.TelldusLiveConfiguration;
import org.openhab.binding.tellstick.handler.DeviceStatusListener;
import org.openhab.binding.tellstick.handler.TelldusBridgeHandler;
import org.openhab.binding.tellstick.handler.TelldusDeviceController;
import org.openhab.binding.tellstick.handler.TelldusDevicesHandler;
import org.openhab.binding.tellstick.handler.live.xml.DataTypeValue;
import org.openhab.binding.tellstick.handler.live.xml.TellstickNetDevice;
import org.openhab.binding.tellstick.handler.live.xml.TellstickNetDevices;
import org.openhab.binding.tellstick.handler.live.xml.TellstickNetSensor;
import org.openhab.binding.tellstick.handler.live.xml.TellstickNetSensors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.device.TellstickDeviceEvent;
import org.tellstick.device.TellstickSensorEvent;
import org.tellstick.device.iface.Device;

/**
 * {@link TelldusLiveBridgeHandler} is the handler for Telldus Live service (Tellstick.NET and ZNET) and connects it
 * to the framework. All {@link TelldusDevicesHandler}s use the
 * {@link TelldusLiveDeviceController} to execute the actual commands.
 *
 * @author Jarle Hjortland
 *
 */
public class TelldusLiveBridgeHandler extends BaseBridgeHandler implements TelldusBridgeHandler {

    final static Logger logger = LoggerFactory.getLogger(TelldusLiveBridgeHandler.class);

    private TellstickNetDevices deviceList = null;
    private TellstickNetSensors sensorList = null;
    private TelldusLiveDeviceController controller = new TelldusLiveDeviceController();
    private List<DeviceStatusListener> deviceStatusListeners = new Vector<DeviceStatusListener>();

    public TelldusLiveBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    private ScheduledFuture<?> pollingJob;
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            refreshDeviceList();
        }
    };

    @Override
    public void dispose() {
        logger.debug("Live Handler disposed.");
        pollingJob.cancel(true);
        deviceList = null;
        sensorList = null;
        super.dispose();
    }

    @Override
    public void initialize() {
        // super.initialize();
        logger.debug("Initializing TelldusLive bridge handler.");
        TelldusLiveConfiguration configuration = getConfigAs(TelldusLiveConfiguration.class);
        // workaround for issue #92: getHandler() returns NULL after
        // configuration update. :
        getThing().setHandler(this);
        this.controller = new TelldusLiveDeviceController();
        this.controller.connectHttpClient(configuration.publicKey, configuration.privateKey, configuration.token,
                configuration.tokenSecret);
        // refreshDeviceList();
        startAutomaticRefresh(configuration.refreshInterval);
        updateStatus(ThingStatus.ONLINE);
    }

    private synchronized void startAutomaticRefresh(long refreshInterval) {
        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0, refreshInterval, TimeUnit.MILLISECONDS);
        }
    }

    synchronized void refreshDeviceList() {
        updateDevices(deviceList);

        updateSensors(sensorList);

        logger.info("Found senors list " + sensorList.getSensors());
    }

    private synchronized void updateDevices(TellstickNetDevices previouslist) {
        TellstickNetDevices newList = controller.callRestMethod(TelldusLiveDeviceController.HTTP_TELLDUS_DEVICES,
                TellstickNetDevices.class);
        logger.info("Device list " + newList.getDevices());
        if (previouslist == null) {
            logger.debug("updateDevices, Creating devices.");
            for (TellstickNetDevice device : newList.getDevices()) {
                device.setUpdated(true);
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceAdded(getThing(), device);
                }
            }
            this.deviceList = newList;
        } else {
            logger.debug("updateDevices, Updating devices.");
            for (TellstickNetDevice device : newList.getDevices()) {
                int index = previouslist.getDevices().indexOf(device);
                logger.debug("Device:" + device + " found at " + index);
                if (index >= 0) {
                    TellstickNetDevice orgDevice = previouslist.getDevices().get(index);
                    if (device.getState() != orgDevice.getState()) {
                        orgDevice.setState(device.getState());
                        orgDevice.setStatevalue(device.getStatevalue());
                        orgDevice.setUpdated(true);
                    }
                } else {
                    logger.debug("New Device - Adding:" + device);
                    previouslist.getDevices().add(device);
                    device.setUpdated(true);
                    for (DeviceStatusListener listener : deviceStatusListeners) {
                        listener.onDeviceAdded(getThing(), device);
                    }
                }
            }
        }

        for (TellstickNetDevice device : deviceList.getDevices()) {
            if (device.isUpdated()) {
                logger.debug("Updated device:" + device);
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceStateChanged(getThing(), device,
                            new TellstickDeviceEvent(device, null, null, null, System.currentTimeMillis()));
                }
                device.setUpdated(false);
            }
        }
    }

    private synchronized void updateSensors(TellstickNetSensors previouslist) {
        TellstickNetSensors newList = controller.callRestMethod(TelldusLiveDeviceController.HTTP_TELLDUS_SENSORS,
                TellstickNetSensors.class);
        logger.debug("Updated sensors:" + newList.getSensors());
        if (previouslist == null) {
            this.sensorList = newList;
            for (TellstickNetSensor sensor : sensorList.getSensors()) {
                sensor.setUpdated(true);
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceAdded(getThing(), sensor);
                }
            }
        } else {
            for (TellstickNetSensor sensor : previouslist.getSensors()) {
                sensor.setUpdated(false);
            }
            for (TellstickNetSensor sensor : newList.getSensors()) {
                int index = this.sensorList.getSensors().indexOf(sensor);
                if (index >= 0) {
                    TellstickNetSensor orgSensor = this.sensorList.getSensors().get(index);
                    if (sensor.getLastUpdated() > orgSensor.getLastUpdated()) {
                        logger.info("Update for sensor:" + sensor);
                        orgSensor.setData(sensor.getData());
                        orgSensor.setLastUpdated(sensor.getLastUpdated());
                        orgSensor.setUpdated(true);
                    }
                } else {
                    this.sensorList.getSensors().add(sensor);
                    sensor.setUpdated(true);
                    for (DeviceStatusListener listener : deviceStatusListeners) {
                        listener.onDeviceAdded(getThing(), sensor);
                    }
                }

            }
        }
        for (TellstickNetSensor sensor : sensorList.getSensors()) {
            for (DeviceStatusListener listener : deviceStatusListeners) {
                if (sensor.getData() != null && sensor.isUpdated()) {
                    for (DataTypeValue type : sensor.getData()) {
                        listener.onDeviceStateChanged(getThing(), sensor,
                                new TellstickSensorEvent(sensor.getId(), type.getValue(), type.getName(),
                                        sensor.getProtocol(), sensor.getModel(), System.currentTimeMillis()));
                        sensor.setUpdated(false);
                    }
                }

            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleRemoval() {
        super.handleRemoval();
    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {
        super.handleUpdate(channelUID, newState);
    }

    @Override
    public void thingUpdated(Thing thing) {
        super.thingUpdated(thing);
    }

    @Override
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

    @Override
    public boolean unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        boolean result = deviceStatusListeners.remove(deviceStatusListener);
        if (result) {
            // onUpdate();
        }
        return result;
    }

    private Device getDevice(String id, List<TellstickNetDevice> devices) {
        for (Device device : devices) {
            if (device.getId() == Integer.valueOf(id)) {
                return device;
            }
        }
        return null;
    }

    private Device getSensor(String id, List<TellstickNetSensor> sensors) {
        for (Device sensor : sensors) {
            if (sensor.getId() == Integer.valueOf(id)) {
                return sensor;
            }
        }
        return null;
    }
    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.tellstick.handler.TelldusBridgeHandlerIntf#getDevice(java.lang.String)
     */

    @Override
    public Device getDevice(String serialNumber) {
        return getDevice(serialNumber, getDevices());
    }

    private List<TellstickNetDevice> getDevices() {
        if (deviceList == null) {
            refreshDeviceList();
        }
        return deviceList.getDevices();
    }

    @Override
    public Device getSensor(String deviceUUId) {
        return getSensor(deviceUUId, sensorList.getSensors());
    }

    @Override
    public void rescanTelldusDevices() {
        refreshDeviceList();
    }

    @Override
    public TelldusDeviceController getController() {
        return controller;
    }
}