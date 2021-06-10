package org.qbicc.tool.llvm;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.tool.Tool;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LlvmLinkInvokerImpl extends AbstractLlvmInvoker implements LlvmLinkInvoker {
    private final List<Path> objectFiles = new ArrayList<>(4);

    LlvmLinkInvokerImpl(LlvmToolChainImpl tool, Path path) {
        super(tool, path);
    }

    @Override
    void addArguments(List<String> cmd) {
        for (Path file: objectFiles) {
            cmd.add(file.toString());
        }
    }

    @Override
    public void addBitcodeFile(Path path) {
        synchronized (objectFiles) {
            objectFiles.add(Assert.checkNotNullParam("path", path));
        }
    }

    @Override
    public int getBitcodeFileCount() {
        return objectFiles.size();
    }

    @Override
    public Path getBitcodeFile(int index) throws IndexOutOfBoundsException {
        return objectFiles.get(index);
    }
}
