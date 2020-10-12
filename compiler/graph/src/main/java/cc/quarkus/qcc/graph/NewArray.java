package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation for arrays.
 */
public final class NewArray extends AbstractValue {
    private final Node dependency;
    private final ArrayType type;
    private final Value size;

    NewArray(final GraphFactory.Context ctxt, final ArrayType type, final Value size) {
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
        this.type = type;
        this.size = size;
    }

    public ArrayType getType() {
        return type;
    }

    public Value getSize() {
        return size;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? size : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
