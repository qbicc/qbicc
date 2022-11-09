package org.qbicc.tests.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.qbicc.machine.tool.ToolExecutionFailureException;
import org.qbicc.tests.integration.utils.NativeExecutable;
import org.qbicc.tests.integration.utils.SnippetsJUnitProvider;

/**
 * The output of each snippet will be checked against .pattern files to verify it.
 */
@Tag("snippets")
public class SnippetsTest {

    private static final Logger LOGGER = Logger.getLogger(SnippetsTest.class.getName());

    @ParameterizedTest
    @ArgumentsSource(SnippetsJUnitProvider.class)
    void runSnippet(final Path snippet, final Pattern outputPattern) throws IOException {
        final String snippetName = snippet.getFileName().toString().replace(".pattern", "");

        Path targetPath = Path.of(".").resolve("target");
        Path nativeOutputPath = targetPath.resolve("native");
        Path outputExecutable = nativeOutputPath.resolve("qbicc-integration-tests");

        StringBuilder stdOut = new StringBuilder();
        StringBuilder stdErr = new StringBuilder();
        try {
            NativeExecutable.run("snippet-" + snippetName, outputExecutable, stdOut, stdErr, LOGGER);
        } catch(ToolExecutionFailureException e) {
            // ensure snippet name gets included in the output message
            throw new ToolExecutionFailureException("Failed running: `"+ snippetName +"`", e);
        }

        assertTrue(stdErr.toString().isBlank(), "Native image execution should produce no error. " + stdErr);

        assertTrue(outputPattern.matcher(stdOut.toString()).matches(),
            "Standard output for " + snippetName + " should have matched the pattern:\n[" +
                outputPattern.pattern() +
                "] but output was:\n["+ stdOut + "]");
    }
}
