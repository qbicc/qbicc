package cc.quarkus.qcc.graph;

abstract class MemoryStateValueImpl extends MemoryStateImpl implements Value, GraphFactory.MemoryStateValue {
    String getShape() {
        return "oval";
    }
}
