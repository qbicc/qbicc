package org.qbicc.graph;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.qbicc.type.InvokableType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A plain method or function call.
 * The return value of the target is the type of this node (which may be {@link org.qbicc.type.VoidType VoidType}).
 * Exceptions thrown by the target are not caught; instead, they are propagated out of the caller's frame.
 *
 * @see BasicBlockBuilder#call(org.qbicc.graph.ValueHandle, java.util.List)
 */
public final class Call extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ValueHandle target;
    private final List<Value> arguments;
    private final InvokableType functionType;

    Call(Node callSite, ExecutableElement element, int line, int bci, Node dependency, ValueHandle target, List<Value> arguments) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.target = target;
        this.arguments = arguments;
        functionType = (InvokableType) target.getPointeeType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(Call.class, dependency, target, arguments);
    }

    @Override
    String getNodeName() {
        return "Call";
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        Iterator<Value> itr = arguments.iterator();
        if (itr.hasNext()) {
            itr.next().toReferenceString(b);
            while (itr.hasNext()) {
                b.append(',');
                itr.next().toReferenceString(b);
            }
        }
        b.append(')');
        return b;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Call && equals((Call) other);
    }

    public boolean equals(Call other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && arguments.equals(other.arguments);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public InvokableType getCalleeType() {
        return functionType;
    }

    @Override
    public ValueType getType() {
        return getCalleeType().getReturnType();
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
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        return false;
    }
}
