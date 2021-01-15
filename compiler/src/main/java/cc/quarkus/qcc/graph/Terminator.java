package cc.quarkus.qcc.graph;

/**
 * A construct which terminates a block.  It holds a dependency on the preceding sequence of inter-thread actions.
 */
public interface Terminator extends Node {
    <T, R> R accept(TerminatorVisitor<T, R> visitor, T param);

    BasicBlock getTerminatedBlock();

    default int getSuccessorCount() {
        return 0;
    }

    default BasicBlock getSuccessor(int index) {
        throw new IndexOutOfBoundsException(index);
    }
}
