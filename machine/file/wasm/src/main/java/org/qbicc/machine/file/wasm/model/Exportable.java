package org.qbicc.machine.file.wasm.model;

/**
 * Things that can be exported.
 */
public sealed interface Exportable permits Func, Global, Memory, Table, Tag {
}
