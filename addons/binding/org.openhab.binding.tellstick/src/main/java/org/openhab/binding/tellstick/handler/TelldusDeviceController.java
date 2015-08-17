package org.openhab.binding.tellstick.handler;

import java.math.BigDecimal;

import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.tellstick.device.TellstickException;
import org.tellstick.device.iface.Device;

public interface TelldusDeviceController {

    void handleSendEvent(Device device, int resendCount, boolean isdimmer, Command command) throws TellstickException;

    State calcState(Device dev);

    BigDecimal calcDimValue(Device device);

}