package org.openhab.binding.tellstick.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.Command;
import org.tellstick.device.iface.Device;

public interface TelldusBridgeHandler {

    boolean registerDeviceStatusListener(DeviceStatusListener deviceStatusListener);

    boolean unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener);

    Device getDevice(String serialNumber);

    Device getSensor(String deviceUUId);

    void rescanTelldusDevices();

    TelldusDeviceController getController();

    void handleCommand(ChannelUID channelUID, Command command);

}