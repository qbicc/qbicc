package org.qbicc.machine.file.wasm.stream;

import org.qbicc.machine.file.wasm.TagAttribute;

/**
 *
 */
public class TagSectionVisitor<E extends Exception> extends Visitor<E> {
    public void visitTag(TagAttribute attribute, int typeIdx) throws E {
    }
}
