package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * An invocation on an object instance which returns a value.
 */
public final class InstanceInvocationValue extends AbstractValue implements InstanceOperation, MethodInvocation, DispatchInvocation, Triable {
    private final Node dependency;
    private final DispatchInvocation.Kind kind;
    private final Value instance;
    private final MethodElement target;
    private final List<Value> arguments;

    InstanceInvocationValue(final GraphFactory.Context ctxt, final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        this.kind = kind;
        this.instance = instance;
        this.target = target;
        this.arguments = arguments;
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
    }

    public MethodElement getInvocationTarget() {
        return target;
    }

    public Type getType() {
        return target.getReturnType();
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public Value getArgument(final int index) {
        return arguments.get(index);
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
        return super.getValueDependencyCount() + 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : super.getValueDependency(index - 1);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T, R> R accept(final TriableVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
