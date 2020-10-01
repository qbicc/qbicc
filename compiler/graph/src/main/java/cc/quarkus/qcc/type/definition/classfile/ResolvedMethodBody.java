package cc.quarkus.qcc.type.definition.classfile;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.type.definition.MethodBody;

final class ResolvedMethodBody implements MethodBody {
    private final ParameterValue[] parameterValues;
    private final BasicBlock entryBlock;
    private final Schedule schedule;

    ResolvedMethodBody(final ParameterValue[] parameterValues, final BasicBlock entryBlock, final Schedule schedule) {
        this.parameterValues = parameterValues;
        this.entryBlock = entryBlock;
        this.schedule = schedule;
    }

    public int getParameterCount() {
        return parameterValues.length;
    }

    public ParameterValue getParameterValue(final int index) throws IndexOutOfBoundsException {
        return parameterValues[index];
    }

    public BasicBlock getEntryBlock() {
        return entryBlock;
    }

    public Schedule getSchedule() {
        return schedule;
    }
}
