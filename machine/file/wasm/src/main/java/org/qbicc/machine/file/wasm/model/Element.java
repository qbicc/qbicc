package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public sealed interface Element permits PassiveElement, ActiveElement, DeclarativeElement {
    RefType type();

    ElementInit init();
}
