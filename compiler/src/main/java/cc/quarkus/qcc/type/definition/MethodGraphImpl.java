package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;

class MethodGraphImpl implements MethodGraph {

    MethodGraphImpl(List<ParameterValue> parameters, BasicBlock entryBlock) {
        this.parameters = parameters;
        this.entryBlock = entryBlock;
    }

    @Override
    public List<ParameterValue> getParameters() {
        return this.parameters;
    }

    @Override
    public BasicBlock getEntryBlock() {
        return this.entryBlock;
    }

    private final List<ParameterValue> parameters;

    private final BasicBlock entryBlock;
}
