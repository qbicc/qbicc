package org.qbicc.graph;

import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.PointerType;

/**
 * A view of an instance method pointer with a bound receiver.
 */
public final class ReceiverBound extends AbstractValue {
    private final Value methodPointer;
    private final Value receiver;

    ReceiverBound(ProgramLocatable pl, Value methodPointer, Value receiver) {
        super(pl);
        this.methodPointer = Assert.checkNotNullParam("methodPointer", methodPointer);
        methodPointer.getPointeeType(InstanceMethodType.class);
        this.receiver = Assert.checkNotNullParam("receiver", receiver);
    }

    public Value methodPointer() {
        return methodPointer;
    }

    public Value boundReceiver() {
        return receiver;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> methodPointer;
            case 1 -> receiver;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    int calcHashCode() {
        return Objects.hash(methodPointer, receiver);
    }

    @Override
    String getNodeName() {
        return "receiver bound";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReceiverBound rb && equals(rb);
    }

    public boolean equals(ReceiverBound other) {
        return this == other || other != null && methodPointer.equals(other.methodPointer) && receiver.equals(other.receiver);
    }

    @Override
    public PointerType getType() {
        return methodPointer.getType(PointerType.class);
    }

    @Override
    public InstanceMethodType getPointeeType() {
        return super.getPointeeType(InstanceMethodType.class);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
