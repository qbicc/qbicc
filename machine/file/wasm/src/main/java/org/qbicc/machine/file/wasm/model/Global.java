package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.ValType;

/**
 *
 */
public sealed interface Global extends Exportable permits DefinedGlobal, ImportedGlobal {
    ValType type();

    Mutability mutability();
}
