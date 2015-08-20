package org.openhab.binding.tellstick.handler.live;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "device")
public class TelldusLiveResponse {
    @XmlElement
    String status;
}
