package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public final class StaticFieldRead extends AbstractValue implements FieldRead {
    private final Node dependency;
    private final FieldElement fieldElement;
    private final ValueType type;
    private final JavaAccessMode mode;

    StaticFieldRead(final int line, final int bci, final Node dependency, final FieldElement fieldElement, final ValueType type, final JavaAccessMode mode) {
        super(line, bci);
        this.dependency = dependency;
        this.fieldElement = fieldElement;
        this.type = type;
        this.mode = mode;
    }

    public FieldElement getFieldElement() {
        return fieldElement;
    }

    public ValueType getType() {
        return type;
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

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, fieldElement, mode);
    }

    public boolean equals(final Object other) {
        return other instanceof StaticFieldRead && equals((StaticFieldRead) other);
    }

    public boolean equals(final StaticFieldRead other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && fieldElement.equals(other.fieldElement)
            && mode.equals(other.mode);
    }
}
