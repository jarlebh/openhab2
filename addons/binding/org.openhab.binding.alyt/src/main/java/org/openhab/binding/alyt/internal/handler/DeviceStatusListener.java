/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt.internal.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.alyt.devices.AlytDevice;

/**
 * The {@link DeviceStatusListener} is notified when a device status has changed
 * or a device has been removed or added.
 * 
 * @author Marcel Verpaalen - Initial contribution
 *
 */
public interface DeviceStatusListener {

	/**
	 * This method is called whenever the state of the given device has changed.
	 * The new state can be obtained by {@link FullLight#getState()}.
	 * 
	 * @param bridge
	 *            The ALYT Cube bridge the changed device is connected to.
	 * @param device
	 *            The device which received the state update.
	 */
	public void onDeviceStateChanged(ThingUID bridge, AlytDevice device);

	/**
	 * This method us called whenever a device is removed.
	 * 
	 * @param bridge
	 *            The ALYT Cube bridge the removed device was connected to.
	 * @param device
	 *            The device which is removed.
	 */
	public void onDeviceRemoved(AlytBridgeHandler bridge, AlytDevice device);

	/**
	 * This method us called whenever a device is added.
	 * 
	 * @param bridge
	 *            The ALYT Cube bridge the added device was connected to.
	 * @param device
	 *            The device which is added.
	 */
	public void onDeviceAdded(Bridge bridge, AlytDevice device);

}
