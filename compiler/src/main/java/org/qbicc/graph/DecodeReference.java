package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A node representing the decoding of a reference into a pointer value.
 * Reference decode nodes are ordered because a decoded pointer is only guaranteed to be valid until the next safepoint event.
 */
public final class DecodeReference extends AbstractValue implements WordCastValue, OrderedNode {
    private final Node dependency;
    private final Value reference;
    private final PointerType valueType;

    DecodeReference(Node callSite, ExecutableElement element, int line, int bci, Node dependency, Value reference, PointerType valueType) {
        super(callSite, element, line, bci);
        // eagerly resolve dependency
        while (dependency instanceof OrderedNode on && ! on.maySafePoint()) {
            dependency = on.getDependency();
        }
        this.dependency = dependency;
        this.reference = reference;
        this.valueType = valueType;
    }

    @Override
    public boolean isNullable() {
        return reference.isNullable();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(DecodeReference.class, dependency, reference);
    }

    @Override
    String getNodeName() {
        return "DecodeReference";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DecodeReference dr && equals(dr);
    }

    public boolean equals(DecodeReference other) {
        return this == other || other != null && dependency.equals(other.dependency) && reference.equals(other.reference);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public Value getInput() {
        return reference;
    }

    @Override
    public PointerType getType() {
        return valueType;
    }

    @Override
    public ReferenceType getInputType() {
        return reference.getType(ReferenceType.class);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        getInput().toReferenceString(b);
        b.append(')');
        b.append(" to ");
        valueType.toString(b);
        return b;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
