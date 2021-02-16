package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ReadModifyWriteValue {
    Value getUpdateValue();

    MemoryAtomicityMode getAtomicityMode();
}
