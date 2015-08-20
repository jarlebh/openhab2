package org.openhab.binding.tellstick.handler.live;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.junit.Test;
import org.tellstick.device.TellstickException;

public class TelldusLiveHandlerTest {

    @Test
    public void testInitialize() throws TellstickException {
        TelldusLiveHandler handler = new TelldusLiveHandler(null);

        ((TelldusLiveDeviceController) handler.getController()).connectHttpClient("FEHUVEW84RAFR5SP22RABURUPHAFRUNU",
                "ZUXEVEGA9USTAZEWRETHAQUBUR69U6EF", "b45a435762b5106c6f08a6264d4f37be04eb1b902",
                "b66e00e0bc1e33db24b4c8f59774fc8d");
        handler.refreshDeviceList();
        handler.refreshDeviceList();
        TellstickNetDevice dev = (TellstickNetDevice) handler.getDevice("848889");
        handler.getController().handleSendEvent(dev, 1, false, OnOffType.OFF);
        dev = (TellstickNetDevice) handler.getDevice("801334");
        handler.getController().handleSendEvent(dev, 1, false, new PercentType(86));
    }

}
