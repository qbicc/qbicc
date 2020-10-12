package cc.quarkus.qcc.graph;

/**
 *
 */
public final class ArrayElementWrite extends AbstractNode implements ArrayElementOperation, WriteOperation, Action {
    private final Node dependency;
    private final Value instance;
    private final Value index;
    private final Value value;
    private final JavaAccessMode mode;

    ArrayElementWrite(final GraphFactory.Context ctxt, final Value instance, final Value index, final Value value, final JavaAccessMode mode) {
        this.instance = instance;
        this.index = index;
        this.value = value;
        this.mode = mode;
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
    }

    public Value getInstance() {
        return instance;
    }

    public Value getIndex() {
        return index;
    }

    public Value getWriteValue() {
        return value;
    }

    public JavaAccessMode getMode() {
        return mode;
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

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : index == 1 ? getWriteValue() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
