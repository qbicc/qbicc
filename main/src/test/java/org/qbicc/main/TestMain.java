package org.qbicc.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.machine.arch.Platform;
import org.qbicc.plugin.llvm.LLVMConfiguration;

/**
 * Basic tests of {@code Main}.
 */
public class TestMain {
    @Test
    //@Disabled("We're testing just WASM today")
    public void testTrivialCompile() throws IOException {
        final Main.Builder builder = Main.builder();
        builder.setMainClass(TrivialMain.class.getName());
        final String myPath = TestMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        builder.addAppPath(ClassPathEntry.of(Path.of(myPath)));
        builder.setCompileOutput(false);
        builder.setOutputName("test-app");
        builder.setOutputPath(Path.of(System.getProperty("qbicc.test.outputPath", "target/test-output")));
        builder.setLlvmConfigurationBuilder(LLVMConfiguration.builder().setPlatform(Platform.HOST_PLATFORM).setCompileOutput(false));
        final Main main = builder.build();
        final DiagnosticContext dc = main.call();
        for (Diagnostic d : dc.getDiagnostics()) {
            d.appendTo(System.out);
        }
        Assertions.assertEquals(0, dc.errors());
    }

    @Test
    @Disabled("WASM isn't working yet")
    public void testTrivialWasmCompile() throws IOException {
        final Main.Builder builder = Main.builder();
        builder.setMainClass(TrivialMain.class.getName());
        final String myPath = TestMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        builder.addAppPath(ClassPathEntry.of(Path.of(myPath)));
        builder.setCompileOutput(false);
        builder.setOutputName("test-app");
        builder.setOutputPath(Path.of(System.getProperty("qbicc.test.outputPath.wasm", "target/test-output-wasm")));
        builder.setPlatform(Platform.parse("wasm32-wasi"));
        builder.setBackend(Backend.wasm);
        final Main main = builder.build();
        final DiagnosticContext dc = main.call();
        for (Diagnostic d : dc.getDiagnostics()) {
            d.appendTo(System.out);
        }
        Assertions.assertEquals(0, dc.errors());
    }

    @Test
    public void testSplitPathString() {
        final List<Path> paths = Main.splitPathString(String.join(File.pathSeparator, "one", "two", "three"));
        final Iterator<Path> iterator = paths.iterator();
        Assertions.assertTrue(iterator.hasNext());
        Assertions.assertEquals("one", iterator.next().toString());
        Assertions.assertTrue(iterator.hasNext());
        Assertions.assertEquals("two", iterator.next().toString());
        Assertions.assertTrue(iterator.hasNext());
        Assertions.assertEquals("three", iterator.next().toString());
        Assertions.assertFalse(iterator.hasNext());
    }
}
