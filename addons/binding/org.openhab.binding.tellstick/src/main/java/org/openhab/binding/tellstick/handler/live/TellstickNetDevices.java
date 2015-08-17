package org.openhab.binding.tellstick.handler.live;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "devices")
public class TellstickNetDevices {

    public TellstickNetDevices() {
        super();
    }

    List<TellstickNetDevice> devices;

    @XmlElement(name = "device")
    public List<TellstickNetDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<TellstickNetDevice> devices) {
        this.devices = devices;
    }
}
