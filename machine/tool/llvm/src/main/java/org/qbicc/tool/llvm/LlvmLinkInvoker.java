package org.qbicc.tool.llvm;

import org.qbicc.machine.tool.MessagingToolInvoker;

import java.nio.file.Path;

public interface LlvmLinkInvoker extends LlvmInvoker {
    void addBitcodeFile(Path path);

    default void addBitcodeFiles(Iterable<Path> paths) {
        for (Path path : paths) {
            addBitcodeFile(path);
        }
    }

    int getBitcodeFileCount();

    Path getBitcodeFile(int index) throws IndexOutOfBoundsException;
}
