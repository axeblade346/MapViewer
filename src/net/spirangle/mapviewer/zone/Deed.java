package net.spirangle.mapviewer.zone;

public final class Deed {
    private final String name;
    private final String founder;
    private final String mayor;
    private final long creationDate;
    private final boolean democracy;
    private final int kingdom;
    private final int x;
    private final int y;
    private final int sx;
    private final int sy;
    private final int ex;
    private final int ey;
    private final boolean permanent;
    private final boolean pvp;
    private int height;
    
    public Deed(final String name, final String founder, final String mayor, final long creationDate, final boolean democracy, final int kingdom,
                final int x, final int y, final boolean permanent, final boolean pvp, final int sx, final int sy, final int ex, final int ey) {
        this.height = 0;
        this.name = name;
        this.founder = founder;
        this.mayor = mayor;
        this.creationDate = creationDate;
        this.democracy = democracy;
        this.kingdom = kingdom;
        this.x = x;
        this.y = y;
        this.permanent = permanent;
        this.pvp = pvp;
        this.sx = sx;
        this.sy = sy;
        this.ex = ex;
        this.ey = ey;
    }
    
    public int getHeight() {
        return this.height;
    }
    
    public void setHeight(final int height) {
        this.height = height;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getFounder() {
        return this.founder;
    }
    
    public String getMayor() {
        return this.mayor;
    }
    
    public long getCreationDate() {
        return this.creationDate;
    }
    
    public boolean isDemocracy() {
        return this.democracy;
    }
    
    public int getKingdom() {
        return this.kingdom;
    }
    
    public int getX() {
        return this.x;
    }
    
    public int getY() {
        return this.y;
    }
    
    public boolean isPermanent() {
        return this.permanent;
    }
    
    public boolean isPvp() {
        return this.pvp;
    }
    
    public int getSx() {
        return this.sx;
    }
    
    public int getSy() {
        return this.sy;
    }
    
    public int getEx() {
        return this.ex;
    }
    
    public int getEy() {
        return this.ey;
    }
}
