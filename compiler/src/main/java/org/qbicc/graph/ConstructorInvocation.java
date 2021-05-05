package org.qbicc.graph;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An invocation on an object instance which returns a value.
 */
public final class ConstructorInvocation extends AbstractValue implements InstanceOperation, Invocation, Triable, OrderedNode {
    private final Node dependency;
    private final Value instance;
    private final ConstructorElement target;
    private final List<Value> arguments;

    ConstructorInvocation(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value instance, final ConstructorElement target, final List<Value> arguments) {
        super(callSite, element, line, bci);
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

    public boolean hasDependency() {
        return target.hasNoModifiersOf(ClassFile.I_ACC_NO_SIDE_EFFECTS);
    }

    @Override
    public Node getDependency() {
        if (hasDependency()) {
            return dependency;
        }
        throw new NoSuchElementException();
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

    int calcHashCode() {
        return Objects.hash(dependency, instance, target, arguments);
    }

    public boolean equals(final Object other) {
        return other instanceof ConstructorInvocation && equals((ConstructorInvocation) other);
    }

    public boolean equals(final ConstructorInvocation other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance)
            && target.equals(other.target)
            && arguments.equals(other.arguments);
    }
}
