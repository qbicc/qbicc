package org.qbicc.tool.llvm;

import org.qbicc.machine.tool.MessagingToolInvoker;

/**
 *
 */
public interface LlvmToolInvoker extends MessagingToolInvoker {
    LlvmToolChain getTool();
}
