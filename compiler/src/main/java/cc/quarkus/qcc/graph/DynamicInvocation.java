package cc.quarkus.qcc.graph;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public final class DynamicInvocation extends AbstractNode implements /* MethodInvocation, TODO: am I a MethodInvocation? */ Triable, Action {
    private final Node dependency;
    private final MethodElement bootstrapMethod;
    private final List<Value> staticArguments;
    private final List<Value> arguments;

    DynamicInvocation(final int line, final int bci, final Node dependency, final MethodElement bootstrapMethod, final List<Value> staticArguments, final List<Value> arguments) {
        super(line, bci);
        this.dependency = dependency;
        this.bootstrapMethod = bootstrapMethod;
        this.staticArguments = staticArguments;
        this.arguments = arguments;
    }
    public MethodElement getBootstrapMethod() { return bootstrapMethod; }

    public int getStaticArgumentCount() {
        return staticArguments.size();
    }

    public Value getStaticArgument(final int index) {
        return staticArguments.get(index);
    }

    public List<Value> getStaticArguments() {
        return staticArguments;
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
        return Objects.hash(dependency, bootstrapMethod, staticArguments, arguments);
    }

    public boolean equals(final Object other) {
        return other instanceof DynamicInvocation && equals((DynamicInvocation) other);
    }

    public boolean equals(final DynamicInvocation other) {
        return this == other || other != null
                && dependency.equals(other.dependency)
                && bootstrapMethod.equals(other.bootstrapMethod)
                && staticArguments.equals(other.staticArguments)
                && arguments.equals(other.arguments);
    }
}