package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ValueType;

/**
 * A fetch of a variable argument from an argument list.
 */
public final class VaArg extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ValueType type;
    private final Value vaList;

    VaArg(final ProgramLocatable pl, Node dependency, Value vaList, ValueType type) {
        super(pl);
        this.dependency = dependency;
        this.type = type;
        this.vaList = vaList;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(dependency, type, vaList);
    }

    @Override
    String getNodeName() {
        return "VaArg";
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof VaArg va && equals(va);
    }

    public boolean equals(final VaArg other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && type.equals(other.type)
            && vaList.equals(other.vaList);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    public Value getVaList() {
        return vaList;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? vaList : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        vaList.toReferenceString(b);
        b.append(',');
        type.toString(b);
        b.append(')');
        return b;
    }

    @Override
    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
