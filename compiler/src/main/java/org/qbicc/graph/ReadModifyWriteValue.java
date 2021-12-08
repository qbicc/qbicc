package org.qbicc.graph;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;

/**
 *
 */
public interface ReadModifyWriteValue extends Value {
    Value getUpdateValue();

    ReadAccessMode getReadAccessMode();

    WriteAccessMode getWriteAccessMode();

    MemoryAtomicityMode getAtomicityMode();
}
