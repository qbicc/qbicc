package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.Data;

/**
 *
 */
public sealed interface Segment permits ActiveSegment, PassiveSegment {
    Data data();
}
