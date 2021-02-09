package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.schedule.Schedule;

/**
 *
 */
public interface MethodBody {

    int getParameterCount();

    ParameterValue getParameterValue(int index) throws IndexOutOfBoundsException;

    List<ParameterValue> getParameterValues();

    BasicBlock getEntryBlock();

    Schedule getSchedule();

    ParameterValue getThisValue();

    static MethodBody of(BasicBlock entryBlock, Schedule schedule, ParameterValue thisValue, ParameterValue... parameterValues) {
        return of(entryBlock, schedule, thisValue, List.of(parameterValues));
    }

    static MethodBody of(BasicBlock entryBlock, Schedule schedule, ParameterValue thisValue, List<ParameterValue> paramValues) {
        return new MethodBody() {
            public int getParameterCount() {
                return paramValues.size();
            }

            public ParameterValue getParameterValue(final int index) throws IndexOutOfBoundsException {
                return paramValues.get(index);
            }

            public List<ParameterValue> getParameterValues() {
                return paramValues;
            }

            public BasicBlock getEntryBlock() {
                return entryBlock;
            }

            public Schedule getSchedule() {
                return schedule;
            }

            public ParameterValue getThisValue() {
                return thisValue;
            }
        };
    }
}
