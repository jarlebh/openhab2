package org.openhab.binding.tellstick.handler;

import org.junit.Test;

public class TellstickBridgeHandlerTest {

	@Test
	public void testGetDevice() {
		TellstickBridgeHandler handler = new TellstickBridgeHandler(null);
		handler.initialize();
	}

}
