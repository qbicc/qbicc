package cc.quarkus.qcc.machine.llvm;

import java.io.BufferedWriter;
import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataTuple;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;

/**
 *
 */
public interface Module {
    // todo: metadata goes at the end for definitions
    FunctionDefinition define(String name);

    // todo: metadata goes after `declare` for declarations
    Function declare(String name);

    Global global(LLValue type);

    Global constant(LLValue type);

    MetadataTuple metadataTuple();
    MetadataTuple metadataTuple(String name);

    void writeTo(BufferedWriter output) throws IOException;

    static Module newModule() {
        return LLVM.newModule();
    }
}
