package org.qbicc.graph;

import java.util.Map;
import java.util.Objects;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Ret extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final Value returnAddressValue;
    private final BasicBlock terminatedBlock;

    Ret(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final Value returnAddressValue, Map<Slot, Value> targetArguments) {
        super(callSite, element, line, bci, targetArguments);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.returnAddressValue = returnAddressValue;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public Value getReturnAddressValue() {
        return returnAddressValue;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? returnAddressValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, returnAddressValue);
    }

    @Override
    String getNodeName() {
        return "Ret";
    }

    public boolean equals(final Object other) {
        return other instanceof Ret && equals((Ret) other);
    }

    public boolean equals(final Ret other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && returnAddressValue.equals(other.returnAddressValue);
    }
}
