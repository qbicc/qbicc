package org.qbicc.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.literal.BlockLiteral;

/**
 * An indirect branch node, used for implementing the legacy {@code ret} bytecode and also useful for outlining.
 */
public final class Ret extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final Value returnAddressValue;
    private final BasicBlock terminatedBlock;
    private List<BasicBlock> successors;

    Ret(final ProgramLocatable pl, final BlockEntry blockEntry, final Node dependency, final Value returnAddressValue, Map<Slot, Value> targetArguments) {
        super(pl, targetArguments);
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
    public int getSuccessorCount() {
        return getSuccessors().size();
    }

    @Override
    public BasicBlock getSuccessor(int index) {
        return getSuccessors().get(index);
    }

    public List<BasicBlock> getSuccessors() {
        List<BasicBlock> successors = this.successors;
        if (successors == null) {
            // calculate once
            if (returnAddressValue instanceof BlockLiteral bl) {
                return this.successors = List.of(bl.getBlock());
            } else {
                if (returnAddressValue instanceof BlockParameter bp) {
                    Set<Value> possibleValues = bp.getPossibleValues();
                    successors = new ArrayList<>(possibleValues.size());
                    for (Value possibleValue : possibleValues) {
                        if (possibleValue instanceof BlockLiteral bl) {
                            successors.add(bl.getBlock());
                        } else {
                            throw invalidSuccessor();
                        }
                    }
                    return this.successors = List.copyOf(successors);
                } else {
                    throw invalidSuccessor();
                }
            }
        } else {
            return successors;
        }
    }

    private static IllegalArgumentException invalidSuccessor() {
        return new IllegalArgumentException("Invalid successor for `Ret` node");
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
