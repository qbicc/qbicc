package cc.quarkus.qcc.graph;

abstract class DependentValueImpl extends DependentNodeImpl implements Value {
    String getShape() {
        return "oval";
    }
}
