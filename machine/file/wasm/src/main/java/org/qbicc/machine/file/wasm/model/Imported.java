package org.qbicc.machine.file.wasm.model;

/**
 *
 */
public sealed interface Imported extends Named permits ImportedFunc, ImportedGlobal, ImportedMemory, ImportedTable, ImportedTag {
    String moduleName();

    String name();
}
