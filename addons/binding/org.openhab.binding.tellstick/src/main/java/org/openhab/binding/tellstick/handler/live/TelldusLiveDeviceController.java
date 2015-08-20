package org.openhab.binding.tellstick.handler.live;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.tellstick.handler.TelldusDeviceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.JNA;
import org.tellstick.device.TellstickDevice;
import org.tellstick.device.TellstickDeviceEvent;
import org.tellstick.device.TellstickException;
import org.tellstick.device.TellstickSensorEvent;
import org.tellstick.device.iface.Device;
import org.tellstick.device.iface.DeviceChangeListener;
import org.tellstick.device.iface.SensorListener;
import org.tellstick.device.iface.SwitchableDevice;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Response;
import com.ning.http.client.oauth.ConsumerKey;
import com.ning.http.client.oauth.OAuthSignatureCalculator;
import com.ning.http.client.oauth.RequestToken;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;

public class TelldusLiveDeviceController implements DeviceChangeListener, SensorListener, TelldusDeviceController {
    private static final Logger logger = LoggerFactory.getLogger(TelldusLiveDeviceController.class);
    private long lastSend = 0;
    long resendInterval = 100;
    public static final long DEFAULT_INTERVAL_BETWEEN_SEND = 250;
    static final int REQUEST_TIMEOUT_MS = 5000;
    AsyncHttpClient client;
    static final String HTTP_API_TELLDUS_COM_XML = "http://api.telldus.com/xml/";
    static final String HTTP_TELLDUS_CLIENTS = HTTP_API_TELLDUS_COM_XML + "clients/list";
    static final String HTTP_TELLDUS_DEVICES = HTTP_API_TELLDUS_COM_XML + "devices/list?supportedMethods=19";
    static final String HTTP_TELLDUS_SENSORS = HTTP_API_TELLDUS_COM_XML + "sensors/list?includeValues=1&includeScale=1";
    static final String HTTP_TELLDUS_SENSOR_INFO = HTTP_API_TELLDUS_COM_XML + "sensor/info";
    static final String HTTP_TELLDUS_DEVICE_DIM = HTTP_API_TELLDUS_COM_XML + "device/dim?id=%d&level=%d";
    static final String HTTP_TELLDUS_DEVICE_TURNOFF = HTTP_API_TELLDUS_COM_XML + "device/turnOff?id=%d";
    static final String HTTP_TELLDUS_DEVICE_TURNON = HTTP_API_TELLDUS_COM_XML + "device/turnOn?id=%d";

    public TelldusLiveDeviceController() {
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

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.tellstick.handler.TelldusDeviceController#handleSendEvent(org.tellstick.device.iface.Device,
     * int, boolean, org.eclipse.smarthome.core.types.Command)
     */
    @Override
    public void handleSendEvent(Device device, int resendCount, boolean isdimmer, Command command)
            throws TellstickException {

        for (int i = 0; i < resendCount; i++) {
            checkLastAndWait(resendInterval);
            logger.info("Send " + command + " to " + device + " times=" + i);
            if (device instanceof TellstickNetDevice) {
                if (command == OnOffType.ON) {
                    turnOn(device);
                } else if (command == OnOffType.OFF) {
                    turnOff(device);
                } else if (command instanceof PercentType) {
                    dim(device, (PercentType) command);
                } else if (command instanceof IncreaseDecreaseType) {
                    increaseDecrease(device, ((IncreaseDecreaseType) command));
                }
            } else if (device instanceof SwitchableDevice) {
                if (command == OnOffType.ON) {
                    if (isdimmer) {
                        logger.info("Turn off first in case it is allready on");
                        turnOff(device);
                        checkLastAndWait(resendInterval);
                    }
                    turnOn(device);
                } else if (command == OnOffType.OFF) {
                    turnOff(device);
                }
            } else {
                logger.warn("Cannot send to " + device);
            }

        }

    }

    private void increaseDecrease(Device dev, IncreaseDecreaseType increaseDecreaseType) throws TellstickException {
        String strValue = ((TellstickDevice) dev).getData();
        double value = 0;
        if (strValue != null) {
            value = Double.valueOf(strValue);
        }
        int percent = (int) Math.round((value / 255) * 100);
        if (IncreaseDecreaseType.INCREASE == increaseDecreaseType) {
            percent = Math.min(percent + 10, 100);
        } else if (IncreaseDecreaseType.DECREASE == increaseDecreaseType) {
            percent = Math.max(percent - 10, 0);
        }

        dim(dev, new PercentType(percent));
    }

    private void dim(Device dev, PercentType command) throws TellstickException {
        double value = command.doubleValue();

        // 0 means OFF and 100 means ON
        if (value == 0 && dev instanceof TellstickNetDevice) {
            turnOff(dev);
        } else if (value == 100 && dev instanceof TellstickNetDevice) {
            turnOn(dev);
        } else if (dev instanceof TellstickNetDevice
                && (((TellstickNetDevice) dev).getMethods() & JNA.CLibrary.TELLSTICK_DIM) > 0) {
            long tdVal = Math.round((value / 100) * 255);
            TelldusLiveResponse response = callRestMethod(String.format(HTTP_TELLDUS_DEVICE_DIM, dev.getId(), tdVal),
                    TelldusLiveResponse.class);
            handleResponse(response);
        } else {
            throw new RuntimeException("Cannot send DIM to " + dev);
        }
    }

    private void turnOff(Device dev) throws TellstickException {
        if (dev instanceof TellstickNetDevice) {
            TelldusLiveResponse response = callRestMethod(String.format(HTTP_TELLDUS_DEVICE_TURNOFF, dev.getId()),
                    TelldusLiveResponse.class);
            handleResponse(response);
        } else {
            throw new RuntimeException("Cannot send OFF to " + dev);
        }
    }

    private void handleResponse(TelldusLiveResponse response) {
        if (!response.status.trim().equals("success")) {
            throw new RuntimeException("Response " + response.status);
        }
    }

    private void turnOn(Device dev) throws TellstickException {
        if (dev instanceof TellstickNetDevice) {
            TelldusLiveResponse response = callRestMethod(String.format(HTTP_TELLDUS_DEVICE_TURNON, dev.getId()),
                    TelldusLiveResponse.class);
            handleResponse(response);
        } else {
            throw new RuntimeException("Cannot send ON to " + dev);
        }
    }

    private void checkLastAndWait(long resendInterval) {
        while ((System.currentTimeMillis() - lastSend) < resendInterval) {
            logger.info("Wait for " + resendInterval + " millisec");
            try {
                Thread.sleep(resendInterval);
            } catch (InterruptedException e) {
                logger.error("Failed to sleep", e);
            }
        }
        lastSend = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.tellstick.handler.TelldusDeviceController#calcState(org.tellstick.device.iface.Device)
     */
    @Override
    public State calcState(Device dev) {
        TellstickNetDevice device = (TellstickNetDevice) dev;
        State st = null;
        switch (device.getState()) {
            case JNA.CLibrary.TELLSTICK_TURNON:
                st = OnOffType.ON;
                break;
            case JNA.CLibrary.TELLSTICK_TURNOFF:
                st = OnOffType.OFF;
                break;
            case JNA.CLibrary.TELLSTICK_DIM:
                BigDecimal dimValue = new BigDecimal(device.getStatevalue());
                if (dimValue.intValue() == 0) {
                    st = OnOffType.OFF;
                } else if (dimValue.intValue() >= 255) {
                    st = OnOffType.ON;
                } else {
                    st = OnOffType.ON;
                }
                break;
            default:
                logger.warn("Could not handle {} for {}", device.getState(), device);
        }
        return st;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.tellstick.handler.TelldusDeviceController#calcDimValue(org.tellstick.device.iface.Device)
     */
    @Override
    public BigDecimal calcDimValue(Device device) {
        BigDecimal dimValue = new BigDecimal(0);
        switch (((TellstickNetDevice) device).getState()) {
            case JNA.CLibrary.TELLSTICK_TURNON:
                dimValue = new BigDecimal(100);
                break;
            case JNA.CLibrary.TELLSTICK_TURNOFF:
                break;
            case JNA.CLibrary.TELLSTICK_DIM:
                dimValue = new BigDecimal(((TellstickNetDevice) device).getStatevalue());
                dimValue = dimValue.multiply(new BigDecimal(100));
                dimValue = dimValue.divide(new BigDecimal(255), 0, BigDecimal.ROUND_HALF_UP);
                break;
            default:
                logger.warn("Could not handle {} for {}", (((TellstickNetDevice) device).getState()), device);
        }
        return dimValue;
    }

    public long getLastSend() {
        return lastSend;
    }

    public void setLastSend(long currentTimeMillis) {
        lastSend = currentTimeMillis;
    }

    @Override
    public void onRequest(TellstickSensorEvent newDevices) {
        setLastSend(newDevices.getTimestamp());
    }

    @Override
    public void onRequest(TellstickDeviceEvent newDevices) {
        setLastSend(newDevices.getTimestamp());

    }

    <T> T callRestMethod(String uri, Class<T> response) {
        try {
            Future<Response> future = client.prepareGet(uri).execute();
            Response resp = future.get(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // TelldusLiveHandler.logger.info("Devices" + resp.getResponseBody());
            JAXBContext jc = JAXBContext.newInstance(response);
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader xsr = xif.createXMLStreamReader(resp.getResponseBodyAsStream());
            // xsr = new PropertyRenamerDelegate(xsr);

            @SuppressWarnings("unchecked")
            T obj = (T) jc.createUnmarshaller().unmarshal(xsr);

            return obj;
        } catch (JAXBException e) {
            TelldusLiveHandler.logger.warn("Encoding error in get", e);
        } catch (XMLStreamException e) {
            TelldusLiveHandler.logger.warn("Communication error in get", e);
        } catch (InterruptedException e) {
            TelldusLiveHandler.logger.warn("InterruptedException error in get", e);
        } catch (ExecutionException e) {
            TelldusLiveHandler.logger.warn("ExecutionException error in get", e);
        } catch (TimeoutException e) {
            TelldusLiveHandler.logger.warn("TimeoutException error in get", e);
        } catch (IOException e) {
            TelldusLiveHandler.logger.warn("IOException error in get", e);
        }

        return null;
    }
}
