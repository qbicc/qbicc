package cc.quarkus.qcc.plugin.linker;

import java.io.IOException;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.driver.Driver;
import cc.quarkus.qcc.machine.tool.CToolChain;
import cc.quarkus.qcc.machine.tool.LinkerInvoker;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;

/**
 *
 */
public class LinkStage implements Consumer<CompilationContext> {
    public void accept(final CompilationContext context) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);
        if (cToolChain == null) {
            context.error("No C tool chain is available");
            return;
        }
        LinkerInvoker linkerInvoker = cToolChain.newLinkerInvoker();
        Linker linker = Linker.get(context);
        linkerInvoker.addObjectFiles(linker.getObjectFilePaths());
        linkerInvoker.addLibraries(linker.getLibraries());
        linkerInvoker.setOutputPath(context.getOutputDirectory().resolve("a.out"));
        linkerInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        try {
            linkerInvoker.invoke();
        } catch (IOException e) {
            context.error("Linker invocation failed: %s", e.toString());
        }
    }
}
