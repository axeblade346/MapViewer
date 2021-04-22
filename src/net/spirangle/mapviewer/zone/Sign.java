package net.spirangle.mapviewer.zone;

public class Sign {
    private final String owner;
    private final long creationDate;
    private final int x;
    private final int y;
    private final int z;
    private final String message;

    public Sign(String owner,long creationDate,int aux,int x,int y,int z,String message) {
        this.owner = owner;
        this.creationDate = creationDate;
        this.x = x;
        this.y = y;
        this.z = z;
        this.message = message;
    }

    public String getOwner() {
        return owner;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getMessage() {
        return message;
    }
}
