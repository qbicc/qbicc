package org.qbicc.graph;

import java.util.List;
import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ValueType;

/**
 * A plain method or function call.
 * The return value of the target is the type of this node (which may be {@link org.qbicc.type.VoidType VoidType}).
 * Exceptions thrown by the target are not caught; instead, they are propagated out of the caller's frame.
 *
 * @see BasicBlockBuilder#call(org.qbicc.graph.Value, org.qbicc.graph.Value, java.util.List)
 */
public final class Call extends AbstractValue implements OrderedNode, InvocationNode {
    private final Node dependency;
    private final Value target;
    private final Value receiver;
    private final List<Value> arguments;

    Call(final ProgramLocatable pl, Node dependency, Value target, Value receiver, List<Value> arguments) {
        super(pl);
        for (int i = 0; i < arguments.size(); i++) {
            Assert.checkNotNullArrayParam("arguments", i, arguments.get(i));
        }
        this.dependency = dependency;
        this.target = target;
        this.receiver = receiver;
        this.arguments = arguments;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(Call.class, dependency, target, receiver, arguments);
    }

    @Override
    String getNodeName() {
        return "Call";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Call && equals((Call) other);
    }

    public boolean equals(Call other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && receiver.equals(other.receiver) && arguments.equals(other.arguments);
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return InvocationNode.toRValueString(this, "call", b);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public boolean maySafePoint() {
        return ! target.isNoSafePoints();
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
    public ValueType getType() {
        return getCalleeType().getReturnType();
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        return false;
    }
}
