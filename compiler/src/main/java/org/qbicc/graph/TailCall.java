package org.qbicc.graph;

import java.util.List;
import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.InvokableType;

/**
 * A method or function tail call.
 * The return value of the callee is returned by the caller and thus must be of the same type as, or a subtype of, the caller's return type.
 * Exceptions thrown by the target are not caught; instead, they are propagated out of the caller's frame.
 * If the caller is a hidden method, and the callee and caller have the same calling convention, the stack frame of the callee may replace the stack frame of the caller.
 * Otherwise, this node will translate to a plain {@code call} followed by a {@code return} on the back end.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#tailCall(org.qbicc.graph.Value, org.qbicc.graph.Value, java.util.List)
 */
public final class TailCall extends AbstractTerminator implements InvocationNode {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final Value target;
    private final Value receiver;
    private final List<Value> arguments;
    private final InvokableType calleeType;

    TailCall(final ProgramLocatable pl, final BlockEntry blockEntry, Node dependency, Value target, Value receiver, List<Value> arguments) {
        super(pl);
        this.dependency = dependency;
        this.terminatedBlock = new BasicBlock(blockEntry, this);
        this.target = target;
        this.receiver = receiver;
        this.arguments = arguments;
        calleeType = (InvokableType) target.getPointeeType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(TailCall.class, dependency, target, receiver, arguments);
    }

    @Override
    String getNodeName() {
        return "TailCall";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TailCall && equals((TailCall) other);
    }

    public boolean equals(TailCall other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && receiver.equals(other.receiver) && arguments.equals(other.arguments);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return InvocationNode.toRValueString(this, "tail call", b);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public boolean maySafePoint() {
        return ! target.isNoSafePoints();
    }

    public InvokableType getCalleeType() {
        return calleeType;
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
    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    @Override
    public <T, R> R accept(TerminatorVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

}
