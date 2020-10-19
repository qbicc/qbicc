package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.schedule.Schedule;

/**
 *
 */
public interface MethodBody {

    int getParameterCount();

    Value getParameterValue(int index) throws IndexOutOfBoundsException;

    BasicBlock getEntryBlock();

    Schedule getSchedule();

    Value getThisValue();

    static MethodBody of(BasicBlock entryBlock, Schedule schedule, Value thisValue, Value... parameterValues) {
        return new MethodBody() {
            public int getParameterCount() {
                return parameterValues.length;
            }

            public Value getParameterValue(final int index) throws IndexOutOfBoundsException {
                return parameterValues[index];
            }

            public BasicBlock getEntryBlock() {
                return entryBlock;
            }

            public Schedule getSchedule() {
                return schedule;
            }

            public Value getThisValue() {
                return thisValue;
            }
        };
    }
}
