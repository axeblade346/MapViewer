package net.spirangle.mapviewer.zone;

public class HighwayNode {

    private static final int HW_NODE = 16711680;
    private static final int HW_WAYSTONE = 16753920;

    public final int x;
    public final int y;
    public final boolean waystone;
    public final int color;
    public final HighwayNode[] nodes;
    public byte nodeCount;

    public HighwayNode(final int x,final int y,final boolean waystone) {
        this.x = x;
        this.y = y;
        this.waystone = waystone;
        this.color = waystone? HW_WAYSTONE : HW_NODE;
        this.nodes = new HighwayNode[]{ null,null,null,null,null,null,null,null };
        this.nodeCount = 0;
    }
}
