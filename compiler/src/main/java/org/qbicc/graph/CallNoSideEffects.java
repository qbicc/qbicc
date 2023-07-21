package org.qbicc.graph;

import java.util.List;
import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.InvokableType;
import org.qbicc.type.ValueType;

/**
 * A plain method or function call with no side-effects.
 * The return value of the target is the type of this node (which may be {@link org.qbicc.type.VoidType VoidType}).
 * Exceptions are considered a side-effect, thus the target must not throw exceptions (this excludes most Java methods, which can throw {@code OutOfMemoryError} among other things).
 *
 * @see BasicBlockBuilder#callNoSideEffects(Value, Value, List)
 */
public final class CallNoSideEffects extends AbstractValue implements InvocationNode {
    private final Value target;
    private final Value receiver;
    private final List<Value> arguments;
    private final InvokableType calleeType;

    CallNoSideEffects(final ProgramLocatable pl, Value target, Value receiver, List<Value> arguments) {
        super(pl);
        for (int i = 0; i < arguments.size(); i++) {
            Assert.checkNotNullArrayParam("arguments", i, arguments.get(i));
        }
        this.target = target;
        this.receiver = receiver;
        this.arguments = arguments;
        calleeType = (InvokableType) target.getPointeeType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(CallNoSideEffects.class, target, receiver, arguments);
    }

    @Override
    String getNodeName() {
        return "CallNoSideEffects";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CallNoSideEffects && equals((CallNoSideEffects) other);
    }

    public boolean equals(CallNoSideEffects other) {
        return this == other || other != null && target.equals(other.target) && receiver.equals(other.receiver) && arguments.equals(other.arguments);
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return InvocationNode.toRValueString(this, "call", b).append(" no-side-effects");
    }

    public InvokableType getCalleeType() {
        return calleeType;
    }

    @Override
    public ValueType getType() {
        return getCalleeType().getReturnType();
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public Value getTarget() {
        return target;
    }

    @Override
    public Value getReceiver() {
        return receiver;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        for (Value argument : arguments) {
            if (! argument.isConstant()) {
                return false;
            }
        }
        return true;
    }
}
