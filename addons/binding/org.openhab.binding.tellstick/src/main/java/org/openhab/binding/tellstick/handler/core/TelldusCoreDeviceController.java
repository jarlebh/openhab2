/**
 * Copyright (c)2016 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tellstick.handler.core;

import java.math.BigDecimal;

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
import org.tellstick.device.iface.DimmableDevice;
import org.tellstick.device.iface.SensorListener;
import org.tellstick.device.iface.SwitchableDevice;

/**
 * Device controller for telldus core (Basic and Duo).
 * This communicates with the telldus DLL using the javatellstick
 * library.
 *
 * @author Jarle Hjortland
 *
 */
public class TelldusCoreDeviceController implements DeviceChangeListener, SensorListener, TelldusDeviceController {
    private static final Logger logger = LoggerFactory.getLogger(TelldusCoreBridgeHandler.class);
    private long lastSend = 0;
    long resendInterval = 100;
    public static final long DEFAULT_INTERVAL_BETWEEN_SEND = 250;

    public TelldusCoreDeviceController(long resendInterval) {
        this.resendInterval = resendInterval;
    }

    @Override
    public void handleSendEvent(Device device, int resendCount, boolean isdimmer, Command command)
            throws TellstickException {

        for (int i = 0; i < resendCount; i++) {
            checkLastAndWait(resendInterval);
            logger.info("Send " + command + " to " + device + " times=" + i);
            if (device instanceof DimmableDevice) {
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
        if (value == 0 && dev instanceof SwitchableDevice) {
            ((SwitchableDevice) dev).off();
        } else if (value == 100 && dev instanceof SwitchableDevice) {
            ((SwitchableDevice) dev).on();
        } else if (dev instanceof DimmableDevice) {
            long tdVal = Math.round((value / 100) * 255);
            ((DimmableDevice) dev).dim((int) tdVal);
        } else {
            throw new RuntimeException("Cannot send DIM to " + dev);
        }
    }

    private void turnOff(Device dev) throws TellstickException {
        if (dev instanceof SwitchableDevice) {
            ((SwitchableDevice) dev).off();
        } else {
            throw new RuntimeException("Cannot send OFF to " + dev);
        }
    }

    private void turnOn(Device dev) throws TellstickException {
        if (dev instanceof SwitchableDevice) {
            ((SwitchableDevice) dev).on();
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

    @Override
    public State calcState(Device dev) {
        TellstickDevice device = (TellstickDevice) dev;
        State st = null;
        switch (device.getStatus()) {
            case JNA.CLibrary.TELLSTICK_TURNON:
                st = OnOffType.ON;
                break;
            case JNA.CLibrary.TELLSTICK_TURNOFF:
                st = OnOffType.OFF;
                break;
            case JNA.CLibrary.TELLSTICK_DIM:
                BigDecimal dimValue = new BigDecimal(device.getData());
                if (dimValue.intValue() == 0) {
                    st = OnOffType.OFF;
                } else if (dimValue.intValue() >= 255) {
                    st = OnOffType.ON;
                } else {
                    st = OnOffType.ON;
                }
                break;
            default:
                logger.warn("Could not handle {} for {}", device.getStatus(), device);
        }
        return st;
    }

    @Override
    public BigDecimal calcDimValue(Device device) {
        BigDecimal dimValue = new BigDecimal(0);
        switch (((TellstickDevice) device).getStatus()) {
            case JNA.CLibrary.TELLSTICK_TURNON:
                dimValue = new BigDecimal(100);
                break;
            case JNA.CLibrary.TELLSTICK_TURNOFF:
                break;
            case JNA.CLibrary.TELLSTICK_DIM:
                dimValue = new BigDecimal(((TellstickDevice) device).getData());
                dimValue = dimValue.multiply(new BigDecimal(100));
                dimValue = dimValue.divide(new BigDecimal(255), 0, BigDecimal.ROUND_HALF_UP);
                break;
            default:
                logger.warn("Could not handle {} for {}", ((TellstickDevice) device).getStatus(), device);
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
}
