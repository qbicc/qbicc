package cc.quarkus.qcc.graph;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * An invoke dynamic invocation which returns a value.
 */
public final class DynamicInvocationValue extends AbstractValue implements /* MethodInvocation, TODO: am I a MethodInvocation? */ Triable {
    private final Node dependency;
    private final MethodElement bootstrapMethod;
    private final List<Value> staticArguments;
    private final ValueType type;
    private final List<Value> arguments;

    DynamicInvocationValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final MethodElement bootstrapMethod, final List<Value> staticArguments, final ValueType type, final List<Value> arguments) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.bootstrapMethod = bootstrapMethod;
        this.staticArguments = staticArguments;
        this.type = type;
        this.arguments = arguments;
    }

    public MethodElement getBootstrapMethod() { return bootstrapMethod; }

    public ValueType getType() { return type; }

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

    public int getValueDependencyCount() {
        return getStaticArgumentCount() + getArgumentCount();
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        int sac = getStaticArgumentCount();
        return index < sac ? getStaticArgument(index) : getArgument(index - sac);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }


    int calcHashCode() {
        return Objects.hash(dependency, bootstrapMethod, staticArguments, type, arguments);
    }

    public boolean equals(final Object other) {
        return other instanceof DynamicInvocationValue && equals((DynamicInvocationValue) other);
    }

    public boolean equals(final DynamicInvocationValue other) {
        return this == other || other != null
                && dependency.equals(other.dependency)
                && bootstrapMethod.equals(other.bootstrapMethod)
                && staticArguments.equals(other.staticArguments)
                && type.equals(other.type)
                && arguments.equals(other.arguments);
    }
}
