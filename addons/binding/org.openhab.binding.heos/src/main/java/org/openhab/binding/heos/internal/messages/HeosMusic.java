package org.openhab.binding.heos.internal.messages;

public class HeosMusic {
    String container;
    String playable;
    String type;
    String name;
    String image_url;
    String cid;
    String mid;

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getPlayable() {
        return playable;
    }

    public void setPlayable(String playable) {
        this.playable = playable;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    @Override
    public String toString() {
        return "HeosMusic [" + (container != null ? "container=" + container + ", " : "")
                + (playable != null ? "playable=" + playable + ", " : "") + (type != null ? "type=" + type + ", " : "")
                + (name != null ? "name=" + name + ", " : "")
                + (image_url != null ? "image_url=" + image_url + ", " : "") + (cid != null ? "cid=" + cid + ", " : "")
                + (mid != null ? "mid=" + mid : "") + "]";
    }
}
