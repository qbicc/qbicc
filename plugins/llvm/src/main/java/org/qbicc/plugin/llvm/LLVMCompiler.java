package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.driver.Driver;
import org.qbicc.machine.tool.CCompilerInvoker;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.tool.llvm.LlcInvoker;
import org.qbicc.tool.llvm.LlvmToolChain;
import org.qbicc.tool.llvm.OptInvoker;
import org.qbicc.tool.llvm.OptPass;
import org.qbicc.tool.llvm.OutputFormat;
import org.qbicc.tool.llvm.RelocationModel;

import java.io.IOException;
import java.nio.file.Path;

public class LLVMCompiler {
    private final LlcInvoker llcInvoker;
    private final OptInvoker optInvoker;
    private final CCompilerInvoker ccInvoker;

    public LLVMCompiler(CompilationContext context, boolean isPie) {
        llcInvoker = createLlcInvoker(context, isPie);
        optInvoker = createOptInvoker(context);
        ccInvoker = createCCompilerInvoker(context);
    }

    public void compileModule(final CompilationContext context, Path modulePath) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);

        String moduleName = modulePath.getFileName().toString();
        if (moduleName.endsWith(".ll")) {
            String baseName = moduleName.substring(0, moduleName.length() - 3);
            String optBitCodeName = baseName + "_opt.bc";
            String assemblyName = baseName + ".s";
            String objectName = baseName + "." + cToolChain.getPlatform().getObjectType().objectSuffix();

            Path optBitCodePath = modulePath.resolveSibling(optBitCodeName);
            Path assemblyPath = modulePath.resolveSibling(assemblyName);
            Path objectPath = modulePath.resolveSibling(objectName);

            optInvoker.setSource(InputSource.from(modulePath));
            optInvoker.setDestination(OutputDestination.of(optBitCodePath));
            int errCnt = context.errors();
            try {
                optInvoker.invoke();
            } catch (IOException e) {
                if (errCnt == context.errors()) {
                    // whatever the problem was, it wasn't reported, so add an additional error here
                    context.error(Location.builder().setSourceFilePath(modulePath.toString()).build(), "`opt` invocation has failed: %s", e.toString());
                }
                return;
            }

            llcInvoker.setSource(InputSource.from(optBitCodePath));
            llcInvoker.setDestination(OutputDestination.of(assemblyPath));
            errCnt = context.errors();
            try {
                llcInvoker.invoke();
            } catch (IOException e) {
                if (errCnt == context.errors()) {
                    // whatever the problem was, it wasn't reported, so add an additional error here
                    context.error(Location.builder().setSourceFilePath(modulePath.toString()).build(), "`llc` invocation has failed: %s", e.toString());
                }
                return;
            }

            // now compile it
            ccInvoker.setSource(InputSource.from(assemblyPath));
            ccInvoker.setOutputPath(objectPath);
            try {
                ccInvoker.invoke();
            } catch (IOException e) {
                context.error("Compiler invocation has failed for %s: %s", modulePath, e.toString());
                return;
            }
            Linker.get(context).addObjectFilePath(objectPath);
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

    private static OptInvoker createOptInvoker(CompilationContext context) {
        LlvmToolChain llvmToolChain = context.getAttachment(Driver.LLVM_TOOL_KEY);
        if (llvmToolChain == null) {
            context.error("No LLVM tool chain is available");
            return null;
        }
        OptInvoker optInvoker = llvmToolChain.newOptInvoker();
        optInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        optInvoker.addOptimizationPass(OptPass.RewriteStatepointsForGc);
        optInvoker.addOptimizationPass(OptPass.AlwaysInline);
        return optInvoker;
    }

    private static LlcInvoker createLlcInvoker(CompilationContext context, boolean isPie) {
        LlvmToolChain llvmToolChain = context.getAttachment(Driver.LLVM_TOOL_KEY);
        if (llvmToolChain == null) {
            context.error("No LLVM tool chain is available");
            return null;
        }
        LlcInvoker llcInvoker = llvmToolChain.newLlcInvoker();
        llcInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        llcInvoker.setOutputFormat(OutputFormat.ASM);
        llcInvoker.setRelocationModel(isPie ? RelocationModel.Pic : RelocationModel.Static);
        return llcInvoker;
    }
}
