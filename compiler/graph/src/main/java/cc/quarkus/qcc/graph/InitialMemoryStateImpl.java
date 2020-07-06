package cc.quarkus.qcc.graph;

final class InitialMemoryStateImpl extends MemoryStateImpl implements InitialMemoryState {
    public String getLabelForGraph() {
        return "initial";
    }
}
