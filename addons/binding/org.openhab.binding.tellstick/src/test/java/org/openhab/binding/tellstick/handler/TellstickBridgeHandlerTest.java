package org.openhab.binding.tellstick.handler;

import org.junit.Test;
import org.openhab.binding.tellstick.handler.core.TelldusCoreBridgeHandler;

public class TellstickBridgeHandlerTest {

    @Test
    public void testGetDevice() {
        TelldusCoreBridgeHandler handler = new TelldusCoreBridgeHandler(null);
        handler.initialize();
    }

}
