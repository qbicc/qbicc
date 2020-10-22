package cc.quarkus.qcc.compiler.native_image.llvm.generic;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.compiler.native_image.api.NativeImageGeneratorFactory;
import cc.quarkus.qcc.context.CompilationContext;

public final class LLVMNativeImageGeneratorFactory implements NativeImageGeneratorFactory {
    public LLVMNativeImageGeneratorFactory() {}

    public String getName() {
        return "llvm-generic";
    }

    public NativeImageGenerator createGenerator(final CompilationContext context) {
        return new LLVMNativeImageGenerator(context);
    }
}
