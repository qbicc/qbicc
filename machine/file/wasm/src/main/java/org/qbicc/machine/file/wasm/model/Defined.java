package org.qbicc.machine.file.wasm.model;

/**
 * Things that are defined within a module.
 */
public sealed interface Defined permits DefinedFunc, DefinedGlobal, DefinedMemory, DefinedTable {
}
