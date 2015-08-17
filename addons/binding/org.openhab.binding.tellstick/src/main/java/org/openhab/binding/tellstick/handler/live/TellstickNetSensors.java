package org.openhab.binding.tellstick.handler.live;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "sensors")
public class TellstickNetSensors {

    public TellstickNetSensors() {
        super();
    }

    List<TellstickNetSensor> sensors;

    @XmlElement(name = "sensor")
    public List<TellstickNetSensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<TellstickNetSensor> devices) {
        this.sensors = devices;
    }
}
