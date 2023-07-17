package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

public final class ReadModifyWrite extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final Value pointer;
    private final Value updateValue;
    private final ReadAccessMode readMode;
    private final WriteAccessMode writeMode;
    private final Op op;

    ReadModifyWrite(final ProgramLocatable pl, final Node dependency, final Value pointer, Op op, final Value updateValue, ReadAccessMode readMode, WriteAccessMode writeMode) {
        super(pl);
        this.dependency = dependency;
        this.pointer = pointer;
        this.updateValue = updateValue;
        this.readMode = readMode;
        this.writeMode = writeMode;
        this.op = op;
        if (! pointer.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
        if (! pointer.isReadable()) {
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

    public Value getPointer() {
        return pointer;
    }

    public Value getUpdateValue() {
        return updateValue;
    }

    public ReadAccessMode getReadAccessMode() {
        return readMode;
    }

    public WriteAccessMode getWriteAccessMode() {
        return writeMode;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), dependency, pointer, updateValue, readMode, writeMode);
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
        updateValue.toReferenceString(b);
        b.append(',');
        b.append(readMode);
        b.append(',');
        b.append(writeMode);
        b.append(')');
        return b;
    }

    private boolean equals(final ReadModifyWrite other) {
        return this == other || other != null &&
            dependency.equals(other.dependency) &&
            pointer.equals(other.pointer) &&
            updateValue.equals(other.updateValue) &&
            readMode.equals(other.readMode) &&
            writeMode.equals(other.writeMode);
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> pointer;
            case 1 -> updateValue;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
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
