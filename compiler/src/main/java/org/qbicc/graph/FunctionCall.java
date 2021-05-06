package org.qbicc.graph;

import static org.qbicc.object.Function.FN_NO_RETURN;
import static org.qbicc.object.Function.FN_NO_SIDE_EFFECTS;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class FunctionCall extends AbstractValue implements Triable, OrderedNode {
    // todo: fixed flags (such as "nothrow", "noreturn")
    // todo: native calling convention (fastcc, ccc, etc)
    private final Node dependency;
    private final Value callTarget;
    private final List<Value> arguments;
    private final int flags;

    FunctionCall(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value callTarget, final List<Value> arguments, int flags) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.callTarget = callTarget;
        this.arguments = arguments;
        this.flags = flags;
    }

    public ValueType getType() {
        return getFunctionType().getReturnType();
    }

    public FunctionType getFunctionType() {
        if (callTarget.getType() instanceof PointerType) {
            return (FunctionType) ((PointerType)callTarget.getType()).getPointeeType();
        } else {
            return (FunctionType) callTarget.getType();
        }
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public Value getArgument(int index) throws IndexOutOfBoundsException {
        return arguments.get(index);
    }

    public List<Value> getArguments() {
        return arguments;
    }

    public Value getCallTarget() {
        return callTarget;
    }

    @Override
    public Node getDependency() {
        if (hasDependency()) {
            return dependency;
        }
        throw new NoSuchElementException();
    }

    public boolean hasDependency() {
        return ! isNoSideEffects();
    }

    public int getValueDependencyCount() {
        return arguments.size() + 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? callTarget : index <= arguments.size() ? getArgument(index - 1) : Util.throwIndexOutOfBounds(index);
    }

    public int getFlags() {
        return flags;
    }

    public boolean isNoReturn() {
        return (flags & FN_NO_RETURN) != 0;
    }

    public boolean isNoSideEffects() {
        return (flags & FN_NO_SIDE_EFFECTS) != 0;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(FunctionCall.class, dependency, callTarget, arguments);
    }

    public boolean equals(final Object other) {
        return other instanceof FunctionCall && equals((FunctionCall) other);
    }

    public boolean equals(final FunctionCall other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && callTarget.equals(other.callTarget)
            && arguments.equals(other.arguments);
    }
}
