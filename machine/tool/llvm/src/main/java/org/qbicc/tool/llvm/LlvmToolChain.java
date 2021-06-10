package org.qbicc.tool.llvm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.tool.Tool;
import org.qbicc.machine.tool.ToolUtil;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;

/**
 *
 */
public interface LlvmToolChain extends Tool {

    default String getToolName() {
        return "llvm";
    }

    default String getImplementationName() {
        return "llvm";
    }

    LlcInvoker newLlcInvoker();

    OptInvoker newOptInvoker();

    LlvmLinkInvoker newLlvmLinkInvoker();

    static Iterable<LlvmToolChain> findAllLlvmToolChains(Platform platform, Predicate<? super LlvmToolChain> filter, ClassLoader classLoader) {
        Path llcPath = ToolUtil.findExecutable("llc");
        Path optPath = ToolUtil.findExecutable("opt");
        Path llvmLinkPath = ToolUtil.findExecutable("llvm-link");
        if (llcPath != null && optPath != null && llvmLinkPath != null) {
            if (optPath != null) {
                // check versions
                ProcessBuilder pb = new ProcessBuilder(List.of(llcPath.toString(), "--version"));
                StringBuilder stdOut = new StringBuilder();
                try {
                    InputSource.empty().transferTo(OutputDestination.of(pb, OutputDestination.discarding(), OutputDestination.of(stdOut)));
                } catch (IOException e) {
                    Llvm.log.warn("Failed to execute LLVM tool chain version command", e);
                    return List.of();
                }
                Matcher matcher = Llvm.LLVM_VERSION_PATTERN.matcher(stdOut);
                if (matcher.find()) {
                    String version = matcher.group(1);
                    return List.of(new LlvmToolChainImpl(llcPath, optPath, llvmLinkPath, platform, version));
                }
                Llvm.log.warn("Failed to identify LLVM version string; skipping");
            }
        }
        return List.of();
    }
}
