package net.spirangle.mapviewer.zone;

public final class Server {
    private final int id;
    private final String name;
    private final boolean pvp;

    public Server(final int id,final String name,final boolean pvp) {
        this.id = id;
        this.name = name;
        this.pvp = pvp;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPvp() {
        return this.pvp;
    }
}
