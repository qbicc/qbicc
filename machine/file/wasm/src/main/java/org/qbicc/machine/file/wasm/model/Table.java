package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public sealed interface Table extends Exportable, Limits permits ImportedTable, DefinedTable {
    RefType type();
}
