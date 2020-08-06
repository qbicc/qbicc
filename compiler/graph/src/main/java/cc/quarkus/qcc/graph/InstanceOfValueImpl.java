package cc.quarkus.qcc.graph;

final class InstanceOfValueImpl extends ValueImpl implements InstanceOfValue {
    ClassType instanceType;
    NodeHandle instance;

    public ClassType getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(final ClassType classType) {
        this.instanceType = classType;
    }

    public Value getInstance() {
        return NodeHandle.getTargetOf(instance);
    }

    public void setInstance(final Value value) {
        instance = NodeHandle.of(value);
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return "instanceof";
    }
}
