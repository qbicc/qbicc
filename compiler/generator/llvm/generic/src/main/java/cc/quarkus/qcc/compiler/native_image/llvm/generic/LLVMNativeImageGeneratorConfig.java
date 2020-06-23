package cc.quarkus.qcc.compiler.native_image.llvm.generic;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface LLVMNativeImageGeneratorConfig {
    Optional<List<String>> entryPointClassNames();
}
