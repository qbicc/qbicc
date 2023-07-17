package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PointerType;

/**
 * A token indicating the automatic stack allocation and initialization of a value.
 */
public final class Auto extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final Value initializer;

    Auto(ProgramLocatable pl, Node dependency, Value initializer) {
        super(pl);
        this.dependency = dependency;
        this.initializer = initializer;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(dependency, initializer);
    }

    @Override
    String getNodeName() {
        return "Auto";
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return initializer.toReferenceString(b.append("auto "));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Auto a && equals(a);
    }

    public boolean equals(Auto other) {
        return this == other || other != null && dependency.equals(other.dependency) && initializer.equals(other.initializer);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> initializer;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public Value getInitializer() {
        return initializer;
    }

    @Override
    public PointerType getType() {
        return initializer.getType().getPointer();
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
