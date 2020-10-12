package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * An invoke instruction which returns a value.
 */
public final class StaticInvocationValue extends AbstractValue implements MethodInvocation, Value, Triable {
    private final Node dependency;
    private final MethodElement target;
    private final List<Value> arguments;

    StaticInvocationValue(final GraphFactory.Context ctxt, final MethodElement target, final List<Value> arguments) {
        this.target = target;
        this.arguments = arguments;
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
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

    public Type getType() {
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
