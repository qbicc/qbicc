package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ObjectType;

/**
 * A node that represents a check of the upper bound of the value against the given type.
 */
public final class InstanceOf extends AbstractValue implements InstanceOperation, OrderedNode, BooleanValue {
    private final Node dependency;
    private final Value input;
    private final Value valueIfTrue;
    private final ObjectType checkType;
    private final int checkDimensions;
    private final BooleanType booleanType;

    InstanceOf(final ProgramLocatable pl, Node dependency, final Value input,
               Value valueIfTrue, final ObjectType checkType, final int checkDimensions, final BooleanType booleanType) {
        super(pl);
        this.dependency = dependency;
        this.input = input;
        this.valueIfTrue = valueIfTrue;
        this.checkType = checkType;
        this.checkDimensions = checkDimensions;
        this.booleanType = booleanType;
    }

    public ObjectType getCheckType() {
        return checkType;
    }

    public int getCheckDimensions() {
        return checkDimensions; 
     }

    int calcHashCode() {
        return Objects.hash(dependency, input, checkType) * 31 + checkDimensions;
    }

    @Override
    String getNodeName() {
        return "InstanceOf";
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceOf && equals((InstanceOf) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        input.toReferenceString(b);
        b.append(',');
        checkType.toString(b);
        b.append(',');
        b.append(checkDimensions);
        b.append(')');
        return b;
    }

    public boolean equals(final InstanceOf other) {
        return this == other || other != null && dependency.equals(other.dependency) && input.equals(other.input) && checkType.equals(other.checkType) && checkDimensions == other.checkDimensions;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? input : Util.throwIndexOutOfBounds(index);
    }

    public Value getInstance() {
        return input;
    }

    public BooleanType getType() {
        return booleanType;
    }

    @Override
    public Value getValueIfTrue(BasicBlockBuilder bbb, Value input) {
        return input.equals(this.input) ? valueIfTrue : super.getValueIfTrue(bbb, input);
    }

    @Override
    public Value getValueIfFalse(BasicBlockBuilder bbb, Value input) {
        return input;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        return input.isConstant();
    }

    @Override
    public Node getDependency() {
        return dependency;
    }
}
