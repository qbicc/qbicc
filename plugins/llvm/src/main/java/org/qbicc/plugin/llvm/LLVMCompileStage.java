package org.qbicc.plugin.llvm;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;

public class LLVMCompileStage implements Consumer<CompilationContext> {
    private final boolean isPie;

    public LLVMCompileStage(final boolean isPie) {
        this.isPie = isPie;
    }

    public void accept(final CompilationContext context) {
        LLVMState llvmState = context.getAttachment(LLVMState.KEY);
        if (llvmState == null) {
            context.note("No LLVM compilation units detected");
            return;
        }

        Iterator<Path> iterator = llvmState.getModulePaths().iterator();
        context.runParallelTask(ctxt -> {
            LLVMCompiler compiler = new LLVMCompiler(context, isPie);
            for (;;) {
                Path modulePath;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    modulePath = iterator.next();
                }
                compiler.compileModule(ctxt, modulePath);
            }
        });
    }
}
