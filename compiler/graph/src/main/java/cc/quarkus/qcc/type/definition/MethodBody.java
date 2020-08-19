package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;

/**
 *
 */
public interface MethodBody {
    MethodBody VOID_EMPTY = new MethodBody() {
        public int getParameterCount() {
            return 0;
        }

        public ParameterValue getParameterValue(final int index) throws IndexOutOfBoundsException {
            throw new IndexOutOfBoundsException(index);
        }

        public BasicBlock getEntryBlock() {
            return BasicBlock.VOID_EMPTY;
        }
    };

    int getParameterCount();

    ParameterValue getParameterValue(int index) throws IndexOutOfBoundsException;

    BasicBlock getEntryBlock();
}
