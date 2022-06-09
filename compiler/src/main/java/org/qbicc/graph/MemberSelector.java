package org.qbicc.graph;

import org.qbicc.type.VoidType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An opaque member selection which always has a {@code void} type.  The wrapped value is a handle to any memory object.
 * Selected members may be unwrapped to be used as lvalues, or may be lazily transformed to plain loads.
 */
public final class MemberSelector extends AbstractValue {
    private final ValueHandle handle;
    private final VoidType voidType;

    MemberSelector(Node callSite, ExecutableElement element, int line, int bci, ValueHandle handle, VoidType voidType) {
        super(callSite, element, line, bci);
        this.handle = handle;
        this.voidType = voidType;
    }

    @Override
    public ValueHandle getValueHandle() {
        return handle;
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    public VoidType getType() {
        return voidType;
    }

    int calcHashCode() {
        return handle.hashCode() * 19;
    }

    @Override
    String getNodeName() {
        return "MemberSelector";
    }

    public boolean equals(final Object other) {
        return other instanceof MemberSelector && equals((MemberSelector) other);
    }

    public boolean equals(final MemberSelector other) {
        return this == other || other != null && handle.equals(other.handle);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }
}