package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolUtil;

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

    static Iterable<LlvmToolChain> findAllLlvmToolChains(Platform platform, Predicate<? super LlvmToolChain> filter, ClassLoader classLoader) {
        Path llcPath = ToolUtil.findExecutable("llc");
        if (llcPath != null) {
            Path optPath = ToolUtil.findExecutable("opt");
            if (optPath != null) {
                // check versions
                // (todo)
                return List.of(new LlvmToolChainImpl(llcPath, optPath, platform, "10" /*todo*/));
            }
        }
        return List.of();
    }
}
