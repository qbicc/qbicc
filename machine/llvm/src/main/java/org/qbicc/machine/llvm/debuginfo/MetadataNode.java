package org.qbicc.machine.llvm.debuginfo;

import org.qbicc.machine.llvm.Commentable;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface MetadataNode extends Commentable {
    LLValue asRef();
    MetadataNode comment(String comment);
}
