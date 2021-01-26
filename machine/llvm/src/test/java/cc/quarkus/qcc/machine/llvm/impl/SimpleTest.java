package cc.quarkus.qcc.machine.llvm.impl;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.LLBuilder;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Values;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class SimpleTest {
    @Test
    public void testOutput() throws IOException {
        final Module module = Module.newModule();
        final FunctionDefinition main = module.define("main").returns(i32).comment("This is the function");
        LLBuilder.newBuilder(main.getRootBlock()).ret(i32, Values.ZERO).comment("This is the return statement").comment("It is the end of the block");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
            module.writeTo(writer);
            writer.flush();
        }
    }
}
