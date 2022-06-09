package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.machine.tool.CCompilerInvoker;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.io.IOException;
import java.nio.file.Path;

public class LLVMEmscriptenCompiler implements LLVMCompiler {
    private final CCompilerInvoker ccInvoker;

    public LLVMEmscriptenCompiler(CompilationContext context, boolean isPie) {
        ccInvoker = createCCompilerInvoker(context);
    }

    @Override
    public void compileModule(final CompilationContext context, LoadedTypeDefinition typeDefinition, Path modulePath) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);

        String moduleName = modulePath.getFileName().toString();
        if (moduleName.endsWith(".ll")) {
            String baseName = moduleName.substring(0, moduleName.length() - 3);
            String objectName = baseName + "." + cToolChain.getPlatform().getObjectType().objectSuffix();
            Path objectPath = modulePath.resolveSibling(objectName);

            ccInvoker.setSource(InputSource.from(modulePath));
            ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.LLVM_IR);
            ccInvoker.setOutputPath(objectPath);
            try {
                ccInvoker.invoke();
            } catch (IOException e) {
                context.error("Compiler invocation has failed for %s: %s", modulePath, e.toString());
                return;
            }
            Linker.get(context).addObjectFilePath(typeDefinition, objectPath);
        } else {
            context.warning("Ignoring unknown module file name \"%s\"", modulePath);
        }
    }

    private static CCompilerInvoker createCCompilerInvoker(CompilationContext context) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);
        if (cToolChain == null) {
            context.error("No C tool chain is available");
            return null;
        }
        CCompilerInvoker ccInvoker = cToolChain.newCompilerInvoker();
        ccInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.ASM);
        return ccInvoker;
    }

}
