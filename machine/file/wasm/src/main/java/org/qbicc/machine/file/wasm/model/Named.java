package org.qbicc.machine.file.wasm.model;

/**
 *
 */
public sealed interface Named permits Element, Func, Global, Imported, Local, Module, Segment, Tag {
    String name();

    default boolean isAnonymous() {
        return name().isEmpty();
    }
}
