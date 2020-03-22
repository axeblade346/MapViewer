package net.spirangle.mapviewer.zone;

public class GuardTower {
    private final String owner;
    private final long creationDate;
    private final int kingdom;
    private final int x;
    private final int y;
    private final int z;
    private final String description;

    public GuardTower(String owner,long creationDate,int kingdom,int x,int y,int z,String description) {
        this.owner = owner;
        this.creationDate = creationDate;
        this.kingdom = kingdom;
        this.x = x;
        this.y = y;
        this.z = z;
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public int getKingdom() {
        return kingdom;
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

    public String getDescription() {
        return description;
    }
}
