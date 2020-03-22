package net.spirangle.mapviewer.zone;

public final class Kingdom {
    private final int kingdom;
    private final String name;
    private final String king;

    public Kingdom(final int kingdom,final String name,final String king) {
        this.kingdom = kingdom;
        this.name = name;
        this.king = king;
    }

    public int getKingdom() {
        return this.kingdom;
    }

    public String getName() {
        return this.name;
    }

    public String getKing() {
        return this.king;
    }
}
