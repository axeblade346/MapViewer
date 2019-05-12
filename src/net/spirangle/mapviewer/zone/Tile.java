package net.spirangle.mapviewer.zone;

public final class Tile {
    private final int x;
    private final int y;
    
    public Tile(final int x, final int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() {
        return this.x;
    }
    
    public int getY() {
        return this.y;
    }
    
    @Override
    public int hashCode() {
        return this.x * 13 + this.y * 7 + 1;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Tile)) {
            return false;
        }
        final Tile t = (Tile)o;
        return t.x == this.x && t.y == this.y;
    }
}
