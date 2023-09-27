package org.qbicc.machine.file.wasm.model;

/**
 *
 */
public sealed interface Memory extends Exportable, Limits permits ImportedMemory, DefinedMemory {
}
