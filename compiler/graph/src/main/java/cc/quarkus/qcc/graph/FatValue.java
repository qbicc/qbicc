package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.type.ValueType;

/**
 * A value which consumes two stack or local variable slots in a bytecode file.
 */
public final class FatValue extends AbstractValue {
    private final Value delegate;

    FatValue(final Value delegate) {
        super(delegate.getSourceLine(), delegate.getBytecodeIndex());
        this.delegate = delegate;
    }

    public static boolean isFat(Value value) {
        return value instanceof FatValue;
    }

    public static FatValue fatten(Value value) {
        return value instanceof FatValue ? (FatValue) value : new FatValue(value);
    }

    public static Value unfatten(Value value) {
        return value instanceof FatValue ? ((FatValue) value).delegate : value;
    }

    public ValueType getType() {
        return delegate.getType();
    }

    public int getValueDependencyCount() {
        return delegate.getValueDependencyCount();
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return delegate.getValueDependency(index);
    }

    public int getBasicDependencyCount() {
        return delegate.getBasicDependencyCount();
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return delegate.getBasicDependency(index);
    }

    public Node getSingleDependency(final BasicBlockBuilder graphFactory, final Node defaultValue) {
        return delegate.getSingleDependency(graphFactory, defaultValue);
    }

    public Constraint getConstraint() {
        return delegate.getConstraint();
    }

    public int getSourceLine() {
        return delegate.getSourceLine();
    }

    public int getBytecodeIndex() {
        return delegate.getBytecodeIndex();
    }

    public String toString() {
        return delegate.toString();
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        // fat values do not appear to visitors
        return delegate.accept(visitor, param);
    }
}
