package cc.quarkus.qcc.graph;

abstract class MemoryStateValueImpl extends MemoryStateImpl implements Value {
    String getShape() {
        return "oval";
    }
}
