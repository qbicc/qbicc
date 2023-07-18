package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.util.List;
import java.util.Set;

public interface LLVMCompiler {
    interface Factory {
        LLVMCompiler of(CompilationContext ctx, Set<Flag> flags, List<String> optOptions, List<String> llcOptions);
    }
    void compileModule(CompilationContext ctxt, LoadedTypeDefinition typeDefinition, LLVMModuleGenerator llvmModuleGenerator);

    enum Flag {
        PIE,
    }
}
