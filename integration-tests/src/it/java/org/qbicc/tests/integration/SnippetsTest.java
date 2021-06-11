package org.qbicc.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.qbicc.context.DiagnosticContext;
import org.qbicc.machine.tool.ToolExecutionFailureException;
import org.qbicc.tests.integration.utils.Javac;
import org.qbicc.tests.integration.utils.NativeExecutable;
import org.qbicc.tests.integration.utils.Qbicc;
import org.qbicc.tests.integration.utils.SnippetsJUnitProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * All .java classes found in snippets directory will be compiled
 * and run (they must have an entrypoint). The output of these programs
 * will be checked against .pattern files to verify it.
 */
@Tag("snippets")
public class SnippetsTest {

    private static final Logger LOGGER = Logger.getLogger(SnippetsTest.class.getName());

    @ParameterizedTest
    @ArgumentsSource(SnippetsJUnitProvider.class)
    void compileAndRunSnippet(final Path snippet, final Pattern outputPattern) throws IOException, InterruptedException {
        final String snippetName = snippet.getFileName().toString().replace(".java", "");

        Path targetPath = Path.of(".").resolve("target");
        Path baseOutputPath = targetPath.resolve("it/snippets").resolve(snippetName);
        Path classOutputPath = baseOutputPath.resolve("classes");
        Path nativeOutputPath = baseOutputPath.resolve("native");
        Path outputExecutable = nativeOutputPath.resolve("a.out");

        // Build via javac
        boolean compilationResult = Javac.compile(classOutputPath, snippet, LOGGER);

        assertTrue(compilationResult, "Compilation should succeed.");

        DiagnosticContext diagnosticContext = Qbicc.build(classOutputPath, nativeOutputPath, snippetName, LOGGER);

        assertEquals(0, diagnosticContext.errors(), "Native image creation should generate no errors.");

        StringBuilder stdOut = new StringBuilder();
        StringBuilder stdErr = new StringBuilder();
        try {
            NativeExecutable.run(snippetName, outputExecutable, stdOut, stdErr, LOGGER);
        } catch(ToolExecutionFailureException e) {
            // ensure snippet name gets included in the output message
            throw new ToolExecutionFailureException("Failed building: `"+ snippetName +"`", e);
        }

        assertTrue(stdErr.toString().isBlank(), "Native image execution should produce no error. " + stdErr);

        assertTrue(outputPattern.matcher(stdOut.toString()).matches(),
            "Standard output should have matched the pattern:\n[" +
                outputPattern.pattern() +
                "] but output was:\n["+ stdOut.toString() + "]");
    }
}
