package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.DefinedTypeDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class LLVMDefaultModuleCompileStage implements Consumer<CompilationContext> {
    private final boolean isPie;
    private final boolean compileOutput;
    private final boolean gcSupport;
    private final LLVMReferencePointerFactory refFactory;
    private LLVMCompiler.Factory llvmCompilerFactory;
    private List<String> optOptions;
    private List<String> llcOptions;

    public LLVMDefaultModuleCompileStage(boolean isPie, boolean compileOutput, boolean gcSupport, LLVMReferencePointerFactory refFactory, LLVMCompiler.Factory llvmCompilerFactory, List<String> optOptions, List<String> llcOptions) {
        this.isPie = isPie;
        this.compileOutput = compileOutput;
        this.gcSupport = gcSupport;
        this.refFactory = refFactory;
        this.llvmCompilerFactory = llvmCompilerFactory;
        this.optOptions = optOptions;
        this.llcOptions = llcOptions;
    }

    @Override
    public void accept(CompilationContext context) {
        LLVMModuleGenerator generator = new LLVMModuleGenerator(context, isPie ? 2 : 0, isPie ? 2 : 0, gcSupport, refFactory);
        DefinedTypeDefinition defaultTypeDefinition = context.getDefaultTypeDefinition();
        Path modulePath = generator.processProgramModule(context.getOrAddProgramModule(defaultTypeDefinition));
        if (compileOutput) {
            LLVMCompiler compiler = llvmCompilerFactory.of(context, isPie, optOptions, llcOptions);
            compiler.compileModule(context, defaultTypeDefinition.load(), modulePath);
        }
    }
}
