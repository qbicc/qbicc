package org.qbicc.graph;

import org.qbicc.type.VoidType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An opaque member selection which always has a {@code void} type.  The wrapped value is a pointer to any memory object.
 */
public final class MemberSelector extends AbstractValue implements UnaryValue {
    private final Value value;
    private final VoidType voidType;

    MemberSelector(Node callSite, ExecutableElement element, int line, int bci, Value value, VoidType voidType) {
        super(callSite, element, line, bci);
        this.value = value;
        this.voidType = voidType;
    }

    public Value getInput() {
        return value;
    }

    public VoidType getType() {
        return voidType;
    }

    int calcHashCode() {
        return value.hashCode() * 19;
    }

    @Override
    String getNodeName() {
        return "MemberSelector";
    }

    public boolean equals(final Object other) {
        return other instanceof MemberSelector && equals((MemberSelector) other);
    }

    public boolean equals(final MemberSelector other) {
        return this == other || other != null && value.equals(other.value);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}