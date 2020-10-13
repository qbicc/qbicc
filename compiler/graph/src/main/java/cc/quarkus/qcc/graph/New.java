package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation.
 */
public final class New extends AbstractValue {
    private final Node dependency;
    private final UninitializedType type;

    New(final GraphFactory.Context ctxt, final ClassType type) {
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
        this.type = new UninitializedTypeImpl(type);
    }

    public UninitializedType getType() {
        return type;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
