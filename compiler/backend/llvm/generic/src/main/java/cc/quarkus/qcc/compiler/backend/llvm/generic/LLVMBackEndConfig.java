package cc.quarkus.qcc.compiler.backend.llvm.generic;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface LLVMBackEndConfig {
    Optional<List<String>> entryPointClassNames();
}
