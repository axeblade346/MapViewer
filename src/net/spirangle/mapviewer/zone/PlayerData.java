package net.spirangle.mapviewer.zone;

import com.wurmonline.server.support.JSONObject;

public class PlayerData {
    private final long wurmId;
    private final String name;
    private final JSONObject json;

    public PlayerData(final long wurmId,final String name,final JSONObject json) {
        this.wurmId = wurmId;
        this.name = name;
        this.json = json;
    }

    public long getWurmId() {
        return wurmId;
    }

    public String getName() {
        return name;
    }

    public JSONObject getJson() {
        return json;
    }
}
