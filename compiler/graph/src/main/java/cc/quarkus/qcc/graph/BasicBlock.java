package cc.quarkus.qcc.graph;

import java.util.Set;

/**
 *
 */
public interface BasicBlock extends Node {
    BasicBlock VOID_EMPTY = BasicBlockImpl.makeEmpty();

    Terminator getTerminator();
    void setTerminator(Terminator terminator);

    Set<BasicBlock> calculateReachableBlocks();

    default <P> void accept(GraphVisitor<P> visitor, P param) {
        // no operation
    }

    default int getSuccessorCount() {
        Terminator terminator = getTerminator();
        if (terminator == null) {
            return 0;
        }
        int cnt = 0;
        if (terminator instanceof Goto) {
            cnt ++;
        }
        if (terminator instanceof Try) {
            cnt ++;
        }
        if (terminator instanceof If) {
            cnt += 2;
        }
        if (terminator instanceof Switch) {
            cnt += 1 + ((Switch) terminator).getNumberOfValues();
        }
        return cnt;
    }

    default BasicBlock getSuccessor(int index) {
        int cnt = index;
        Terminator terminator = getTerminator();
        if (terminator == null) {
            throw new IndexOutOfBoundsException(index);
        }
        if (terminator instanceof Goto) {
            if (cnt == 0) {
                return ((Goto) terminator).getNextBlock();
            } else {
                cnt--;
            }
        }
        if (terminator instanceof Try) {
            if (cnt == 0) {
                return ((Try) terminator).getCatchHandler();
            } else {
                cnt --;
            }
        }
        if (terminator instanceof If) {
            if (cnt == 0) {
                return ((If) terminator).getTrueBranch();
            } else if (cnt == 1) {
                return ((If) terminator).getFalseBranch();
            } else {
                cnt -= 2;
            }
        }
        if (terminator instanceof Switch) {
            Switch switch_ = (Switch) terminator;
            if (cnt == 0) {
                return switch_.getDefaultTarget();
            }
            cnt--;
            if (cnt < switch_.getNumberOfValues()) {
                return switch_.getTargetForValue(switch_.getValue(cnt));
            }
        }
        throw new IndexOutOfBoundsException(index);
    }
}
