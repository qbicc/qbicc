package cc.quarkus.qcc.graph.build;

import cc.quarkus.qcc.graph.node.RegionNode;

class CatchEntry {
    CatchEntry(int handlerIndex, RegionNode region) {
        this.handlerIndex = handlerIndex;
        this.region = region;
        this.matcher = new CatchMatcher();
    }

    public CatchMatcher getMatcher() {
        return this.matcher;
    }

    public RegionNode getRegion() {
        return this.region;
    }

    @Override
    public String toString() {
        return "CatchEntry{" +
                "matcher=" + matcher +
                ", handlerIndex=" + handlerIndex +
                ", handler=" + region +
                '}';
    }

    final int handlerIndex;

    final RegionNode region;

    private final CatchMatcher matcher;
}
