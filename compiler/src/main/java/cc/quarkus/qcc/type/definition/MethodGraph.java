package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;

public interface MethodGraph {
    List<ParameterValue> getParameters();
    BasicBlock getEntryBlock();
}
