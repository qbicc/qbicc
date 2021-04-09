package org.qbicc.plugin.linker;

import java.io.IOException;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.LinkerInvoker;
import org.qbicc.machine.tool.ToolMessageHandler;

/**
 *
 */
public class LinkStage implements Consumer<CompilationContext> {
    private final boolean isPie;

    public LinkStage(final boolean isPie) {
        this.isPie = isPie;
    }

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
        linkerInvoker.setIsPie(isPie);
        try {
            linkerInvoker.invoke();
        } catch (IOException e) {
            context.error("Linker invocation failed: %s", e.toString());
        }
    }
}
