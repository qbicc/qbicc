package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.ThisValue;
import cc.quarkus.qcc.graph.schedule.Schedule;

/**
 *
 */
public interface MethodBody {

    int getParameterCount();

    ParameterValue getParameterValue(int index) throws IndexOutOfBoundsException;

    BasicBlock getEntryBlock();

    Schedule getSchedule();

    ThisValue getInstanceValue();
}
