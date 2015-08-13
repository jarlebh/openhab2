/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt.config;

/**
 * Configuration class for {@link MaxBinding} bridge used to connect to the
 * maxCube device.
 * 
 * @author Marcel Verpaalen - Initial contribution
 */

public class AlytBridgeConfiguration {

	/** The IP address of the ALYTCube LAN gateway */
	public String ipAddress;

	/**
	 * The port of the ALYT Cube LAN gateway as provided at
	 * http://www.elv.de/controller.aspx?cid=824&detail=10&detail2=3484
	 */
	public Integer port;

	/** The refresh interval in ms which is used to poll given ALYT Cube */
	public Integer refreshInterval;

	/** The unique serial number for a device */
	public String id;

	public String password;

}
