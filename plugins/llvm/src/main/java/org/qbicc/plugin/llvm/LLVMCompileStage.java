package org.qbicc.plugin.llvm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.qbicc.tool.llvm.LlvmLinkInvoker;
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
        final List<Path> bitcodePaths = new ArrayList<>();

        context.runParallelTask(ctxt -> {
            OptInvoker optInvoker = llvmToolChain.newOptInvoker();
            optInvoker.addOptimizationPass(OptPass.RewriteStatepointsForGc);
            optInvoker.addOptimizationPass(OptPass.AlwaysInline);

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
                    Path optBitCodePath = modulePath.resolveSibling(optBitCodeName);

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
                    synchronized (bitcodePaths) {
                        bitcodePaths.add(optBitCodePath);
                    }
                } else {
                    ctxt.warning("Ignoring unknown module file name \"%s\"", modulePath);
                }
            }
        });

        LlvmLinkInvoker llvmLinkInvoker = llvmToolChain.newLlvmLinkInvoker();

        LlcInvoker llcInvoker = llvmToolChain.newLlcInvoker();
        llcInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        llcInvoker.setOutputFormat(OutputFormat.ASM);
        llcInvoker.setRelocationModel(isPie ? RelocationModel.Pic : RelocationModel.Static);


        CCompilerInvoker ccInvoker = cToolChain.newCompilerInvoker();
        ccInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.ASM);

        String qbiccApp = "qbicc-app";
        Path fatBitCodePath = context.getOutputDirectory().resolve(qbiccApp + ".bc");
        Path assemblyPath = context.getOutputDirectory().resolve(qbiccApp + ".s");
        Path objectPath = context.getOutputDirectory().resolve(qbiccApp + "." + cToolChain.getPlatform().getObjectType().objectSuffix());

        llvmLinkInvoker.addBitcodeFiles(bitcodePaths);
        llvmLinkInvoker.setSource(InputSource.empty());
        llvmLinkInvoker.setDestination(OutputDestination.of(fatBitCodePath));
        int errCnt = context.errors();
        try {
            llvmLinkInvoker.invoke();
        } catch (IOException e) {
            if (errCnt == context.errors()) {
                // whatever the problem was, it wasn't reported, so add an additional error here
                context.error("`llvm-link` invocation has failed: %s", e.toString());
            }
        }

        llcInvoker.setSource(InputSource.from(fatBitCodePath));
        llcInvoker.setDestination(OutputDestination.of(assemblyPath));
        errCnt = context.errors();
        try {
            llcInvoker.invoke();
        } catch (IOException e) {
            if (errCnt == context.errors()) {
                // whatever the problem was, it wasn't reported, so add an additional error here
                context.error("`llc` invocation has failed: %s", e.toString());
            }
        }

        // now compile it
        ccInvoker.setSource(InputSource.from(assemblyPath));
        ccInvoker.setOutputPath(objectPath);
        try {
            ccInvoker.invoke();
        } catch (IOException e) {
            context.error("Compiler invocation has failed for %s: %s", assemblyPath, e.toString());
        }
        linker.addObjectFilePath(objectPath);
    }
}
