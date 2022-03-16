package org.qbicc.machine.llvm.debuginfo;

import org.qbicc.machine.llvm.LLValue;

/**
 * A debug expression which can be a literal or a declared metadata node.
 */
public interface DIGlobalVariableExpression extends MetadataNode {
    DIGlobalVariableExpression comment(String comment);

    LLValue asValue();
}
