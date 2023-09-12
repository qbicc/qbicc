package org.qbicc.machine.file.wasm.model;

/**
 *
 */
public sealed interface Imported permits ImportedFunc, ImportedGlobal, ImportedMemory, ImportedTable {
    String moduleName();

    String name();
}
