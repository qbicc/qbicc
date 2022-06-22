package org.qbicc.main;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;

/**
 * Basic tests of {@code Main}.
 */
public class TestMain {
    @Test
    public void testTrivialCompile() throws IOException {
        final Main.Builder builder = Main.builder();
        builder.setMainClass(TrivialMain.class.getName());
        final String myPath = TestMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        builder.addAppPath(ClassPathEntry.of(Path.of(myPath)));
        builder.setCompileOutput(false);
        builder.setOutputName("test-app");
        builder.setOutputPath(Path.of(System.getProperty("qbicc.test.outputPath", "target/test-output")));
        final Main main = builder.build();
        final DiagnosticContext dc = main.call();
        for (Diagnostic d : dc.getDiagnostics()) {
            d.appendTo(System.out);
        }
        Assertions.assertEquals(0, dc.errors());
    }
}
