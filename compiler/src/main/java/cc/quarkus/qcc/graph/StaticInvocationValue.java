package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * An invoke instruction which returns a value.
 */
public final class StaticInvocationValue extends AbstractValue implements MethodInvocation, Triable {
    private final Node dependency;
    private final MethodElement target;
    private final List<Value> arguments;

    StaticInvocationValue(final int line, final int bci, final Node dependency, final MethodElement target, final List<Value> arguments) {
        super(line, bci);
        this.dependency = dependency;
        this.target = target;
        this.arguments = arguments;
    }

    public MethodElement getInvocationTarget() {
        return target;
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public Value getArgument(final int index) {
        return arguments.get(index);
    }

    public List<Value> getArguments() {
        return arguments;
    }

    public ValueType getType() {
        return target.getReturnType();
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
