package org.qbicc.graph;

import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.InvokableType;
import org.qbicc.type.ValueType;

/**
 * An invocation target with a thread binding.
 */
public final class ThreadBound extends AbstractValue {
    private final Value threadPtr;
    private final Value target;

    ThreadBound(final ProgramLocatable pl, final Value threadPtr, final Value target) {
        super(pl);
        this.threadPtr = Assert.checkNotNullParam("threadPtr", threadPtr);
        this.target = Assert.checkNotNullParam("target", target);
        threadPtr.getPointeeType();
        target.getPointeeType(InvokableType.class);
    }

    public Value getThreadPointer() {
        return threadPtr;
    }

    public Value getTarget() {
        return target;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> threadPtr;
            case 1 -> target;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    int calcHashCode() {
        return Objects.hash(threadPtr, target);
    }

    @Override
    String getNodeName() {
        return "ThreadBound";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ThreadBound tb && equals(tb);
    }

    public boolean equals(ThreadBound other) {
        return this == other || other != null && threadPtr.equals(other.threadPtr) && target.equals(other.target);
    }

    @Override
    public ValueType getType() {
        return target.getType();
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
