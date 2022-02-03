package org.qbicc.graph;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.qbicc.type.InvokableType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A method or function tail call.
 * The return value of the callee is returned by the caller and thus must be of the same type as, or a subtype of, the caller's return type.
 * Exceptions thrown by the target are not caught; instead, they are propagated out of the caller's frame.
 * If the caller is a hidden method, and the callee and caller have the same calling convention, the stack frame of the callee may replace the stack frame of the caller.
 * Otherwise, this node will translate to a plain {@code call} followed by a {@code return} on the back end.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#tailCall(org.qbicc.graph.ValueHandle, java.util.List)
 */
public final class TailCall extends AbstractTerminator {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final ValueHandle target;
    private final List<Value> arguments;
    private final InvokableType calleeType;

    TailCall(Node callSite, ExecutableElement element, int line, int bci, final BlockEntry blockEntry, Node dependency, ValueHandle target, List<Value> arguments) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.terminatedBlock = new BasicBlock(blockEntry, this);
        this.target = target;
        this.arguments = arguments;
        calleeType = (InvokableType) target.getValueType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(TailCall.class, dependency, target, arguments);
    }

    @Override
    String getNodeName() {
        return "TailCall";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TailCall && equals((TailCall) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        Iterator<Value> itr = arguments.iterator();
        if (itr.hasNext()) {
            itr.next().toString(b);
            while (itr.hasNext()) {
                b.append(',');
                itr.next().toString(b);
            }
        }
        b.append(')');
        return b;
    }

    public boolean equals(TailCall other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && arguments.equals(other.arguments);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public InvokableType getCalleeType() {
        return calleeType;
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public int getValueDependencyCount() {
        return arguments.size();
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return arguments.get(index);
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return target;
    }

    @Override
    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    @Override
    public <T, R> R accept(TerminatorVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean registerValue(PhiValue phi, Value val) {
        throw new IllegalStateException("No outbound values may be registered for a tail call");
    }
}
