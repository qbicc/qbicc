package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.type.ObjectReference;

public interface CallResult {
    Object getReturnValue();
    ObjectReference getThrowValue();
}
