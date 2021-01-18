package cc.quarkus.qcc.graph;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.Element;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public final class StaticInvocation extends AbstractNode implements MethodInvocation, Triable, Action {
    private final Node dependency;
    private final MethodElement target;
    private final List<Value> arguments;

    StaticInvocation(final Element element, final int line, final int bci, final Node dependency, final MethodElement target, final List<Value> arguments) {
        super(element, line, bci);
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

    int calcHashCode() {
        return Objects.hash(dependency, target, arguments);
    }

    public boolean equals(final Object other) {
        return other instanceof StaticInvocation && equals((StaticInvocation) other);
    }

    public boolean equals(final StaticInvocation other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && target.equals(other.target)
            && arguments.equals(other.arguments);
    }
}
