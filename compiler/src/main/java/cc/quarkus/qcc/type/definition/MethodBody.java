package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.schedule.Schedule;

/**
 *
 */
public interface MethodBody {

    int getParameterCount();

    Value getParameterValue(int index) throws IndexOutOfBoundsException;

    List<Value> getParameterValues();

    BasicBlock getEntryBlock();

    Schedule getSchedule();

    Value getThisValue();

    static MethodBody of(BasicBlock entryBlock, Schedule schedule, Value thisValue, Value... parameterValues) {
        return of(entryBlock, schedule, thisValue, List.of(parameterValues));
    }

    static MethodBody of(BasicBlock entryBlock, Schedule schedule, Value thisValue, List<Value> paramValues) {
        return new MethodBody() {
            public int getParameterCount() {
                return paramValues.size();
            }

            public Value getParameterValue(final int index) throws IndexOutOfBoundsException {
                return paramValues.get(index);
            }

            public List<Value> getParameterValues() {
                return paramValues;
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
