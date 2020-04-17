package cc.quarkus.qcc.machine.llvm.impl;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
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
        final FunctionDefinition main = module.define("main").returns(i32);
        main.ret(i32, Values.ZERO);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
            module.writeTo(writer);
            writer.flush();
        }
    }
}
