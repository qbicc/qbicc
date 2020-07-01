package cc.quarkus.qcc.graph.schedule;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.MemoryState;
import cc.quarkus.qcc.graph.MemoryStateDependent;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Value;

/**
 * A linear schedule for basic block instructions.
 */
public interface Schedule {
    /**
     * Get the basic block that this schedule belongs to.
     *
     * @return the basic block
     */
    BasicBlock getBasicBlock();

    /**
     * Get the instruction list for this schedule.  The list will not be empty.  Each node in the list
     * implements {@link Value} or {@link MemoryStateDependent}.  Each {@code Value} in the list appears before any
     * of its usages.
     * <p>
     * The final node in the list implements {@link Terminator}.  Every non-final node in the list which implements
     * {@link MemoryStateDependent} also implements {@link MemoryState}.
     *
     * @return the instruction list (not {@code null})
     */
    List<Node> getInstructions();

    /**
     * Get the schedule for the given successor block.  The block must be a direct successor to this schedule's block.
     *
     * @return the schedule
     * @throws IllegalArgumentException if the block is not a successor to this schedule's block
     */
    Schedule getSuccessorSchedule(BasicBlock successor) throws IllegalArgumentException;

    /**
     * Create a schedule for the method whose entry block is the given block.
     *
     * @param entryBlock the entry block
     * @return a schedule for the method
     */
    static Schedule forMethod(BasicBlock entryBlock) {
        throw new UnsupportedOperationException();
    }
}
