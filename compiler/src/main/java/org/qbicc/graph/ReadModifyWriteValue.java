package org.qbicc.graph;

/**
 *
 */
public interface ReadModifyWriteValue {
    Value getUpdateValue();

    MemoryAtomicityMode getAtomicityMode();
}
