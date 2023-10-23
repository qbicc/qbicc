package org.qbicc.plugin.llvm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.driver.Driver;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.tool.llvm.LlvmObjCopyInvoker;
import org.qbicc.tool.llvm.LlvmToolChain;

/**
 * Strip all of the stack map sections from every object file.
 */
public final class LLVMStripStackMapStage implements Consumer<CompilationContext> {
    @Override
    public void accept(CompilationContext context) {
        ArrayList<Path> list = new ArrayList<>(Linker.get(context).getObjectFilePathsInLinkOrder());
        Iterator<Path> iterator = list.iterator();
        context.runParallelTask(ctxt -> {
            LlvmObjCopyInvoker smrInvoker = createStackMapRemovingInvoker(ctxt);
            if (smrInvoker == null) {
                return;
            }

            // strip the stack map of every object file
            for (;;) {
                Path item;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    item = iterator.next();
                }

                smrInvoker.setObjectFilePath(item);
                try {
                    smrInvoker.invoke();
                } catch (IOException e) {
                    // just always report it because it's weird
                    context.error(Location.builder().setSourceFilePath(item.toString()).build(), "`llvm-objcopy` invocation has failed: %s", e.toString());
                }
            }
        });
    }


    private static LlvmObjCopyInvoker createStackMapRemovingInvoker(CompilationContext context) {
        LlvmToolChain llvmToolChain = context.getAttachment(Driver.LLVM_TOOL_KEY);
        if (llvmToolChain == null) {
            context.error("No LLVM tool chain is available");
            return null;
        }
        LlvmObjCopyInvoker objCopyInvoker = llvmToolChain.newLlvmObjCopyInvoker();
        objCopyInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        objCopyInvoker.removeSection(context.getPlatform().objectType().formatSectionName("llvm_stackmaps", "llvm_stackmaps"));
        return objCopyInvoker;
    }
}
