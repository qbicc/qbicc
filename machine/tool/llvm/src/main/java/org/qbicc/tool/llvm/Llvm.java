package org.qbicc.tool.llvm;

import java.util.regex.Pattern;

import org.jboss.logging.Logger;

final class Llvm {
    static final Pattern LLVM_VERSION_PATTERN = Pattern.compile("LLVM version (\\d+(?:\\.\\d+)*)");

    private Llvm() {}

    static final Logger log = Logger.getLogger("org.qbicc.tool.llvm");
}
