package org.openhab.binding.tellstick.handler;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tellstick.device.TellstickDevice;
import org.tellstick.device.TellstickException;
import org.tellstick.device.iface.Device;
import org.tellstick.device.iface.DimmableDevice;
import org.tellstick.device.iface.SwitchableDevice;

public class TellstickDeviceController {
    private static final Logger logger = LoggerFactory.getLogger(TellstickBridgeHandler.class);
    private long lastSend = 0;
    long resendInterval = 100;
    public static final long DEFAULT_INTERVAL_BETWEEN_SEND = 250;

    public TellstickDeviceController(long resendInterval) {
        this.resendInterval = resendInterval;
    }

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

    public long getLastSend() {
        return lastSend;
    }

    public void setLastSend(long currentTimeMillis) {
        lastSend = currentTimeMillis;
    }
}
