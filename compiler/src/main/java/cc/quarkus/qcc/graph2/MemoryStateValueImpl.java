package cc.quarkus.qcc.graph2;

abstract class MemoryStateValueImpl extends MemoryStateImpl implements Value {
    String getShape() {
        return "oval";
    }
}
