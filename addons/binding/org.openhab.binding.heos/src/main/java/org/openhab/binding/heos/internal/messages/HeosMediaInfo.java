package org.openhab.binding.heos.internal.messages;

public class HeosMediaInfo {
    String type;
    String song;
    String station;
    String album;
    String artist;
    String image_url;
    String album_id;
    String mid;
    String qid;
    String sid;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSong() {
        return song;
    }

    public void setSong(String song) {
        this.song = song;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getQid() {
        return qid;
    }

    public void setQid(String qid) {
        this.qid = qid;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(String album_id) {
        this.album_id = album_id;
    }

    @Override
    public String toString() {
        return "HeosMediaInfo [" + (type != null ? "type=" + type + ", " : "")
                + (song != null ? "song=" + song + ", " : "") + (station != null ? "station=" + station + ", " : "")
                + (album != null ? "album=" + album + ", " : "") + (artist != null ? "artist=" + artist + ", " : "")
                + (image_url != null ? "image_url=" + image_url + ", " : "")
                + (album_id != null ? "album_id=" + album_id + ", " : "") + (mid != null ? "mid=" + mid + ", " : "")
                + (qid != null ? "qid=" + qid + ", " : "") + (sid != null ? "sid=" + sid : "") + "]";
    }

}
