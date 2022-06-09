package org.qbicc.graph;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.qbicc.type.InvokableType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A plain method or function call with no side-effects.
 * The return value of the target is the type of this node (which may be {@link org.qbicc.type.VoidType VoidType}).
 * Exceptions are considered a side-effect, thus the target must not throw exceptions (this excludes most Java methods, which can throw {@code OutOfMemoryError} among other things).
 *
 * @see BasicBlockBuilder#callNoSideEffects(ValueHandle, List)
 */
public final class CallNoSideEffects extends AbstractValue {
    private final ValueHandle target;
    private final List<Value> arguments;
    private final InvokableType calleeType;

    CallNoSideEffects(Node callSite, ExecutableElement element, int line, int bci, ValueHandle target, List<Value> arguments) {
        super(callSite, element, line, bci);
        this.target = target;
        this.arguments = arguments;
        calleeType = (InvokableType) target.getValueType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(CallNoSideEffects.class, target, arguments);
    }

    @Override
    String getNodeName() {
        return "CallNoSideEffects";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CallNoSideEffects && equals((CallNoSideEffects) other);
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

    public boolean equals(CallNoSideEffects other) {
        return this == other || other != null && target.equals(other.target) && arguments.equals(other.arguments);
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

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
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
