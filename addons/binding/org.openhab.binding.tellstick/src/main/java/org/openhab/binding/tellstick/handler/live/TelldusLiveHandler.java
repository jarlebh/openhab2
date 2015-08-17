package org.openhab.binding.tellstick.handler.live;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Response;
import com.ning.http.client.oauth.ConsumerKey;
import com.ning.http.client.oauth.OAuthSignatureCalculator;
import com.ning.http.client.oauth.RequestToken;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;

public class TelldusLiveHandler extends BaseBridgeHandler implements TelldusBridgeHandler {

    static final String HTTP_API_TELLDUS_COM_XML = "http://api.telldus.com/xml/";
    static final String HTTP_TELLDUS_CLIENTS = HTTP_API_TELLDUS_COM_XML + "clients/list";
    static final String HTTP_TELLDUS_DEVICES = HTTP_API_TELLDUS_COM_XML + "devices/list";
    static final String HTTP_TELLDUS_SENSORS = HTTP_API_TELLDUS_COM_XML + "sensors/list?includeValues=1&includeScale=1";
    static final String HTTP_TELLDUS_SENSOR_INFO = HTTP_API_TELLDUS_COM_XML + "sensor/info";
    private final static Logger logger = LoggerFactory.getLogger(TelldusLiveHandler.class);

    private static final int REQUEST_TIMEOUT_MS = 2000;

    private AsyncHttpClient client;
    private TellstickNetDevices deviceList;
    private TellstickNetSensors sensorList;
    private TelldusLiveDeviceController controller;
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
        connectHttpClient(configuration.publicKey, configuration.privateKey, configuration.token,
                configuration.tokenSecret);
        refreshDeviceList();
        startAutomaticRefresh(configuration.refreshInterval);
        updateStatus(ThingStatus.ONLINE);
    }

    private synchronized void startAutomaticRefresh(long refreshInterval) {
        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0, refreshInterval, TimeUnit.MILLISECONDS);
        }
    }

    void connectHttpClient(String publicKey, String privateKey, String token, String tokenSecret) {
        ConsumerKey consumer = new ConsumerKey(publicKey, privateKey);
        RequestToken user = new RequestToken(token, tokenSecret);
        OAuthSignatureCalculator calc = new OAuthSignatureCalculator(consumer, user);
        this.client = new AsyncHttpClient(new NettyAsyncHttpProvider(createAsyncHttpClientConfig()));
        try {
            this.client.setSignatureCalculator(calc);
            Response response = client.prepareGet(HTTP_TELLDUS_CLIENTS).execute().get();
            logger.info("Response " + response.getResponseBody() + " tt " + response.getStatusText());

        } catch (InterruptedException | ExecutionException | IOException e) {
            // TODO Auto-generated catch block
            logger.error("Failed to connect", e);
        }
    }

    private AsyncHttpClientConfig createAsyncHttpClientConfig() {
        Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setRequestTimeoutInMs(REQUEST_TIMEOUT_MS);
        builder.setUseRawUrl(true);
        return builder.build();
    }

    void refreshDeviceList() {
        updateDevices(deviceList);

        updateSensors(sensorList);

        logger.info("sensorList list " + sensorList.getSensors());
        lastUpdate = System.currentTimeMillis() / 1000L;
    }

    private void updateDevices(TellstickNetDevices previouslist) {
        TellstickNetDevices newList = getDocument(HTTP_TELLDUS_DEVICES, TellstickNetDevices.class);
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

    private void updateSensors(TellstickNetSensors previouslist) {
        TellstickNetSensors newList = getDocument(HTTP_TELLDUS_SENSORS, TellstickNetSensors.class);
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
                logger.info("sensor:" + sensor);
                if (sensor.getLastUpdated() > lastUpdate) {
                    int index = this.sensorList.getSensors().indexOf(sensor);

                    if (index >= 0) {
                        TellstickNetSensor orgSensor = this.sensorList.getSensors().get(index);
                        orgSensor.setData(sensor.getData());
                        orgSensor.setUpdated(true);
                    } else {
                        this.sensorList.getSensors().add(sensor);
                        sensor.setUpdated(true);
                        for (DeviceStatusListener listener : deviceStatusListeners) {
                            listener.onDeviceAdded(getThing(), sensor);
                        }
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

    private <T> T getDocument(String uri, Class<T> response) {
        try {
            Future<Response> future = client.prepareGet(uri).execute();
            Response resp = future.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            logger.info("Devices" + resp.getResponseBody());
            JAXBContext jc = JAXBContext.newInstance(response);
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader xsr = xif.createXMLStreamReader(resp.getResponseBodyAsStream());
            // xsr = new PropertyRenamerDelegate(xsr);

            @SuppressWarnings("unchecked")
            T obj = (T) jc.createUnmarshaller().unmarshal(xsr);

            return obj;
        } catch (JAXBException e) {
            logger.warn("Encoding error in get", e);
        } catch (XMLStreamException e) {
            logger.warn("Communication error in get", e);
        } catch (InterruptedException e) {
            logger.warn("InterruptedException error in get", e);
        } catch (ExecutionException e) {
            logger.warn("ExecutionException error in get", e);
        } catch (TimeoutException e) {
            logger.warn("TimeoutException error in get", e);
        } catch (IOException e) {
            logger.warn("IOException error in get", e);
        }

        return null;
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
        return getDevice(serialNumber, deviceList.getDevices());
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
