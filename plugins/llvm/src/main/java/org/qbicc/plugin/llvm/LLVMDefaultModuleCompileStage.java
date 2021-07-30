package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;

import java.nio.file.Path;
import java.util.function.Consumer;

public class LLVMDefaultModuleCompileStage implements Consumer<CompilationContext> {
    private final boolean isPie;

    public LLVMDefaultModuleCompileStage(boolean isPie) {
        this.isPie = isPie;
    }

    @Override
    public void accept(CompilationContext context) {
        LLVMModuleGenerator generator = new LLVMModuleGenerator(context, isPie ? 2 : 0, isPie ? 2 : 0);
        Path modulePath = generator.processProgramModule(context.getProgramModule(context.getDefaultTypeDefinition()));
        LLVMCompiler compiler = new LLVMCompiler(context, isPie);
        compiler.compileModule(context, modulePath);
    }
}
