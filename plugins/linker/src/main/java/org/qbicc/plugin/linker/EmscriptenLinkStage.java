package org.qbicc.plugin.linker;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.LinkerInvoker;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 */
public class EmscriptenLinkStage implements Consumer<CompilationContext> {
    private final String outputName;
    private final boolean isPie;
    private final List<Path> librarySearchPaths;

    public EmscriptenLinkStage(String outputName, final boolean isPie, List<Path> librarySearchPaths) {
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
        Map<LoadedTypeDefinition, Path> objectFilePathsByType = linker.getObjectFilePathsByType();
        List<LoadedTypeDefinition> types = new ArrayList<>(objectFilePathsByType.keySet());
        types.sort(Comparator.comparingInt(LoadedTypeDefinition::getTypeId));
        List<Path> sortedPaths = new ArrayList<>(types.size());
        for (LoadedTypeDefinition type : types) {
            sortedPaths.add(objectFilePathsByType.get(type));
        }
        linkerInvoker.addObjectFiles(sortedPaths);
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
