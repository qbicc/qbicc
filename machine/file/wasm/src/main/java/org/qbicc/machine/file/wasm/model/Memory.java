package org.qbicc.machine.file.wasm.model;

import java.util.List;

/**
 *
 */
public sealed interface Memory extends Exportable permits ImportedMemory, DefinedMemory {
    List<ActiveSegment> initializers();

    long minSize();

    long maxSize();
}
