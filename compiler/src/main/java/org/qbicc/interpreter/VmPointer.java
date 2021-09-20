package org.qbicc.interpreter;

import org.qbicc.type.PointerType;

/**
 * A VM pointer value.
 */
public interface VmPointer {
    PointerType getType();

    Memory getMemory();

    long getIndex();
}
