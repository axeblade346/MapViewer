package net.spirangle.mapviewer.zone;

public class HighwayNode {

    public final int x;
    public final int y;
    public final int z;
    public final boolean waystone;
    public final boolean onBridge;
    public final boolean surfaced;
    public final HighwayNode[] nodes;
    public byte nodeCount;

    public HighwayNode(final int x,final int y,final int z,final boolean waystone,final boolean onBridge,final boolean surfaced) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.waystone = waystone;
        this.onBridge = onBridge;
        this.surfaced = surfaced;
        this.nodes = new HighwayNode[]{ null,null,null,null,null,null,null,null };
        this.nodeCount = 0;
    }
}
