/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link HeosBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author jarlebh - Initial contribution
 */
public class HeosBindingConstants {

    public static final String BINDING_ID = "heos";

    // List of all Thing Type UIDs
    public final static ThingTypeUID HEOSPLAYER_THING_TYPE_UID = new ThingTypeUID(BINDING_ID, "heosplayer");
    public final static ThingTypeUID HEOSBRIDGE_THING_TYPE_UID = new ThingTypeUID(BINDING_ID, "heosbridge");
    // List of all Channel ids
    public final static String ADD = "add";
    public final static String ALARM = "alarm";
    public final static String ALARMPROPERTIES = "alarmproperties";
    public final static String ALARMRUNNING = "alarmrunning";
    public final static String CONTROL = "control";
    public final static String CURRENTALBUM = "currentalbum";
    public final static String CURRENTARTIST = "currentartist";
    public final static String CURRENTTITLE = "currenttitle";
    public final static String CURRENTTRACK = "currenttrack";
    public final static String MID = "mediaid";
    public final static String SID = "sourceid";
    public final static String FAVORITE = "favorite";
    public final static String LED = "led";
    public final static String LINEIN = "linein";
    public final static String LOCALCOORDINATOR = "localcoordinator";
    public final static String MUTE = "mute";
    public final static String PLAYLINEIN = "playlinein";
    public final static String PLAYLIST = "playlist";
    public final static String PLAYQUEUE = "playqueue";
    public final static String PLAYTRACK = "playtrack";
    public final static String PLAYURI = "playuri";
    public final static String PUBLICADDRESS = "publicaddress";
    public final static String RADIO = "radio";
    public final static String REMOVE = "remove";
    public final static String RESTORE = "restore";
    public final static String RESTOREALL = "restoreall";
    public final static String SAVE = "save";
    public final static String SAVEALL = "saveall";
    public final static String SNOOZE = "snooze";
    public final static String STANDALONE = "standalone";
    public final static String STATE = "state";
    public final static String STOP = "stop";
    public final static String VOLUME = "volume";
    public final static String ZONEGROUP = "zonegroup";
    public final static String ZONEGROUPID = "zonegroupid";
    public final static String ZONENAME = "zonename";

    public final static String PLAYER_PID = "pid";
    public final static String PLAYER_MODEL = "model";
    public final static String PLAYER_VERSION = "version";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

}
