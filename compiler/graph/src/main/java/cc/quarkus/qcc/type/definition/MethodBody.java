package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.schedule.Schedule;

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

        public Schedule getSchedule() {
            return Schedule.EMPTY;
        }
    };

    int getParameterCount();

    ParameterValue getParameterValue(int index) throws IndexOutOfBoundsException;

    BasicBlock getEntryBlock();

    Schedule getSchedule();
}
