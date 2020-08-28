package net.spirangle.mapviewer.zone;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HighwayManager {

    private static final Logger logger = Logger.getLogger(HighwayManager.class.getName());

    private List<HighwayNode> nodes;

    public HighwayManager(List<HighwayNode> nodes) {
        this.nodes = nodes;
    }

    public void unlinkCornerNodes() {
        logger.log(Level.INFO,"Unlinking highway corner nodes");
        for(final HighwayNode n : nodes) {
            if(n.nodes[0]!=null && n.nodes[1]!=null) unlinkHighwayCornerDiagonal(n,0,1,3);
            else if(n.nodes[2]!=null && n.nodes[1]!=null) unlinkHighwayCornerDiagonal(n,2,1,4);
            else if(n.nodeCount==2) {
                if(n.nodes[0]!=null && n.nodes[2]!=null) unlinkHighwayCorner(n,0,2,4);
                else if(n.nodes[2]!=null && n.nodes[7]!=null) unlinkHighwayCorner(n,2,7,6);
            }
        }
    }

    private void unlinkHighwayCorner(HighwayNode n,int d1,int d2,int c) {
        n.nodes[d1].nodes[c] = n.nodes[d2];
        n.nodes[d2].nodes[7-c] = n.nodes[d1];
        n.nodes[d1].nodes[7-d1] = null;
        n.nodes[d2].nodes[7-d2] = null;
        n.nodes[d1] = null;
        n.nodes[d2] = null;
        n.nodeCount = 0;
    }

    private void unlinkHighwayCornerDiagonal(HighwayNode n,int d,int c1,int c2) {
        HighwayNode nc1 = n.nodes[c1];
        if(nc1.nodeCount==2) {
            nc1.nodes[7-c1] = null;
            nc1.nodes[c2] = null;
            nc1.nodeCount = 0;
            n.nodes[c1] = null;
            n.nodeCount--;
            n.nodes[d].nodes[7-c2] = null;
            n.nodes[d].nodeCount--;
        } else if((nc1.nodeCount==4 && nc1.nodes[d]!=null && nc1.nodes[7-d]!=null) ||
                  (nc1.nodeCount==3 && (nc1.nodes[d]!=null || nc1.nodes[7-d]!=null))) {
            nc1.nodes[7-c1] = null;
            nc1.nodes[c2] = null;
            if(nc1.nodes[d]!=null) {
                nc1.nodes[d].nodes[7-d] = null;
                nc1.nodes[d].nodeCount--;
                nc1.nodes[d] = null;
            }
            if(nc1.nodes[7-d]!=null) {
                nc1.nodes[7-d].nodes[d] = null;
                nc1.nodes[7-d].nodeCount--;
                nc1.nodes[7-d] = null;
            }
            nc1.nodeCount = 0;
            n.nodes[c1] = null;
            n.nodeCount--;
            n.nodes[d].nodes[7-c2] = null;
            n.nodes[d].nodeCount--;
        } else {
            n.nodes[d].nodes[7-d] = null;
            n.nodes[d].nodeCount--;
            n.nodes[d] = null;
            n.nodeCount--;
        }
    }

    public void unlinkNodesInLine() {
        logger.log(Level.INFO,"Unlinking highway nodes in a straight line");
        for(final HighwayNode n : nodes) {
            if(n.nodeCount==0) continue;
            for(int i=0; i<8; ++i)
                if(n.nodes[i]!=null) {
                    HighwayNode a = n.nodes[i];
                    for(HighwayNode b; a.nodes[i]!=null; a=b) {
                        b = a.nodes[i];
                        a.nodes[i] = null;
                        a.nodes[7-i] = null;
                        a.nodeCount -= 2;
                    }
                    n.nodes[i] = a;
                    a.nodes[7-i] = n;
                }
        }
    }

    public void removeDoubleOrUnlinkedNodes() {
        logger.log(Level.INFO,"Removing double links and unlinked highway nodes (except waystones)");
        Iterator<HighwayNode> iterator = nodes.iterator();
        while(iterator.hasNext()) {
            HighwayNode n = iterator.next();
            if(n.nodeCount>0 && !n.waystone) {
                for(int i = 0; i<8; ++i)
                    if(n.nodes[i]!=null && n.nodes[i].nodes[7-i]==n) {
                        n.nodes[i] = null;
                        n.nodeCount--;
                    }
            }
            if(!n.waystone && n.nodeCount==0) iterator.remove();
        }
    }
}
