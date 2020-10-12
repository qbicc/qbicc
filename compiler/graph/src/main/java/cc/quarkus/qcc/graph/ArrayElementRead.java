package cc.quarkus.qcc.graph;

/**
 * A read of an array element.
 */
public final class ArrayElementRead extends AbstractValue implements ArrayElementOperation {
    private final Node dependency;
    private final Value instance;
    private final Value index;
    private final JavaAccessMode mode;

    ArrayElementRead(final GraphFactory.Context ctxt, final Value instance, final Value index, final JavaAccessMode mode) {
        this.instance = instance;
        this.index = index;
        this.mode = mode;
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
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

    public Type getType() {
        return ((ArrayType)instance.getType()).getElementType();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
