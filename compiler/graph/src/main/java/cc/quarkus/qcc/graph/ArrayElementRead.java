package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;

/**
 * A read of an array element.
 */
public final class ArrayElementRead extends AbstractValue implements ArrayElementOperation {
    private final Node dependency;
    private final ValueType type;
    private final Value instance;
    private final Value index;
    private final JavaAccessMode mode;

    ArrayElementRead(final Node dependency, final ValueType type, final Value instance, final Value index, final JavaAccessMode mode) {
        this.dependency = dependency;
        this.type = type;
        this.instance = instance;
        this.index = index;
        this.mode = mode;
    }

    public JavaAccessMode getMode() {
        return mode;
    }

    public Value getInstance() {
        return instance;
    }

    public Value getIndex() {
        return index;
    }

    public ValueType getType() {
        return type;
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? instance : index == 1 ? this.index : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
