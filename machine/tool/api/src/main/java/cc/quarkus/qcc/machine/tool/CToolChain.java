package cc.quarkus.qcc.machine.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.process.InputSource;

/**
 *
 */
public interface CToolChain extends Tool {

    default String getToolName() {
        return "C Tool Chain";
    }

    CCompilerInvoker newCompilerInvoker();

    LinkerInvoker newLinkerInvoker();

    static Iterable<CToolChain> findAllCToolChains(final Platform platform, Predicate<? super CToolChain> filter, ClassLoader classLoader) {
        // for now, just a simple iterative check
        List<Path> paths = new ArrayList<>();
        // we also need to include cross-compilers at some point
        for (String name : Arrays.asList("cc", "gcc", "clang")) {
            Path path = ToolUtil.findExecutable(name);
            if (path != null) {
                paths.add(path);
            }
        }
        Iterable<CToolChain> iterable = ToolProvider.findAllTools(CToolChain.class, platform, filter, classLoader, paths);
        // now find all that work
        List<CToolChain> working = new ArrayList<>();
        for (CToolChain toolChain : iterable) {
            CCompilerInvoker compilerInvoker = toolChain.newCompilerInvoker();
            compilerInvoker.setSource(InputSource.from("int main() { return 0; }"));
            Path tempFile;
            try {
                tempFile = Files.createTempFile(null, ".out");
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create temporary file", e);
            }
            try {
                compilerInvoker.setOutputPath(tempFile);
                compilerInvoker.setMessageHandler(ToolMessageHandler.DISCARDING);
                compilerInvoker.invoke();
            } catch (IOException e) {
                // didn't work; don't add it
                continue;
            } finally {
                try {
                    Files.delete(tempFile);
                } catch (IOException ignored) {
                }
            }
            working.add(toolChain);
        }
        return working;
    }

}
