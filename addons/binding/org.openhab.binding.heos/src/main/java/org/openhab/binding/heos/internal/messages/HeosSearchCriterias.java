package org.openhab.binding.heos.internal.messages;

public class HeosSearchCriterias {
    String name;
    String scid;
    String wildcard;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScid() {
        return scid;
    }

    public void setScid(String scid) {
        this.scid = scid;
    }

    public String getWildcard() {
        return wildcard;
    }

    public void setWildcard(String wildcard) {
        this.wildcard = wildcard;
    }

    @Override
    public String toString() {
        return "HeosSearchCriterias [" + (name != null ? "name=" + name + ", " : "")
                + (scid != null ? "scid=" + scid + ", " : "") + (wildcard != null ? "wildcard=" + wildcard : "") + "]";
    }
}
