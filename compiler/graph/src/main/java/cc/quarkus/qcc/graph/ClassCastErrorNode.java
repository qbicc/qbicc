package cc.quarkus.qcc.graph;

/**
 *
 */
public final class ClassCastErrorNode extends AbstractNode implements Error {
    private final Node dependency;
    private final Value fromType;
    private final Value toType;

    ClassCastErrorNode(final int line, final int bci, final Node dependency, final Value fromType, final Value toType) {
        super(line, bci);
        this.dependency = dependency;
        this.fromType = fromType;
        this.toType = toType;
    }

    public Value getFromType() {
        return fromType;
    }

    public Value getToType() {
        return toType;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? fromType : index == 1 ? toType : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
