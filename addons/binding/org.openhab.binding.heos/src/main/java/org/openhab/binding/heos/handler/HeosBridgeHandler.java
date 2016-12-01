package org.openhab.binding.heos.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.heos.HeosBindingConstants;
import org.openhab.binding.heos.internal.HeosCommand;
import org.openhab.binding.heos.internal.HeosListener;
import org.openhab.binding.heos.internal.HeosUpdateReceivedCallback;
import org.openhab.binding.heos.internal.messages.HeosMessage;
import org.openhab.binding.heos.internal.messages.HeosMusic;
import org.openhab.binding.heos.internal.messages.HeosMusicSource;
import org.openhab.binding.heos.internal.messages.HeosPlayer;
import org.openhab.binding.heos.internal.messages.HeosPlayerGroup;
import org.openhab.binding.heos.internal.messages.HeosPlayerGroup.GroupMember;
import org.openhab.binding.heos.internal.messages.HeosSearchCriterias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class HeosBridgeHandler extends BaseBridgeHandler implements DeviceStatusListener, HeosUpdateReceivedCallback {
    private static final String FAVORITES = "Favorites";
    private static final String IP_ADDRESS = "ipAddress";
    private List<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArrayList<>();
    private Map<String, HeosPlayer> players = null;
    private Logger logger = LoggerFactory.getLogger(HeosBridgeHandler.class);
    private HeosListener listener = null;
    private Gson gson = new Gson();
    private HeosMusicSource[] musicSources = null;
    private HeosSearchCriterias[] radioSearchCriterias = null;
    private Map<String, HeosMusic[]> musicList = null;
    private boolean loggedIn = false;
    private Long lastScan = 0L;
    private HeosPlayerGroup[] groups;
    private ScheduledFuture<?> pollingJob;
    private ScheduledFuture<?> sendJob;

    public HeosBridgeHandler(Bridge bridge) {
        super(bridge);
        this.players = new HashMap<String, HeosPlayer>();
        this.musicList = new HashMap<String, HeosMusic[]>();
    }

    @Override
    public void initialize() {
        setupListener();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Refresh command received.");
            rescanHeosPlayers();
        } else {
            logger.warn("No bridge commands defined.");
        }
    }

    public void rescanHeosPlayers() {
        try {
            if ((System.currentTimeMillis() - lastScan) > (10 * 1000)) {
                lastScan = System.currentTimeMillis();
                if (!loggedIn) {
                    String userName = (String) getConfig().get(HeosBindingConstants.USERNAME);
                    String password = (String) getConfig().get(HeosBindingConstants.PASSWORD);
                    if (userName != null && password != null) {
                        sendCommand(HeosCommand.SYSTEM_SIGNIN, "un=" + userName + "&pw=" + password);
                    }
                }
                sendCommand(HeosCommand.GETPLAYERS, null);
                sendCommand(HeosCommand.PLAYER_GET_GROUPS, null);
            }
        } catch (Exception e) {
            logger.error("Failed to get devices", e);
        }

    }

    public void sendCommand(HeosCommand command, String param) {
        try {
            listener.sendCommand(command, param);
        } catch (IOException e) {
            logger.error("Failed to send command", e);
            throw new RuntimeException("Failed to send command", e);
        }
    }

    @Override
    public void onDeviceStateChanged(Bridge bridge, HeosPlayer device) {
        logger.debug("onDeviceStateChanged {} {}", bridge, device);
    }

    @Override
    public void onDeviceRemoved(Bridge bridge, HeosPlayer device) {
        logger.debug("onDeviceRemoved {} {}", bridge, device);
    }

    @Override
    public void onDeviceAdded(Bridge bridge, HeosPlayer device) {
        logger.debug("onDeviceAdded {} {}", bridge, device);

    }

    public void registerDeviceStatusListener(DeviceStatusListener heosPlayerHandler) {
        logger.debug("registerDeviceStatusListener {}", heosPlayerHandler);
        deviceStatusListeners.add(heosPlayerHandler);
    }

    public HeosPlayer getPlayer(String pid) {
        return players.get(pid);
    }

    @Override
    public void updateReceived(HeosMessage message) {
        if (message.getHeos().getCommand() != null) {
            if (message.getHeos().getMessage() != null && message.getHeos().getMessage().contains("pid")) {
                updatePlayer(message);
            } else if (message.getHeos().getMessage() != null && message.getHeos().getMessage().contains("gid")) {
                updateGroup(message);
            } else {
                switch (message.getHeos().getCommand()) {
                    case GETPLAYERS:
                        updatePlayersList(message);
                        break;
                    case BROWSE_MUSIC_SOURCES:
                        updateMusicSources(message);
                        break;
                    case BROWSE_SEARCH_CRITERIA:
                        updateSearchCriterias(message);
                        break;
                    case BROWSE_BROWSE:
                        updateMediaList(message);
                        break;
                    case SYSTEM_SIGNIN:
                        handleLogin(message);
                        break;
                    case PLAYER_GET_GROUPS:
                        updateGroup(message);
                        break;
                    case EVENT_GROUPS_CHANGED:

                    case EVENT_PLAYERS_CHANGED:
                    case EVENT_SOURCES_CHANGED:
                        rescanHeosPlayers();
                        break;
                    case SYSTEM_HEARTBEAT:
                    case SYSTEM_REGISTER_CHANGEEVENTS:
                        // IGNORE
                        break;
                    default:
                        logger.error("unhandled command {} ", message.getHeos().getCommand());
                }
            }
        } else {
            logger.debug("No command, {}", message);
        }

    }

    private void updateGroup(HeosMessage message) {
        switch (message.getHeos().getCommand()) {
            case PLAYER_GET_GROUPS:
                updateGroups(message);
                break;
            case GROUP_SET_GROUP:
            default:
                logger.error("unhandled group command {} ", message.getHeos().getCommand());
        }
    }

    private void updateGroups(HeosMessage message) {
        groups = gson.fromJson(message.getPayload(), HeosPlayerGroup[].class);
        for (HeosPlayerGroup group : groups) {
            for (GroupMember member : group.getPlayers()) {
                HeosPlayerHandler handler = getPlayerHandler(member.getPid());
                if (handler != null) {
                    handler.updateCurrentZoneName(group, member);
                }
            }
        }
    }

    private void handleLogin(HeosMessage message) {
        String result = message.getHeos().getResult();
        if (result.equals("error")) {
            logger.error("Failed to login {}:{}", result, message.getHeos().getMessage());
        } else {
            loggedIn = true;
            sendCommand(HeosCommand.BROWSE_MUSIC_SOURCES, null);
        }
        logger.debug("Login {}:{}", result, message.getHeos().getMessage());
    }

    private void updateMediaList(HeosMessage message) {
        String sid = extractFromURL(message, "sid");
        if (message.getPayload() != null) {
            musicList.put(sid, gson.fromJson(message.getPayload(), HeosMusic[].class));
        }
    }

    public List<HeosMusic> getFavorites() {
        List<HeosMusic> result = null;
        if (!musicList.isEmpty()) {
            result = Arrays.asList(musicList.get(getFavoritesSID()));
        }
        return result;
    }

    public String getFavoritesSID() {
        String sid = null;
        for (HeosMusicSource source : this.musicSources) {
            if (source.getName().equals(FAVORITES)) {
                sid = source.getSid();
                break;
            }
        }
        return sid;
    }

    public HeosSearchCriterias[] getRadioSearchCriterias() {
        return radioSearchCriterias;
    }

    private void updateMusicSources(HeosMessage message) {
        musicSources = gson.fromJson(message.getPayload(), HeosMusicSource[].class);
        for (HeosMusicSource source : musicSources) {
            if (source.getName().equals("TuneIn")) {
                sendCommand(HeosCommand.BROWSE_SEARCH_CRITERIA, "sid=" + source.getSid());
            } else if (source.getName().equals(FAVORITES)) {
                sendCommand(HeosCommand.BROWSE_BROWSE, "sid=" + source.getSid());
            }
        }
    }

    private void updateSearchCriterias(HeosMessage message) {
        radioSearchCriterias = gson.fromJson(message.getPayload(), HeosSearchCriterias[].class);
    }

    private void updatePlayer(HeosMessage message) {
        logger.trace("updatePlayer, {}", message);
        String pid = extractFromURL(message, "pid");
        HeosPlayerHandler handler = getPlayerHandler(pid);
        if (handler != null) {
            handler.updateReceived(message);
        }
    }

    public HeosPlayerHandler getPlayerHandler(String pid) {
        HeosPlayerHandler res = null;
        for (Thing thing : getThing().getThings()) {
            if (thing.getUID().getId().equals(pid)) {
                res = ((HeosPlayerHandler) thing.getHandler());
                break;
            }
        }
        return res;
    }

    private String extractFromURL(HeosMessage message, String key) {
        String pid = message.getHeos().getMessage()
                .substring(message.getHeos().getMessage().indexOf(key + "=") + key.length() + 1);
        if (pid.indexOf("&") > 0) {
            pid = pid.substring(0, pid.indexOf("&")).replace("'", "");
        }
        return pid;
    }

    private void updatePlayersList(HeosMessage message) {
        HeosPlayer[] players = gson.fromJson(message.getPayload(), HeosPlayer[].class);
        if (players != null) {
            for (HeosPlayer player : players) {
                logger.debug("Player: " + player);
                if (this.players.get(player.getPId()) == null) {
                    this.players.put(player.getPId(), player);
                    for (DeviceStatusListener list : deviceStatusListeners) {
                        list.onDeviceAdded(getThing(), player);
                    }
                }

            }
            sendCommand(HeosCommand.SYSTEM_REGISTER_CHANGEEVENTS, "enable=on");
            updateStatus(ThingStatus.ONLINE);
        } else if (message.getHeos().getResult().contains("error")) {
            logger.error("Failed to get players", message);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    @Override
    public void listenerConnected() {
        logger.debug("listenerConnected");
        rescanHeosPlayers();
    }

    @Override
    public void listenerDisconnected() {
        logger.debug("listenerDisconnected");
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
    }

    public void unregisterDeviceStatusListener(DeviceStatusListener heosDiscoveryService) {
        deviceStatusListeners.remove(heosDiscoveryService);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#handleConfigurationUpdate(java.util.Map)
     */
    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        setupListener();
        super.handleConfigurationUpdate(configurationParameters);
    }

    private void setupListener() {
        String ip = (String) getConfig().get(IP_ADDRESS);
        if (listener == null) {
            listener = new HeosListener(ip, this);
        } else {
            logger.debug("New IP {} old IP {}", ip, listener.getIpAddrs());
            if (!(listener.getIpAddrs().contains(ip))) {
                listener.addIpAddr(ip);
            }
        }
        startAutomaticRefresh(100);
    }

    private synchronized void startAutomaticRefresh(long refreshInterval) {
        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleAtFixedRate(listener, 0, refreshInterval, TimeUnit.MILLISECONDS);
        }
        if (sendJob == null || sendJob.isCancelled()) {
            sendJob = scheduler.scheduleAtFixedRate(listener.sender, 0, refreshInterval + 1, TimeUnit.MILLISECONDS);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.BaseThingHandler#updateConfiguration(org.eclipse.smarthome.config.core.
     * Configuration)
     */
    @Override
    protected void updateConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub
        super.updateConfiguration(configuration);
    }

}