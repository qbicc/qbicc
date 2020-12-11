package cc.quarkus.qcc.graph;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * An invocation on an object instance which returns a value.
 */
public final class InstanceInvocationValue extends AbstractValue implements InstanceOperation, MethodInvocation, DispatchInvocation, Triable {
    private final Node dependency;
    private final DispatchInvocation.Kind kind;
    private final Value instance;
    private final MethodElement target;
    private final ValueType type;
    private final List<Value> arguments;

    InstanceInvocationValue(final int line, final int bci, final Node dependency, final Kind kind, final Value instance, final MethodElement target, final ValueType type, final List<Value> arguments) {
        super(line, bci);
        this.dependency = dependency;
        this.kind = kind;
        this.instance = instance;
        this.target = target;
        this.type = type;
        this.arguments = arguments;
    }

    public MethodElement getInvocationTarget() {
        return target;
    }

    public ValueType getType() {
        return type;
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

    public DispatchInvocation.Kind getKind() {
        return kind;
    }

    public Value getInstance() {
        return instance;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return MethodInvocation.super.getValueDependencyCount() + 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : MethodInvocation.super.getValueDependency(index - 1);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }


    int calcHashCode() {
        return Objects.hash(dependency, kind, instance, target, arguments);
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceInvocationValue && equals((InstanceInvocationValue) other);
    }

    public boolean equals(final InstanceInvocationValue other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && kind.equals(other.kind)
            && instance.equals(other.instance)
            && target.equals(other.target)
            && arguments.equals(other.arguments);
    }
}
