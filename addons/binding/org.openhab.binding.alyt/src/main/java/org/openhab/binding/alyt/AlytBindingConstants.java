/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alyt;

import java.util.Collection;
import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * The {@link AlytBinding} class defines common constants, which are 
 * used across the whole binding.
 * 
 * @author Jarle Hjortland - Initial contribution
 */
public class AlytBindingConstants {

    public static final String BINDING_ID = "alyt";
    public static final String BRIDGE_ALYTHUB = "bridge";
    public static final String IP_ADDRESS = "ipAddress";
	public static final String ALYT_ID = "id";
	 public static final String DEVICE_SWITCH = "switch";
	 public static final String CHANNEL_STATE = "state";
	// List of all Thing Type UIDs
    
	 public final static ThingTypeUID SWITCH_THING_TYPE = new ThingTypeUID(BINDING_ID, DEVICE_SWITCH);
	 public final static ThingTypeUID ALYTBRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, BRIDGE_ALYTHUB);
    
    public final static Collection<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Lists.newArrayList(
    		/*HEATINGTHERMOSTAT_THING_TYPE, HEATINGTHERMOSTATPLUS_THING_TYPE, WALLTHERMOSTAT_THING_TYPE, */
    		SWITCH_THING_TYPE, ALYTBRIDGE_THING_TYPE);

    public final static Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS =ImmutableSet.of(
    		/*HEATINGTHERMOSTAT_THING_TYPE, HEATINGTHERMOSTATPLUS_THING_TYPE, WALLTHERMOSTAT_THING_TYPE, */
    		SWITCH_THING_TYPE);

    
    public final static Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS =ImmutableSet.of(
    		ALYTBRIDGE_THING_TYPE);
	
}
