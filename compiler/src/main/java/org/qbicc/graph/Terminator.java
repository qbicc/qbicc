package org.qbicc.graph;

import java.util.Map;

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

    Value getOutboundValue(PhiValue phi);

    boolean registerValue(PhiValue phi, Value val);

    Map<PhiValue, Value> getOutboundValues();
}
