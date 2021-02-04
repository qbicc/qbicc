package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 * A read of an instance field.
 */
public final class InstanceFieldRead extends AbstractValue implements FieldRead, InstanceOperation, OrderedNode {
    private final Node dependency;
    private final Value instance;
    private final FieldElement fieldElement;
    private final ValueType type;
    private final JavaAccessMode mode;

    InstanceFieldRead(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value instance, final FieldElement fieldElement, final ValueType type, final JavaAccessMode mode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.instance = instance;
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

    public Value getInstance() {
        return instance;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, instance, fieldElement, mode);
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceFieldRead && equals((InstanceFieldRead) other);
    }

    public boolean equals(final InstanceFieldRead other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance)
            && fieldElement.equals(other.fieldElement)
            && mode.equals(other.mode);
    }
}
