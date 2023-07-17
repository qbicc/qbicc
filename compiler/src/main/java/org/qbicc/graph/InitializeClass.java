package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class InitializeClass extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final Value classToInitialize;

    InitializeClass(final ProgramLocatable pl, Node dependency, Value classToInitialize) {
        super(pl);
        this.dependency = dependency;
        this.classToInitialize = classToInitialize;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> classToInitialize;
            default -> Util.throwIndexOutOfBounds(index);
        };
    }

    public Value getInitializeClassValue() {
        return classToInitialize;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    String getNodeName() {
        return "InitializeClass";
    }

    @Override
    int calcHashCode() {
        return Objects.hash(InitializeClass.class, dependency, classToInitialize);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return classToInitialize.toReferenceString(super.toString(b).append('(')).append(')');
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InitializeClass r && equals(r);
    }

    public boolean equals(InitializeClass other) {
        return this == other || other != null && dependency.equals(other.dependency) && classToInitialize.equals(other.classToInitialize);
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
