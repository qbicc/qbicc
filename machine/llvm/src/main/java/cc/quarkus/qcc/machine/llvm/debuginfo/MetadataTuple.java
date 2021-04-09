package org.qbicc.machine.llvm.debuginfo;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface MetadataTuple extends MetadataNode {
    MetadataTuple elem(LLValue type, LLValue value);
    MetadataTuple comment(String comment);
}
