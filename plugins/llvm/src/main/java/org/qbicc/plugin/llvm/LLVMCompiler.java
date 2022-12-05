package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.util.List;

public interface LLVMCompiler {
    interface Factory {
        LLVMCompiler of(CompilationContext ctx, boolean isPie, List<String> optOptions, List<String> llcOptions);
    }
    void compileModule(CompilationContext ctxt, LoadedTypeDefinition typeDefinition, LLVMModuleGenerator llvmModuleGenerator);
}
