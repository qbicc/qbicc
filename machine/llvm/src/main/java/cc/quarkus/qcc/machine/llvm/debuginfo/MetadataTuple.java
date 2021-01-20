package cc.quarkus.qcc.machine.llvm.debuginfo;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface MetadataTuple extends MetadataNode {
    MetadataTuple elem(LLValue type, LLValue value);
    MetadataTuple comment(String comment);
}
