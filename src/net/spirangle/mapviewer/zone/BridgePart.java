package net.spirangle.mapviewer.zone;

public final class BridgePart {
    private final int material;
    private final int heightOffset;
    private final int slope;
    
    public BridgePart(final int material, final int heightOffset, final int slope) {
        this.material = material;
        this.heightOffset = heightOffset;
        this.slope = slope;
    }
    
    public int getMaterial() {
        return this.material;
    }
    
    public int getHeightOffset() {
        return this.heightOffset;
    }
    
    public int getSlope() {
        return this.slope;
    }
    
    public int getColour() {
        switch (this.material) {
            case 1: {
                return 8089440;
            }
            case 3: {
                return 13156783;
            }
            case 4: {
                return 7627852;
            }
            default: {
                return 5592405;
            }
        }
    }
    
    public int getShadowColour() {
        final int rgb = this.getColour();
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        r /= 3;
        g /= 3;
        b /= 3;
        return r << 16 | g << 8 | b;
    }
}
