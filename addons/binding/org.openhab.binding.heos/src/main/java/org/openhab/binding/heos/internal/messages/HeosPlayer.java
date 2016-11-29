/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos.internal.messages;

import java.io.Serializable;

import org.apache.commons.lang.StringEscapeUtils;
import org.openhab.binding.heos.internal.HeosResourceMetaData;

/**
 * The {@link HeosPlayer} is a datastructure to describe
 * multimedia "entries" in the Sonos ecosystem
 *
 * @author Karel Goderis - Initial contribution
 */
public class HeosPlayer implements Serializable {

    private static final long serialVersionUID = -4543607156929701588L;
    private final String pid;
    private final String name;
    private final String parentId;
    private final String upnpClass;
    private final String res;
    private final String album;
    private final String albumArtUri;
    private final String creator;
    private final String version;

    public String getVersion() {
        return version;
    }

    public String getModel() {
        return model;
    }

    private final String model;
    private final int originalTrackNumber;
    private final HeosResourceMetaData resourceMetaData;

    public HeosPlayer(String pid, String name, String parentId, String album, String albumArtUri, String creator,
            String upnpClass, String res) {
        this(pid, name, parentId, album, albumArtUri, creator, upnpClass, res, -1);
    }

    public HeosPlayer(String pid, String name, String parentId, String album, String albumArtUri, String creator,
            String upnpClass, String res, int originalTrackNumber) {
        this(pid, name, parentId, album, albumArtUri, creator, upnpClass, res, originalTrackNumber, null, null, null);
    }

    public HeosPlayer(String pid, String name, String parentId, String album, String albumArtUri, String creator,
            String upnpClass, String res, int originalTrackNumber, HeosResourceMetaData resourceMetaData, String model,
            String version) {
        this.pid = pid;
        this.name = name;
        this.parentId = parentId;
        this.album = album;
        this.albumArtUri = albumArtUri;
        this.creator = creator;
        this.upnpClass = upnpClass;
        this.res = res;
        this.originalTrackNumber = originalTrackNumber;
        this.resourceMetaData = resourceMetaData;
        this.model = model;
        this.version = version;
    }

    /**
     * @return the title of the entry.
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * @return the unique identifier of this entry.
     */
    public String getPId() {
        return pid;
    }

    /**
     * @return the title of the entry.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the unique identifier of the parent of this entry.
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * @return a URI of this entry.
     */
    public String getRes() {
        return res;
    }

    /**
     * @return the UPnP classname for this entry.
     */
    public String getUpnpClass() {
        return upnpClass;
    }

    /**
     * @return the name of the album.
     */
    public String getAlbum() {
        return album;
    }

    /**
     * @return the URI for the album art.
     */
    public String getAlbumArtUri() {
        return StringEscapeUtils.unescapeXml(albumArtUri);
    }

    /**
     * @return the name of the artist who created the entry.
     */
    public String getCreator() {
        return creator;
    }

    public int getOriginalTrackNumber() {
        return originalTrackNumber;
    }

    /**
     * The resourceMetaData field from the ResMD parent, this will be login info for
     * streaming accounts to use in favorites
     *
     * @return
     */
    public HeosResourceMetaData getResourceMetaData() {
        return resourceMetaData;
    }
}
