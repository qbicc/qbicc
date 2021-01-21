package cc.quarkus.qcc.machine.llvm.debuginfo;

import cc.quarkus.qcc.machine.llvm.Commentable;
import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface MetadataNode extends Commentable {
    LLValue asRef();
    MetadataNode comment(String comment);
}
