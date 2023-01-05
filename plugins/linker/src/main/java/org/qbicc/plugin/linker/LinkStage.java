package org.qbicc.plugin.linker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
    private final String outputName;
    private final boolean isPie;
    private final List<Path> librarySearchPaths;

    public LinkStage(String outputName, final boolean isPie, List<Path> librarySearchPaths) {
        this.outputName = outputName;
        this.isPie = isPie;
        this.librarySearchPaths = librarySearchPaths;
    }

    public void accept(final CompilationContext context) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);
        if (cToolChain == null) {
            context.error("No C tool chain is available");
            return;
        }
        LinkerInvoker linkerInvoker = cToolChain.newLinkerInvoker();
        Linker linker = Linker.get(context);
        linkerInvoker.setWorkingDirectory(context.getOutputDirectory());
        linkerInvoker.addObjectFiles(linker.getObjectFilePathsInLinkOrder());
        linkerInvoker.addLibraries(linker.getLibraries());
        linkerInvoker.addLibraryPaths(librarySearchPaths);
        linkerInvoker.setOutputPath(context.getOutputDirectory().resolve(outputName));
        linkerInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        linkerInvoker.setIsPie(isPie);
        try {
            linkerInvoker.invoke();
        } catch (IOException e) {
            context.error("Linker invocation failed: %s", e.toString());
        }
    }
}
