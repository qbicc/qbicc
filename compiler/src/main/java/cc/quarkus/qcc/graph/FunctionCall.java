package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public final class FunctionCall extends AbstractValue implements Triable {
    // todo: fixed flags (such as "nothrow", "noreturn")
    // todo: native calling convention (fastcc, ccc, etc)
    private final Node dependency;
    private final Value callTarget;
    private final List<Value> arguments;

    FunctionCall(final int line, final int bci, final Node dependency, final Value callTarget, final List<Value> arguments) {
        super(line, bci);
        this.dependency = dependency;
        this.callTarget = callTarget;
        this.arguments = arguments;
    }

    public ValueType getType() {
        return getFunctionType().getReturnType();
    }

    public FunctionType getFunctionType() {
        return (FunctionType) callTarget.getType();
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

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return arguments.size();
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? callTarget : index <= arguments.size() ? getArgument(index - 1) : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
