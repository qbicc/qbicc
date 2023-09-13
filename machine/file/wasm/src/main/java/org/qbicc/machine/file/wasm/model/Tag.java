package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.TagAttribute;

/**
 *
 */
public sealed interface Tag permits DefinedTag, ImportedTag {
    TagAttribute attribute();

    FuncType type();
}
