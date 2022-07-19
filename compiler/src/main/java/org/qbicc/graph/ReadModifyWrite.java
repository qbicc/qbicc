package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

public final class ReadModifyWrite extends AbstractValue implements ReadModifyWriteValue, OrderedNode {
    private final Node dependency;
    private final PointerValue target;
    private final Value updateValue;
    private final ReadAccessMode readMode;
    private final WriteAccessMode writeMode;
    private final Op op;

    ReadModifyWrite(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final PointerValue target, Op op, final Value updateValue, ReadAccessMode readMode, WriteAccessMode writeMode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.target = target;
        this.updateValue = updateValue;
        this.readMode = readMode;
        this.writeMode = writeMode;
        this.op = op;
        if (! target.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
        if (! target.isReadable()) {
            throw new IllegalArgumentException("Handle is not readable");
        }
    }

    public ValueType getType() {
        return updateValue.getType();
    }

    public Op getOp() {
        return op;
    }

    public boolean isConstant() {
        // we could possibly make this more exact in the future
        return false;
    }

    public Node getDependency() {
        return dependency;
    }

    public PointerValue getPointerValue() {
        return target;
    }

    public Value getUpdateValue() {
        return updateValue;
    }

    @Override
    public ReadAccessMode getReadAccessMode() {
        return readMode;
    }

    @Override
    public WriteAccessMode getWriteAccessMode() {
        return writeMode;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), dependency, target, updateValue, readMode, writeMode);
    }

    @Override
    String getNodeName() {
        return "ReadModifyWrite";
    }

    public boolean equals(final Object other) {
        return other instanceof ReadModifyWrite && equals((ReadModifyWrite) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('.');
        b.append(op);
        b.append('(');
        b.append(updateValue);
        b.append(',');
        b.append(readMode);
        b.append(',');
        b.append(writeMode);
        b.append(')');
        return b;
    }

    private boolean equals(final ReadModifyWrite other) {
        return this == other || other.getClass() == getClass() && dependency.equals(other.dependency) && target.equals(other.target)
            && updateValue.equals(other.updateValue) && readMode.equals(other.readMode) && writeMode.equals(other.writeMode);
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? updateValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public boolean hasPointerValueDependency() {
        return true;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }

    public enum Op {
        SET,
        ADD,
        SUB,
        BITWISE_AND,
        BITWISE_NAND,
        BITWISE_OR,
        BITWISE_XOR,
        MIN,
        MAX,
        ;
    }
}
