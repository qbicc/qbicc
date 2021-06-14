package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.element.ExecutableElement;

public class ClassInitCheck extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final ObjectType objectType;

    ClassInitCheck(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final ObjectType objectType) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.objectType = objectType;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    int calcHashCode() {
        return Objects.hash(ClassInitCheck.class, dependency, objectType);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public boolean equals(Object other) {
        return other instanceof ClassInitCheck && equals((ClassInitCheck) other);
    }

    public boolean equals(final ClassInitCheck other) {
        return this == other || other != null
               && dependency.equals(other.dependency)
               && objectType == other.objectType;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
