package org.qbicc.tool.llvm;

import java.nio.file.Path;

/**
 * An invoker for the {@code llvm-objcopy} tool.
 */
public interface LlvmObjCopyInvoker extends LlvmToolInvoker {
    void setObjectFilePath(Path path);

    void removeSection(String sectionName);
}
