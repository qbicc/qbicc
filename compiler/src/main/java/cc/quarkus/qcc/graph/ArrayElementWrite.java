package cc.quarkus.qcc.graph;

import java.util.Objects;

/**
 *
 */
public final class ArrayElementWrite extends AbstractNode implements ArrayElementOperation, WriteOperation, Action {
    private final Node dependency;
    private final Value instance;
    private final Value index;
    private final Value value;
    private final JavaAccessMode mode;

    ArrayElementWrite(final int line, final int bci, final Node dependency, final Value instance, final Value index, final Value value, final JavaAccessMode mode) {
        super(line, bci);
        this.dependency = dependency;
        this.instance = instance;
        this.index = index;
        this.value = value;
        this.mode = mode;
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

    int calcHashCode() {
        return Objects.hash(dependency, instance, index, value, mode);
    }

    public boolean equals(final Object other) {
        return other instanceof ArrayElementWrite && equals((ArrayElementWrite) other);
    }

    public boolean equals(final ArrayElementWrite other) {
        return other == this || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance)
            && index.equals(other.index)
            && value.equals(other.value)
            && mode.equals(other.mode);
    }
}
