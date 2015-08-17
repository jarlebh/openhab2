package org.openhab.binding.tellstick.handler.live;

import org.junit.Test;

public class TelldusLiveHandlerTest {

    @Test
    public void testInitialize() {
        TelldusLiveHandler handler = new TelldusLiveHandler(null);
        handler.connectHttpClient("FEHUVEW84RAFR5SP22RABURUPHAFRUNU", "ZUXEVEGA9USTAZEWRETHAQUBUR69U6EF",
                "b45a435762b5106c6f08a6264d4f37be04eb1b902", "b66e00e0bc1e33db24b4c8f59774fc8d");
        handler.refreshDeviceList();
        handler.refreshDeviceList();
    }

}
