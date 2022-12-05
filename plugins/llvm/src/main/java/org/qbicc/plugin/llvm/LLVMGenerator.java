package org.qbicc.plugin.llvm;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.object.ProgramModule;

/**
 *
 */
public class LLVMGenerator implements Consumer<CompilationContext>, ValueVisitor<CompilationContext, LLValue> {
    private final LLVMConfiguration config;

    public LLVMGenerator(LLVMConfiguration config) {
        this.config = config;
    }

    public void accept(final CompilationContext compilationContext) {
        LLVMModuleGenerator generator = new LLVMModuleGenerator(compilationContext, config);
        List<ProgramModule> allProgramModules = compilationContext.getAllProgramModules();
        Iterator<ProgramModule> iterator = allProgramModules.iterator();
        compilationContext.runParallelTask(ctxt -> {
            final LLVMCompilerImpl compiler = new LLVMCompilerImpl(ctxt, config, generator);
            for (;;) {
                ProgramModule programModule;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    programModule = iterator.next();
                }
                compiler.compileModule(ctxt, programModule.getTypeDefinition().load(), generator);
            }
        });
    }

    public int getLlvmMajor() {
        return config.getMajorVersion();
    }
}
