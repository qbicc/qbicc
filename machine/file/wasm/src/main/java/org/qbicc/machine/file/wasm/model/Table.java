package org.qbicc.machine.file.wasm.model;

import java.util.List;

import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public sealed interface Table extends Exportable permits ImportedTable, DefinedTable {
    RefType type();

    long minSize();

    long maxSize();

    List<ActiveElement> initializers();
}
