package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.nio.file.Path;

public interface LLVMCompiler {
    interface Factory {
        LLVMCompiler of(CompilationContext ctx, boolean isPie);
    }
    void compileModule(CompilationContext context, LoadedTypeDefinition typeDefinition, Path modulePath);
}
