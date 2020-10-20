package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;

/**
 * An invocation on an object instance which returns a value.
 */
public final class ConstructorInvocation extends AbstractValue implements InstanceOperation, Invocation, Triable {
    private final Node dependency;
    private final Value instance;
    private final ConstructorElement target;
    private final List<Value> arguments;

    ConstructorInvocation(final Node dependency, final Value instance, final ConstructorElement target, final List<Value> arguments) {
        this.dependency = dependency;
        this.instance = instance;
        this.target = target;
        this.arguments = arguments;
    }

    public ConstructorElement getInvocationTarget() {
        return target;
    }

    public ValueType getType() {
        return instance.getType();
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
        return Invocation.super.getValueDependencyCount() + 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : Invocation.super.getValueDependency(index - 1);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
