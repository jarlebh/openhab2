package org.openhab.binding.tellstick.handler;

import static org.openhab.binding.tellstick.TellstickBindingConstants.*;

import java.math.BigDecimal;
import java.util.Calendar;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.tellstick.TellstickBindingConstants;
import org.openhab.binding.tellstick.handler.live.DataTypeValue;
import org.openhab.binding.tellstick.handler.live.TellstickNetSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.device.TellstickException;
import org.tellstick.device.TellstickSensor;
import org.tellstick.device.TellstickSensorEvent;
import org.tellstick.device.iface.Device;
import org.tellstick.device.iface.DimmableDevice;
import org.tellstick.device.iface.TellstickEvent;
import org.tellstick.enums.DataType;
import org.tellstick.enums.DeviceType;

public class TelldusDevicesHandler extends BaseThingHandler
        implements org.openhab.binding.tellstick.handler.DeviceStatusListener {

    private Logger logger = LoggerFactory.getLogger(TelldusDevicesHandler.class);
    private String deviceId;
    private Boolean isDimmer = Boolean.FALSE;
    private int resend = 1;
    private TelldusBridgeHandler bridgeHandler = null;

    public TelldusDevicesHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("Handle event " + command + " for " + channelUID);
        TelldusBridgeHandler bridgeHandler = getTellstickBridgeHandler();
        if (bridgeHandler == null) {
            logger.warn("Tellstick bridge handler not found. Cannot handle command without bridge.");
            return;
        }
        if (command instanceof RefreshType) {
            bridgeHandler.handleCommand(channelUID, command);
            return;
        }
        Device dev = bridgeHandler.getDevice(deviceId);
        if (dev == null) {
            logger.warn("Device not found. Can't send command to device '{}'", deviceId);
            return;
        }

        if (channelUID.getId().equals(CHANNEL_DIMMER) || channelUID.getId().equals(CHANNEL_STATE)) {
            try {
                if (dev.getDeviceType() == DeviceType.DEVICE) {
                    getTellstickBridgeHandler().getController().handleSendEvent(dev, resend, isDimmer, command);
                } else {
                    logger.warn("{} is not an updateable device. Read-only", dev);
                }
            } catch (TellstickException e) {
                logger.error("Failed to send command to tellstick", e);
            }
        } else {
            logger.warn("Setting of channel {} not possible. Read-only", channelUID);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {

        Configuration config = getThing().getConfiguration();
        final Object configDeviceId = config.get(TellstickBindingConstants.DEVICE_ID);
        if (configDeviceId != null) {
            deviceId = configDeviceId.toString();
        }
        if (deviceId != null) {
            logger.debug("Initialized TellStick device handler for {}.", deviceId);
        } else {
            logger.debug("Initialized TellStick device missing serialNumber configuration... troubles ahead");
        }
        final Boolean isADimmer = (Boolean) config.get(TellstickBindingConstants.DEVICE_ISDIMMER);
        if (isADimmer != null) {
            this.isDimmer = isADimmer;
        }
        final BigDecimal repeatCount = (BigDecimal) config.get(TellstickBindingConstants.DEVICE_RESEND_COUNT);
        if (repeatCount != null) {
            resend = repeatCount.intValue();
        }
    }

    @Override
    protected void bridgeHandlerInitialized(ThingHandler thingHandler, Bridge bridge) {
        TelldusBridgeHandler tellHandler = (TelldusBridgeHandler) thingHandler;
        if (tellHandler != null) {
            this.bridgeHandler = tellHandler;
            this.bridgeHandler.registerDeviceStatusListener(this);
            Configuration config = getThing().getConfiguration();
            Device dev = null;
            if (getThing().getThingTypeUID().equals(TellstickBindingConstants.SENSOR_THING_TYPE)) {
                dev = tellHandler.getSensor(deviceId);
                updateSensorStates(dev);
            } else {
                dev = tellHandler.getDevice(deviceId);
                updateDeviceState(dev);
            }
            if (dev != null) {
                if (dev.getName() != null) {
                    config.put(TellstickBindingConstants.DEVICE_NAME, dev.getName());
                }
                config.put(TellstickBindingConstants.DEVICE_PROTOCOL, dev.getProtocol());
                config.put(TellstickBindingConstants.DEVICE_MODEL, dev.getModel());
            } else {
                logger.warn("Could not find {}, please make sure it is defined and that telldus service is running",
                        deviceId);
            }

        }
    }

    private void updateSensorStates(Device dev) {
        if (dev instanceof TellstickSensor) {
            updateStatus(ThingStatus.ONLINE);
            for (DataType type : ((TellstickSensor) dev).getData().keySet()) {
                updateSensorDateState(type, ((TellstickSensor) dev).getData(type));
            }
        } else if (dev instanceof TellstickNetSensor) {
            if (((TellstickNetSensor) dev).getOnline()) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
            for (DataTypeValue type : ((TellstickNetSensor) dev).getData()) {
                updateSensorDateState(type.getName(), type.getValue());
            }
        }
    }

    @Override
    protected void bridgeHandlerDisposed(ThingHandler thingHandler, Bridge bridge) {
        logger.info("Bridge disposed for {}", getThing().getUID());
    }

    private synchronized TelldusBridgeHandler getTellstickBridgeHandler() {

        if (this.bridgeHandler == null) {
            logger.debug("No available bridge handler found for {} bridge {} .", deviceId, getBridge());
        }
        return this.bridgeHandler;
    }

    @Override
    public void onDeviceStateChanged(Bridge bridge, Device device, TellstickEvent event) {
        if (device.getUUId().equals(deviceId)) {

            logger.debug("Updating states of {} {} ({}) id: {}", device.getDeviceType(), device.getName(),
                    device.getUUId(), getThing().getUID());
            switch (device.getDeviceType()) {
                case DEVICE:
                    updateDeviceState(device);
                    break;
                case SENSOR:
                    TellstickSensorEvent sensorevent = (TellstickSensorEvent) event;
                    updateSensorDateState(sensorevent.getDataType(), sensorevent.getData());
                    break;

                default:
                    logger.debug("Unhandled Device {}.", device.getDeviceType());
                    break;

            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getTimestamp());
            updateState(new ChannelUID(getThing().getUID(), CHANNEL_TIMESTAMP), new DateTimeType(cal));

        }
    }

    private void updateSensorDateState(DataType dataType, String data) {
        switch (dataType) {
            case HUMIDITY:
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_HUMIDITY), new DecimalType(data));
                break;
            case TEMPERATURE:
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE), new DecimalType(data));
                break;
            default:
        }
    }

    private void updateDeviceState(Device device) {
        if (device != null) {
            State st = getTellstickBridgeHandler().getController().calcState(device);
            if (st != null) {
                BigDecimal dimValue = getTellstickBridgeHandler().getController().calcDimValue(device);
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_STATE), st);
                if (device instanceof DimmableDevice) {
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_DIMMER), new PercentType(dimValue));
                }
                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            updateStatus(ThingStatus.REMOVED);
        }
    }

    @Override
    public void onDeviceRemoved(Bridge bridge, Device device) {
        if (device.getUUId().equals(deviceId)) {
            updateStatus(ThingStatus.REMOVED);
        }
    }

    @Override
    public void onDeviceAdded(Bridge bridge, Device device) {
        // TODO Auto-generated method stub

    }

}
