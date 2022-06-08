package org.qbicc.machine.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.OS;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.tool.process.InputSource;

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
        List<String> names = new ArrayList<>(16);
        // cross-compiler: cpu-os-vendor
        Cpu cpu = platform.getCpu();
        String cpuName = cpu.getSimpleName();
        OS os = platform.getOs();
        String osName = os.getName();
        String cc = System.getenv("CC");
        if (cc != null) {
            names.add(cc);
        } else {
            if (os == OS.LINUX && (os != Platform.HOST_PLATFORM.getOs() || cpu != Platform.HOST_PLATFORM.getCpu())) {
                names.add(cpuName + "-" + osName + "-gnu-gcc");
            }
            // generic compiler names
            names.addAll(List.of("clang", "emcc"));
        }
        for (String name : names) {
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
            compilerInvoker.setSource(InputSource.from("#include <pthread.h>\nint main() { return 0; }"));
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
