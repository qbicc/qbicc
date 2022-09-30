package org.qbicc.graph;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.qbicc.type.InvokableType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A plain method or function call which never returns normally.
 * Exceptions thrown by the target are not caught; instead, they are propagated out of the caller's frame.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#callNoReturn(ValueHandle, List)
 */
public final class CallNoReturn extends AbstractTerminator {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final ValueHandle target;
    private final List<Value> arguments;
    private final InvokableType calleeType;

    CallNoReturn(Node callSite, ExecutableElement element, int line, int bci, final BlockEntry blockEntry, Node dependency, ValueHandle target, List<Value> arguments, Map<Slot, BlockParameter> parameters) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.terminatedBlock = new BasicBlock(blockEntry, this, parameters);
        this.target = target;
        this.arguments = arguments;
        calleeType = (InvokableType) target.getPointeeType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(CallNoReturn.class, dependency, target, arguments);
    }

    @Override
    String getNodeName() {
        return "CallNoReturn";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CallNoReturn && equals((CallNoReturn) other);
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

    public boolean equals(CallNoReturn other) {
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
        throw new IllegalStateException("No outbound values may be registered for a no-return call");
    }
}
