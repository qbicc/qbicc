package cc.quarkus.qcc.type;

public interface CallResult<V> {
    V getReturnValue();
    ObjectReference getThrowValue();
}
