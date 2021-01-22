package cc.quarkus.qcc.machine.llvm.debuginfo;

import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface DICompileUnit extends MetadataNode {
    DICompileUnit producer(String producer);
    DICompileUnit isOptimized(boolean isOptimized);
    DICompileUnit flags(String flags);
    DICompileUnit runtimeVersion(int runtimeVersion);
    DICompileUnit splitDebugFilename(String splitDebugFilename);
    DICompileUnit enums(LLValue enums);
    DICompileUnit retainedTypes(LLValue retainedTypes);
    DICompileUnit globals(LLValue globals);
    DICompileUnit imports(LLValue imports);
    DICompileUnit macros(LLValue macros);

    DICompileUnit comment(String comment);
}
