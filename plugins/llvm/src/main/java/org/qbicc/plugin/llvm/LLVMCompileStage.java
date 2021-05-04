package org.qbicc.plugin.llvm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;

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

public class LLVMCompileStage implements Consumer<CompilationContext> {
    private final boolean isPie;

    public LLVMCompileStage(final boolean isPie) {
        this.isPie = isPie;
    }

    public void accept(final CompilationContext context) {
        LLVMState llvmState = context.getAttachment(LLVMState.KEY);
        if (llvmState == null) {
            context.note("No LLVM compilation units detected");
            return;
        }
        LlvmToolChain llvmToolChain = context.getAttachment(Driver.LLVM_TOOL_KEY);
        if (llvmToolChain == null) {
            context.error("No LLVM tool chain is available");
            return;
        }
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);
        if (cToolChain == null) {
            context.error("No C tool chain is available");
            return;
        }

        Linker linker = Linker.get(context);

        Iterator<Path> iterator = llvmState.getModulePaths().iterator();
        context.runParallelTask(ctxt -> {
            LlcInvoker llcInvoker = llvmToolChain.newLlcInvoker();
            llcInvoker.setMessageHandler(ToolMessageHandler.reporting(ctxt));
            llcInvoker.setOutputFormat(OutputFormat.ASM);
            llcInvoker.setRelocationModel(isPie ? RelocationModel.Pic : RelocationModel.Static);

            OptInvoker optInvoker = llvmToolChain.newOptInvoker();
            optInvoker.addOptimizationPass(OptPass.RewriteStatepointsForGc);
            optInvoker.addOptimizationPass(OptPass.AlwaysInline);

            CCompilerInvoker ccInvoker = cToolChain.newCompilerInvoker();
            ccInvoker.setMessageHandler(ToolMessageHandler.reporting(ctxt));
            ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.ASM);

            for (;;) {
                Path modulePath;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    modulePath = iterator.next();
                }
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
                    int errCnt = ctxt.errors();
                    try {
                        optInvoker.invoke();
                    } catch (IOException e) {
                        if (errCnt == ctxt.errors()) {
                            // whatever the problem was, it wasn't reported, so add an additional error here
                            ctxt.error(Location.builder().setSourceFilePath(modulePath.toString()).build(), "`opt` invocation has failed: %s", e.toString());
                        }
                        continue;
                    }

                    llcInvoker.setSource(InputSource.from(optBitCodePath));
                    llcInvoker.setDestination(OutputDestination.of(assemblyPath));
                    errCnt = ctxt.errors();
                    try {
                        llcInvoker.invoke();
                    } catch (IOException e) {
                        if (errCnt == ctxt.errors()) {
                            // whatever the problem was, it wasn't reported, so add an additional error here
                            ctxt.error(Location.builder().setSourceFilePath(modulePath.toString()).build(), "`llc` invocation has failed: %s", e.toString());
                        }
                        continue;
                    }

                    // now compile it
                    ccInvoker.setSource(InputSource.from(assemblyPath));
                    ccInvoker.setOutputPath(objectPath);
                    try {
                        ccInvoker.invoke();
                    } catch (IOException e) {
                        ctxt.error("Compiler invocation has failed for %s: %s", modulePath, e.toString());
                        continue;
                    }
                    linker.addObjectFilePath(objectPath);
                } else {
                    ctxt.warning("Ignoring unknown module file name \"%s\"", modulePath);
                }
            }
        });
    }
}
