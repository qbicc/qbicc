package org.qbicc.machine.llvm.debuginfo;

import org.qbicc.machine.llvm.LLValue;

/**
 * A debug expression which can be a literal or a declared metadata node.
 */
public interface DIExpression extends MetadataNode {
    DIExpression comment(String comment);

    DIExpression arg(int value);

    DIExpression arg(DIOpcode op);

    DIExpression arg(DIEncoding op);

    LLValue asValue();
}
