package org.qbicc.plugin.llvm;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.DefinedTypeDefinition;

public class LLVMDefaultModuleCompileStage implements Consumer<CompilationContext> {
    private final LLVMConfiguration config;

    public LLVMDefaultModuleCompileStage(LLVMConfiguration config) {
        this.config = config;
    }

    @Override
    public void accept(CompilationContext context) {
        LLVMModuleGenerator generator = new LLVMModuleGenerator(context, config);
        DefinedTypeDefinition defaultTypeDefinition = context.getDefaultTypeDefinition();
        final LLVMCompilerImpl compiler = new LLVMCompilerImpl(context, config, generator);
        compiler.compileModule(context, defaultTypeDefinition.load(), new LLVMModuleGenerator(context, config));
    }
}
