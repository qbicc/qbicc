package org.qbicc.graph;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A construct which terminates a block.  It holds a dependency on the preceding sequence of inter-thread actions.
 */
public interface Terminator extends OrderedNode {
    <T, R> R accept(TerminatorVisitor<T, R> visitor, T param);

    BasicBlock getTerminatedBlock();

    default int getSuccessorCount() {
        return 0;
    }

    default BasicBlock getSuccessor(int index) {
        throw new IndexOutOfBoundsException(index);
    }

    @Deprecated
    Value getOutboundValue(PhiValue phi);

    @Deprecated
    boolean registerValue(PhiValue phi, Value val);

    @Deprecated
    Map<PhiValue, Value> getOutboundValues();

    /**
     * Get the outbound argument for the given block parameter slot, to be passed into any successor block.
     * Terminators which do not pass control flow to a successor block should return an empty map.
     * The terminator must establish a value dependency on each value propagated as an outbound argument.
     *
     * @param slot the slot (must not be {@code null})
     * @return the argument (not {@code null})
     * @throws NoSuchElementException if there is no outbound argument specified for the given slot
     */
    default Value getOutboundArgument(Slot slot) throws NoSuchElementException {
        throw new NoSuchElementException();
    }

    /**
     * Get the set of outbound argument names, in no particular order.
     *
     * @return the names set (not {@code null})
     */
    default Set<Slot> getOutboundArgumentNames() {
        return Set.of();
    }
}
