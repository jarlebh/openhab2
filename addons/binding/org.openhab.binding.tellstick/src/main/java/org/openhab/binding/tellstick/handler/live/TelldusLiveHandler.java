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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.device.TellstickDeviceEvent;
import org.tellstick.device.TellstickSensorEvent;
import org.tellstick.device.iface.Device;

public class TelldusLiveHandler extends BaseBridgeHandler implements TelldusBridgeHandler {

    final static Logger logger = LoggerFactory.getLogger(TelldusLiveHandler.class);

    private TellstickNetDevices deviceList = null;
    private TellstickNetSensors sensorList = null;
    private TelldusLiveDeviceController controller = new TelldusLiveDeviceController();
    private List<DeviceStatusListener> deviceStatusListeners = new Vector<DeviceStatusListener>();
    private long lastUpdate;

    public TelldusLiveHandler(Bridge bridge) {
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

        logger.info("sensorList list " + sensorList.getSensors());
        lastUpdate = ((System.currentTimeMillis() - 1000L) / 1000L);
    }

    private synchronized void updateDevices(TellstickNetDevices previouslist) {
        TellstickNetDevices newList = controller.callRestMethod(TelldusLiveDeviceController.HTTP_TELLDUS_DEVICES,
                TellstickNetDevices.class);
        logger.info("Device list " + newList.getDevices());
        if (previouslist == null) {
            this.deviceList = newList;
            for (TellstickNetDevice device : deviceList.getDevices()) {
                device.setUpdated(true);
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceAdded(getThing(), device);
                }
            }
        } else {
            for (TellstickNetDevice device : deviceList.getDevices()) {
                device.setUpdated(false);
            }
            for (TellstickNetDevice device : newList.getDevices()) {
                logger.info("device:" + device);
                int index = this.deviceList.getDevices().indexOf(device);
                if (index >= 0) {
                    TellstickNetDevice orgDevice = this.deviceList.getDevices().get(index);
                    if (device.getState() != orgDevice.getState()) {
                        orgDevice.setState(device.getState());
                        orgDevice.setStatevalue(device.getStatevalue());
                        orgDevice.setUpdated(true);
                    }
                } else {
                    this.deviceList.getDevices().add(device);
                    device.setUpdated(true);
                    for (DeviceStatusListener listener : deviceStatusListeners) {
                        listener.onDeviceAdded(getThing(), device);
                    }
                }
            }
        }

        for (TellstickNetDevice device : deviceList.getDevices()) {
            if (device.isUpdated()) {
                for (DeviceStatusListener listener : deviceStatusListeners) {
                    listener.onDeviceStateChanged(getThing(), device,
                            new TellstickDeviceEvent(device, null, null, null, System.currentTimeMillis()));
                }
            }
        }
    }

    private synchronized void updateSensors(TellstickNetSensors previouslist) {
        TellstickNetSensors newList = controller.callRestMethod(TelldusLiveDeviceController.HTTP_TELLDUS_SENSORS,
                TellstickNetSensors.class);
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

    private Device getSensor(String id, List<TellstickNetSensor> devices) {
        for (Device device : devices) {
            if (device.getId() == Integer.valueOf(id)) {
                return device;
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
