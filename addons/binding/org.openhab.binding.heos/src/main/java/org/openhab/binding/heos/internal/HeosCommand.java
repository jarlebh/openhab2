package org.openhab.binding.heos.internal;

import com.google.gson.annotations.SerializedName;

public enum HeosCommand {
    @SerializedName("player/get_players")
    GETPLAYERS("player", "get_players"),
    @SerializedName("player")
    PLAYER("player", ""),
    @SerializedName("player/get_now_playing_media")
    PLAYER_MEDIA("player", "get_now_playing_media"),
    @SerializedName("player/set_play_state")
    PLAYER_SET_STATE("player", "set_play_state"),
    @SerializedName("player/get_play_state")
    PLAYER_GET_STATE("player", "get_play_state"),
    @SerializedName("player/play_next")
    PLAYER_NEXT("player", "play_next"),
    @SerializedName("player/play_previous")
    PLAYER_PREV("player", "play_previous"),
    @SerializedName("player/set_volume")
    PLAYER_SET_VOLUME("player", "set_volume"),
    @SerializedName("player/get_volume")
    PLAYER_GET_VOLUME("player", "get_volume"),
    @SerializedName("browse/get_music_sources")
    BROWSE_MUSIC_SOURCES("browse", "get_music_sources"),
    @SerializedName("browse/search")
    BROWSE_SEARCH("browse", "search"),
    @SerializedName("browse/browse")
    BROWSE_BROWSE("browse", "browse"),
    @SerializedName("browse/get_search_criteria")
    BROWSE_SEARCH_CRITERIA("browse", "get_search_criteria"),
    @SerializedName("browse/play_stream")
    BROWSE_PLAY_STREAM("browse", "play_stream"),
    @SerializedName("system/register_for_change_events")
    SYSTEM_REGISTER_CHANGEEVENTS("system", "register_for_change_events"),
    @SerializedName("system/heart_beat")
    SYSTEM_HEARTBEAT("system", "heart_beat"),
    @SerializedName("system/sign_in")
    SYSTEM_SIGNIN("system", "sign_in"),
    @SerializedName("system/sign_out")
    SYSTEM_SIGNOUT("system", "sign_out"),
    @SerializedName("system/check_account")
    SYSTEM_CHECK_ACCOUNT("system", "check_account"),
    @SerializedName("event/player_state_changed")
    EVENT_STATECHANGE("event", "player_state_changed"),
    @SerializedName("event/player_playback_error")
    EVENT_PLAYBACK_ERROR("event", "player_playback_error"),
    @SerializedName("event/shuffle_mode_changed")
    EVENT_SHUFFLE_MODE_CHANGE("event", "shuffle_mode_changed"),
    @SerializedName("event/player_now_playing_changed")
    EVENT_NOW_PLAYING_CHANGED("event", "player_now_playing_changed"),
    @SerializedName("event/repeat_mode_changed")
    EVENT_REPEAT_MODE_CHANGED("event", "repeat_mode_changed"),
    @SerializedName("event/groups_changed")
    EVENT_GROUPS_CHANGED("event", "groups_changed"),
    @SerializedName("event/group_volume_changed")
    EVENT_GROUPS_VOLUME_CHANGED("event", "group_volume_changed"),
    @SerializedName("event/player_now_playing_progress")
    EVENT_PLAYER_PROGRESS("event", "player_now_playing_progress"),
    @SerializedName("event/player_volume_changed")
    EVENT_PLAYER_VOLUME_CHANGED("event", "player_volume_changed"),
    @SerializedName("event/player_queue_changed")
    EVENT_PLAYER_QUEU_CHANGED("event", "player_queue_changed"),
    @SerializedName("event/sources_changed")
    EVENT_SOURCES_CHANGED("event", "sources_changed"),
    @SerializedName("event/user_changed")
    EVENT_USER_CHANGED("event", "user_changed"),
    @SerializedName("event/players_changed")
    EVENT_PLAYERS_CHANGED("event", "players_changed"),
    @SerializedName("player/get_groups")
    PLAYER_GET_GROUPS("player", "get_groups"),
    @SerializedName("group/set_group")
    GROUP_SET_GROUP("group", "set_group");

    private String group;
    private String command;

    private HeosCommand(String group, String command) {
        this.group = group;
        this.command = command;
    }

    public static HeosCommand toCommand(String command) {
        HeosCommand result = null;
        String[] parts = command.split("/");
        for (HeosCommand com : HeosCommand.values()) {
            if (com.getGroup().equals(parts[0]) && com.getCommand().equals(parts[1])) {
                result = com;
                break;
            }
        }
        return result;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "COMMAND:" + getGroup() + "/" + getCommand();
    }
}
