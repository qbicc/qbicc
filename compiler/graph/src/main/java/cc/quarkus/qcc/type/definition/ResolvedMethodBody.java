package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ParameterValue;

public interface ResolvedMethodBody extends VerifiedMethodBody {
    default ResolvedMethodBody verify() {
        return this;
    }

    default ResolvedMethodBody resolve() {
        return this;
    }

    List<ParameterValue> getParameters();
    BasicBlock getEntryBlock();
}
