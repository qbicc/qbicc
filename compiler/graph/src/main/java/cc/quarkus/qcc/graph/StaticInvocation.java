package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public final class StaticInvocation extends AbstractNode implements MethodInvocation, Triable, Action {
    private final Node dependency;
    private final MethodElement target;
    private final List<Value> arguments;

    StaticInvocation(final Node dependency, final MethodElement target, final List<Value> arguments) {
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

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
