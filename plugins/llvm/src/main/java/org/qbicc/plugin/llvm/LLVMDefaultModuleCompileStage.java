package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;

import java.nio.file.Path;
import java.util.function.Consumer;

public class LLVMDefaultModuleCompileStage implements Consumer<CompilationContext> {
    private final boolean isPie;
    private final boolean compileOutput;

    public LLVMDefaultModuleCompileStage(boolean isPie, boolean compileOutput) {
        this.isPie = isPie;
        this.compileOutput = compileOutput;
    }

    @Override
    public void accept(CompilationContext context) {
        LLVMModuleGenerator generator = new LLVMModuleGenerator(context, isPie ? 2 : 0, isPie ? 2 : 0);
        Path modulePath = generator.processProgramModule(context.getProgramModule(context.getDefaultTypeDefinition()));
        if (compileOutput) {
            LLVMCompiler compiler = new LLVMCompiler(context, isPie);
            compiler.compileModule(context, modulePath);
        }
    }
}
