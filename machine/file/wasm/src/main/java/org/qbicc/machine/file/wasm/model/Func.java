package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.FuncType;

/**
 *
 */
public sealed interface Func extends Exportable, Named permits DefinedFunc, ImportedFunc {
    FuncType type();
}
