package cc.quarkus.qcc.plugin.llvm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.driver.Driver;
import cc.quarkus.qcc.machine.tool.CCompilerInvoker;
import cc.quarkus.qcc.machine.tool.CToolChain;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import cc.quarkus.qcc.plugin.linker.Linker;
import cc.quarkus.qcc.tool.llvm.LlcInvoker;
import cc.quarkus.qcc.tool.llvm.LlvmToolChain;
import cc.quarkus.qcc.tool.llvm.OutputFormat;

public class LLVMCompileStage implements Consumer<CompilationContext> {

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
        LlcInvoker llcInvoker = llvmToolChain.newLlcInvoker();
        llcInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        llcInvoker.setOutputFormat(OutputFormat.ASM);
        CCompilerInvoker ccInvoker = cToolChain.newCompilerInvoker();
        ccInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.ASM);
        Linker linker = Linker.get(context);
        for (Path modulePath : llvmState.getModulePaths()) {
            String fileNameString = modulePath.getFileName().toString();
            if (fileNameString.endsWith(".ll")) {
                String baseName = fileNameString.substring(0, fileNameString.length() - 3);
                String outputNameString = baseName + ".s";
                String objNameString = baseName + "." + cToolChain.getPlatform().getObjectType().objectSuffix();
                Path outputPath = modulePath.resolveSibling(outputNameString);
                llcInvoker.setSource(InputSource.from(modulePath));
                llcInvoker.setDestination(OutputDestination.of(outputPath));
                int errCnt = context.errors();
                try {
                    llcInvoker.invoke();
                } catch (IOException e) {
                    if (errCnt == context.errors()) {
                        // whatever the problem was, it wasn't reported, so add an additional error here
                        context.error(Location.builder().setSourceFilePath(modulePath.toString()).build(), "`llc` invocation has failed: %s", e.toString());
                    }
                    continue;
                }
                // now compile it
                ccInvoker.setSource(InputSource.from(outputPath));
                Path objectPath = modulePath.resolveSibling(objNameString);
                ccInvoker.setOutputPath(objectPath);
                try {
                    ccInvoker.invoke();
                } catch (IOException e) {
                    context.error("Compiler invocation has failed for %s: %s", modulePath, e.toString());
                    continue;
                }
                linker.addObjectFilePath(objectPath);
            } else {
                context.warning("Ignoring unknown module file name \"%s\"", modulePath);
            }
        }
    }
}
