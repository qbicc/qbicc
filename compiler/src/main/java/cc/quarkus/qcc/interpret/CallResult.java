package cc.quarkus.qcc.interpret;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.QType;

public interface CallResult<V extends QType> {
    V getReturnValue();
    ObjectReference getThrowValue();
}
