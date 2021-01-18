package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 * A {@code new} allocation operation.
 */
public final class New extends AbstractValue {
    private final ClassObjectType type;

    New(final Element element, final int line, final int bci, final ClassObjectType type) {
        super(element, line, bci);
        this.type = type;
    }

    public ReferenceType getType() {
        return type.getReference();
    }

    public ClassObjectType getClassObjectType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        // every allocation is unique
        return System.identityHashCode(this);
    }

    public boolean equals(final Object other) {
        return this == other;
    }
}
