package org.qbicc.graph;

/**
 *
 */
public interface ReadModifyWriteValue extends Value {
    Value getUpdateValue();

    MemoryAtomicityMode getAtomicityMode();
}
