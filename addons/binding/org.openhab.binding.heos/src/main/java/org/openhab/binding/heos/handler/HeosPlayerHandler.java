/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos.handler;

import static org.openhab.binding.heos.HeosBindingConstants.*;
import static org.openhab.binding.heos.internal.HeosCommand.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryListener;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.heos.HeosBindingConstants;
import org.openhab.binding.heos.internal.HeosCommand;
import org.openhab.binding.heos.internal.messages.HeosMediaInfo;
import org.openhab.binding.heos.internal.messages.HeosMessage;
import org.openhab.binding.heos.internal.messages.HeosMusic;
import org.openhab.binding.heos.internal.messages.HeosPlayer;
import org.openhab.binding.heos.internal.messages.HeosPlayerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link HeosPlayerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 *
 */
public class HeosPlayerHandler extends BaseThingHandler implements DiscoveryListener, DeviceStatusListener {

    private Logger logger = LoggerFactory.getLogger(HeosPlayerHandler.class);

    private Map<String, String> stateMap = Collections.synchronizedMap(new HashMap<String, String>());

    private String pid;
    private String coordinator;
    private HeosBridgeHandler bridgeHandler = null;
    private Gson gson = new Gson();

    public HeosPlayerHandler(Thing thing, String pid) {
        super(thing);
        this.pid = pid;

        logger.debug("Creating a HeosPlayerHandler for thing '{}'", getThing().getUID());

    }

    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
    }

    @Override
    public void initialize() {

        Configuration configuration = getConfig();

        if (configuration.get("pid") != null) {
            onUpdate();
        } else {
            logger.warn("Cannot initalize the HeosPlayer. PID not set.");
        }
    }

    @Override

    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("HeosPlayer {} bridgeStatusChanged {}", pid, bridgeStatusInfo);
        if (bridgeStatusInfo.getStatus().equals(ThingStatus.ONLINE)) {
            try {
                Thing bridge = getBridge();
                ThingHandler handler = bridge.getHandler();
                HeosBridgeHandler heosHandler = (HeosBridgeHandler) handler;
                logger.debug("Init bridge for {}, bridge:{}", pid, heosHandler);
                if (heosHandler != null) {
                    this.bridgeHandler = heosHandler;
                    this.bridgeHandler.registerDeviceStatusListener(this);
                    Configuration config = editConfiguration();
                    HeosPlayer dev = bridgeHandler.getPlayer(pid);

                    if (dev != null) {
                        if (dev.getVersion() != null) {
                            config.put(HeosBindingConstants.PLAYER_VERSION, dev.getVersion());
                        }
                        if (dev.getModel() != null) {
                            config.put(HeosBindingConstants.PLAYER_MODEL, dev.getModel());
                        }
                        updateConfiguration(config);
                        updateStatus(ThingStatus.ONLINE);
                    } else {
                        logger.warn("Could not find {}, it is turned on", pid);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                    }

                }
            } catch (Exception e) {
                logger.error("Failed to init bridge for " + pid, e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    private void onUpdate() {
        if (getBridge() != null && getBridge().getStatus() == ThingStatus.ONLINE) {
            bridgeStatusChanged(getBridge().getStatusInfo());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Refresh command received. {}", channelUID);
            try {
                switch (channelUID.getId()) {
                    case CONTROL:
                        updatePlayerState();
                    case CURRENTALBUM:
                        updateMediaInfo();
                }
            } catch (IOException e) {
                logger.error("Failed to update state", e);
            }

        } else {
            switch (channelUID.getId()) {
                case LED:
                    this.setLed(command);
                    break;
                case MUTE:
                    this.setMute(command);
                    break;
                case STOP:
                    stop();
                    break;
                case VOLUME:
                    setVolume(command);
                    break;
                case ADD:
                    addMember(command);
                    break;
                case REMOVE:
                    removeMember(command);
                    break;
                case STANDALONE:
                    becomeStandAlonePlayer();
                    break;
                case PUBLICADDRESS:
                    publicAddress();
                    break;
                case RADIO:
                    playRadio(command);
                    break;
                case MID:
                    // playRadio(command);
                    break;
                case FAVORITE:
                    playFavorite(command);
                    break;
                case PLAYLIST:
                    playPlayList(command);
                    break;
                case PLAYQUEUE:
                    playQueue(command);
                    break;
                case PLAYTRACK:
                    playTrack(command);
                    break;
                case PLAYURI:
                    playURI(command);
                    break;
                case PLAYLINEIN:
                    playLineIn(command);
                    break;
                case CONTROL:
                    if (command instanceof PlayPauseType) {
                        if (command == PlayPauseType.PLAY) {
                            play();
                        } else if (command == PlayPauseType.PAUSE) {
                            pause();
                        }
                    }
                    if (command instanceof NextPreviousType) {
                        if (command == NextPreviousType.NEXT) {
                            next();
                        } else if (command == NextPreviousType.PREVIOUS) {
                            previous();
                        }
                    }
                    if (command instanceof RewindFastforwardType) {
                        // Rewind and Fast Forward are currently not implemented by the binding
                    }
                    break;
                default:
                    break;

            }

        }
    }

    protected void updateCurrentZoneName(HeosPlayerGroup group, HeosPlayerGroup.GroupMember member) {
        stateMap.put(ZONENAME, group.getName());
        updateState(ZONENAME, createStringType(group.getName()));
        coordinator = group.getCoordinator().getPid();
    }

    protected void updateLed() {

    }

    protected void updateTime() {

    }

    protected void updatePosition() {

    }

    protected void updateRunningAlarmProperties() {

    }

    protected void updateZoneInfo() {

    }

    public String getCoordinator() {
        if (coordinator == null) {
            return pid;
        } else {
            return coordinator;
        }
    }

    public boolean isCoordinator() {
        return false;
    }

    protected void updateTrackMetaData() {

    }

    protected void updateCurrentURIFormatted(String URI) {

    }

    public boolean isGroupCoordinator() {

        return false;

    }

    public String getPosition() {
        updatePosition();
        return stateMap.get("cur_pos");
    }

    public long getCurrenTrackNr() {
        updatePosition();
        String value = stateMap.get("Track");
        if (value != null) {
            return Long.valueOf(value);
        } else {
            return -1;
        }
    }

    public String getVolume() {
        return stateMap.get("level");
    }

    public List<HeosMusic> getFavorites() {
        return bridgeHandler.getFavorites();
    }

    protected List<HeosPlayer> getEntries(String type, String filter) {
        return null;
    }

    public void setVolume(Command command) {
        if (command != null) {
            if (command instanceof OnOffType || command instanceof IncreaseDecreaseType
                    || command instanceof DecimalType || command instanceof PercentType) {

                String newValue = null;
                if (command instanceof IncreaseDecreaseType && command == IncreaseDecreaseType.INCREASE) {
                    int i = Integer.valueOf(this.getVolume());
                    newValue = String.valueOf(Math.min(100, i + 1));
                } else if (command instanceof IncreaseDecreaseType && command == IncreaseDecreaseType.DECREASE) {
                    int i = Integer.valueOf(this.getVolume());
                    newValue = String.valueOf(Math.max(0, i - 1));
                } else if (command instanceof OnOffType && command == OnOffType.ON) {
                    newValue = "100";
                } else if (command instanceof OnOffType && command == OnOffType.OFF) {
                    newValue = "0";
                } else if (command instanceof DecimalType) {
                    newValue = command.toString();
                } else {
                    return;
                }
                sendCommand(PLAYER_SET_VOLUME, "level=" + newValue);

            }
        }
    }

    public void addURIToQueue(String URI, String meta, int desiredFirstTrack, boolean enqueueAsNext) {

    }

    public void setCurrentURI(HeosPlayer newEntry) {
    }

    public void setCurrentURI(String URI, String URIMetaData) {

    }

    public void setPosition(String relTime) {
        seek("REL_TIME", relTime);
    }

    public void setPositionTrack(long tracknr) {
        seek("TRACK_NR", Long.toString(tracknr));
    }

    public void setPositionTrack(String tracknr) {
        seek("TRACK_NR", tracknr);
    }

    protected void seek(String unit, String target) {

    }

    public void play() {
        sendCommand(PLAYER_SET_STATE, "state=play");
    }

    public void stop() {
        sendCommand(PLAYER_SET_STATE, "state=stop");
    }

    private void sendCommand(HeosCommand playerSetState, String params) {
        String fullParams = "pid=" + pid;
        if (params != null) {
            fullParams = fullParams + "&" + params;
        }
        bridgeHandler.sendCommand(playerSetState, fullParams);

    }

    public void pause() {
        sendCommand(PLAYER_SET_STATE, "state=pause");
    }

    /**
     * Clear all scheduled music from the current queue.
     *
     */
    public void removeAllTracksFromQueue() {

    }

    /**
     * Play music from the line-in of the given Player referenced by the given UDN or name
     *
     * @param udn or name
     */
    public void playLineIn(Command command) {

    }

    protected HeosPlayerHandler getHandlerByName(String remotePlayerName) {
        return bridgeHandler.getPlayerHandler(remotePlayerName);
    }

    public void setMute(Command command) {
        if (command != null) {
            if (command instanceof OnOffType || command instanceof OpenClosedType || command instanceof UpDownType) {

                Map<String, String> inputs = new HashMap<String, String>();
                inputs.put("Channel", "Master");

                if (command.equals(OnOffType.ON) || command.equals(UpDownType.UP)
                        || command.equals(OpenClosedType.OPEN)) {
                    inputs.put("DesiredMute", "True");
                } else if (command.equals(OnOffType.OFF) || command.equals(UpDownType.DOWN)
                        || command.equals(OpenClosedType.CLOSED)) {
                    inputs.put("DesiredMute", "False");

                }

            }
        }
    }

    public String getTime() {
        updateTime();
        return stateMap.get("CurrentLocalTime");
    }

    public Boolean isLineInConnected() {
        return stateMap.get("LineInConnected").equals("1") ? true : false;
    }

    public void becomeStandAlonePlayer() {
        sendCommand(GROUP_SET_GROUP, "pid=" + getCoordinator());
    }

    public void addMember(Command command) {
        sendCommand(GROUP_SET_GROUP, "pid=" + getCoordinator() + "," + pid + "," + command.toString());
    }

    public boolean publicAddress() {
        return false;
    }

    /**
     * Play a given url to music in one of the music libraries.
     *
     * @param url
     *            in the format of //host/folder/filename.mp3
     */
    public void playURI(Command command) {

        if (command != null && command instanceof StringType) {

            String url = command.toString();

            HeosPlayerHandler coordinator = getHandlerByName(getCoordinator());

            // stop whatever is currently playing
            coordinator.stop();

            coordinator.playURL(url);
        }

    }

    private void playURL(String url) {
        // TODO Auto-generated method stub
        sendCommand(BROWSE_PLAY_STREAM, "url=" + url);
    }

    public void playQueue(Command command) {
        // HeosPlayerHandler coordinator = getHandlerByName(getCoordinator());

    }

    public void setLed(Command command) {
        if (command != null) {
            if (command instanceof OnOffType || command instanceof OpenClosedType || command instanceof UpDownType) {

                Map<String, String> inputs = new HashMap<String, String>();

                if (command.equals(OnOffType.ON) || command.equals(UpDownType.UP)
                        || command.equals(OpenClosedType.OPEN)) {
                    inputs.put("DesiredLEDState", "On");
                } else if (command.equals(OnOffType.OFF) || command.equals(UpDownType.DOWN)
                        || command.equals(OpenClosedType.CLOSED)) {
                    inputs.put("DesiredLEDState", "Off");

                }

            }
        }
    }

    public void removeMember(Command command) {
        // if (command != null && command instanceof StringType) {
        // HeosPlayerHandler oldmemberHandler = getHandlerByName(command.toString());
        //
        // }
    }

    public void previous() {
        sendCommand(PLAYER_PREV, null);
    }

    public void next() {
        sendCommand(PLAYER_NEXT, null);
    }

    public void playRadio(Command command) {
        List<HeosMusic> stations = getFavorites();
        if (stations != null) {
            for (HeosMusic music : stations) {
                if (music.getName().contains(command.toString())) {
                    getHandlerByName(getCoordinator()).playMid(getFavorutesSID(), music.getMid());
                    break;
                }
            }
        }
    }

    public void playMid(String sid, String mid) {
        sendCommand(BROWSE_PLAY_STREAM, "sid=" + sid + "&mid=" + mid);
    }

    private String getFavorutesSID() {
        return bridgeHandler.getFavoritesSID();
    }

    /**
     * This will attempt to match the station string with a entry in the
     * favorites list, this supports both single entries and playlists
     *
     * @param favorite to match
     * @return true if a match was found and played.
     */
    public void playFavorite(Command command) {

    }

    public void playTrack(Command command) {

        if (command != null && command instanceof DecimalType) {
            HeosPlayerHandler coordinator = getHandlerByName(getCoordinator());

            String trackNumber = command.toString();

            // seek the track - warning, we do not check if the tracknumber falls in the boundary of the queue
            setPositionTrack(trackNumber);

            // take the system off mute
            coordinator.setMute(OnOffType.OFF);

            // start jammin'
            coordinator.play();
        }

    }

    public void playPlayList(Command command) {

    }

    public void addURIToQueue(HeosPlayer newEntry) {
    }

    public String getZoneName() {
        return stateMap.get(ZONENAME);
    }

    public String getZoneGroupID() {
        return stateMap.get("LocalGroupUUID");
    }

    public String getMute() {
        return stateMap.get("mute");
    }

    public boolean getLed() {
        return stateMap.get("CurrentLEDState").equals("On") ? true : false;
    }

    public String getCurrentZoneName() {
        return stateMap.get(ZONENAME);
    }

    @Override
    public Collection<ThingUID> removeOlderResults(DiscoveryService source, long timestamp,
            Collection<ThingTypeUID> thingTypeUIDs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void thingDiscovered(DiscoveryService source, DiscoveryResult result) {
        // TODO Auto-generated method stub

    }

    @Override
    public void thingRemoved(DiscoveryService source, ThingUID thingUID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeviceStateChanged(Bridge bridge, HeosPlayer device) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeviceRemoved(Bridge bridge, HeosPlayer device) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeviceAdded(Bridge bridge, HeosPlayer device) {
        // TODO Auto-generated method stub

    }

    public void updateReceived(HeosMessage message) {
        // TODO Auto-generated method stub

        logger.trace("updateReceived in Player {}", message);
        try {
            if (message.getHeos().getCommand() != null) {
                switch (message.getHeos().getCommand()) {
                    case PLAYER_MEDIA:
                        updateMediaInfo(message);
                        break;
                    case PLAYER_GET_STATE:
                    case PLAYER_GET_VOLUME:
                    case PLAYER_SET_STATE:
                    case PLAYER_SET_VOLUME:
                        handleEvent(message);
                        break;
                    case EVENT_NOW_PLAYING_CHANGED:
                        updateMediaInfo();
                        break;
                    case EVENT_PLAYER_PROGRESS:
                        // IGNORE
                        break;
                    case EVENT_STATECHANGE:
                    case EVENT_PLAYER_VOLUME_CHANGED:
                    case EVENT_REPEAT_MODE_CHANGED:
                    case EVENT_SHUFFLE_MODE_CHANGE:
                    case EVENT_PLAYBACK_ERROR:
                        handleEvent(message);
                        break;
                    default:
                        logger.error("Unhandled command {} ", message.getHeos().getCommand());
                }
            } else {
                logger.debug("No command, {}", message);
            }
        } catch (IOException e) {
            logger.error("Failed to update", e);
        }
    }

    // private void handleShortReply(HeosMessage message) {
    // if (message.getPayload() != null) {
    // JsonObject obj = message.getPayload().getAsJsonObject();
    // HashMap<String, String> list = new HashMap<String, String>();
    // for (Entry<String, JsonElement> entry : obj.entrySet()) {
    // list.put(entry.getKey(), entry.getValue().getAsString());
    // }
    // handleStateChanges(list);
    // }
    //
    // }

    public static Map<String, String> splitQuery(String url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    private void handleEvent(HeosMessage message) {
        try {
            Map<String, String> params = splitQuery(message.getHeos().getMessage());
            handleStateChanges(params);

        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to parse params", e);
        }

    }

    private void handleStateChanges(Map<String, String> params) {
        if (params.containsKey("state")) {
            updatePlayState((params.get("state")));
        }
        if (params.containsKey("level")) {
            updateVolume((params.get("level")));
        }
        if (params.containsKey("error")) {
            updateState(CURRENTTITLE, createStringType(params.get("error")));
        }
        stateMap.putAll(params);
    }

    private void updateVolume(String string) {
        updateState(VOLUME, new PercentType(string));
    }

    private void updatePlayState(String string) {
        PlayPauseType state = PlayPauseType.PAUSE;
        if (string.equals("play")) {
            state = PlayPauseType.PLAY;
        }
        updateState(STATE, state);
        updateState(CONTROL, state);
    }

    protected void updateMediaInfo() throws IOException {
        sendCommand(PLAYER_MEDIA, null);
    }

    protected void updatePlayerState() throws IOException {
        sendCommand(PLAYER_GET_STATE, null);
        sendCommand(PLAYER_GET_VOLUME, null);
    }

    public void updateMediaInfo(HeosMessage message) {
        HeosMediaInfo mediaInfo = gson.fromJson(message.getPayload(), HeosMediaInfo.class);
        if (mediaInfo != null) {
            updateStatus(ThingStatus.ONLINE);
            logger.debug("Update MediaInfo for {} to {} msg {}", getThing().getUID().getId(), mediaInfo,
                    message.getHeos());
            updateState(CURRENTALBUM, createStringType(mediaInfo.getAlbum()));
            updateState(CURRENTARTIST, createStringType(mediaInfo.getArtist()));
            updateState(CURRENTTITLE, createStringType(mediaInfo.getSong()));
            updateState(RADIO, createStringType(mediaInfo.getStation()));
            updateState(MID, createStringType(mediaInfo.getMid()));
            updateState(SID, createStringType(mediaInfo.getSid()));
        }
    }

    private StringType createStringType(String valueAsString) {
        return new StringType(valueAsString != null ? valueAsString : "");
    }

    public void playNotificationSoundURI(StringType stringType) {
        playURI(stringType);
    }
}
