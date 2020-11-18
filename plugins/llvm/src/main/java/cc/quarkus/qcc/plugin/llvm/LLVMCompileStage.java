package cc.quarkus.qcc.plugin.llvm;

import java.nio.file.Path;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;

public class LLVMCompileStage implements Consumer<CompilationContext> {

    public void accept(final CompilationContext context) {
        LLVMState llvmState = context.getAttachment(LLVMState.KEY);
        if (llvmState == null) {
            context.note("No LLVM compilation units detected");
            return;
        }
        for (Path modulePath : llvmState.getModulePaths()) {
            // todo: call `llc` and then add the object files to the main context
        }
    }
}
