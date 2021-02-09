package cc.quarkus.qcc.tests.integration;

import cc.quarkus.qcc.tests.integration.utils.App;
import cc.quarkus.qcc.tests.integration.utils.BuildAndRunCmd;
import cc.quarkus.qcc.tests.integration.utils.Commands;
import cc.quarkus.qcc.tests.integration.utils.Logs;
import cc.quarkus.qcc.tests.integration.utils.SnippetsJUnitProvider;
import cc.quarkus.qcc.tests.integration.utils.WhitelistLogLines;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static cc.quarkus.qcc.tests.integration.utils.App.APP_BUILD_OUT_DIR;
import static cc.quarkus.qcc.tests.integration.utils.App.MAVEN_COMPILER_RELEASE;
import static cc.quarkus.qcc.tests.integration.utils.App.QCC_BOOT_MODULE_PATH;
import static cc.quarkus.qcc.tests.integration.utils.App.QCC_MAIN_JAR;
import static cc.quarkus.qcc.tests.integration.utils.App.QCC_RUNTIME_API_JAR;
import static cc.quarkus.qcc.tests.integration.utils.Commands.deleteAppFiles;
import static cc.quarkus.qcc.tests.integration.utils.Commands.wrapUp;
import static cc.quarkus.qcc.tests.integration.utils.Logs.getLogsDir;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        final App app = new App(snippet.getParent().toString(),
                WhitelistLogLines.NONE,
                // Flow to build and run the snippet...
                new BuildAndRunCmd(new String[][]{
                        new String[]{"javac", "-cp",
                                QCC_RUNTIME_API_JAR,
                                     "--release",
                                     MAVEN_COMPILER_RELEASE,
                                snippet.getFileName().toString()},
                        new String[]{"java", "-jar", QCC_MAIN_JAR,
                                "--boot-module-path",
                                ".:" + QCC_BOOT_MODULE_PATH,
                                "--output-path",
                                APP_BUILD_OUT_DIR,
                                snippetName},
                        new String[]{APP_BUILD_OUT_DIR + File.separator + "a.out"}
                })
        );

        LOGGER.info("Testing snippet: " + snippet);
        Process process = null;
        Path buildLog = null;
        Path runLog = null;
        // report.md
        final StringBuilder report = new StringBuilder();
        // Parent dir for each snippet
        final Path appDir = Path.of(app.dir);
        // Naming, what the test runs are called in report.md
        final String cn = this.getClass().getCanonicalName();
        final String mn = snippet.getParent().getFileName() + "-" + snippetName;

        try {
            // Cleanup
            deleteAppFiles(app);
            Files.createDirectories(Path.of(app.dir, "logs"));

            // Build
            buildLog = Path.of(app.dir, "logs", "build.log");
            assertTrue(app.buildAndRunCmd.cmds.length > 1, "At least two commands expected in the array.");
            Logs.appendln(report, "# " + cn + ", " + mn);
            for (int i = 0; i < app.buildAndRunCmd.cmds.length - 1; i++) {
                final ExecutorService buildService = Executors.newFixedThreadPool(1);
                final List<String> cmd = List.of(app.buildAndRunCmd.cmds[i]);
                buildService.submit(new Commands.ProcessRunner(appDir, buildLog, cmd, 3));
                Logs.appendln(report, (new Date()).toString());
                Logs.appendln(report, app.dir);
                Logs.appendlnSection(report, String.join(" ", cmd));
                buildService.shutdown();
                // A snippet compilation should not take long, but then again, there might be edge cases...
                buildService.awaitTermination(3, TimeUnit.MINUTES);
            }

            // Run
            runLog = Paths.get(app.dir, "logs", "run.log");
            final List<String> cmd = List.of(app.buildAndRunCmd.cmds[app.buildAndRunCmd.cmds.length - 1]);
            process = Commands.runCommand(cmd, appDir.toFile(), runLog.toFile());
            // Assumes a snippet won't run longer...
            process.waitFor(15, TimeUnit.SECONDS);
            Logs.appendln(report, app.dir);
            Logs.appendlnSection(report, String.join(" ", cmd));
            Commands.processStopper(process, false);

            assertTrue(outputPattern.matcher(Files.readString(runLog)).matches(),
                    "The output stored in "
                            + getLogsDir(cn, mn) + File.separator + "run.log should have matched the pattern: " +
                            outputPattern.pattern());

            // There should not have been any problems logged during the build and run
            Logs.checkLog(cn, mn, app, buildLog.toFile());
            Logs.checkLog(cn, mn, app, runLog.toFile());
        } finally {
            wrapUp(process, cn, mn, report, app, buildLog, runLog, Path.of(app.dir, APP_BUILD_OUT_DIR));
        }
    }
}
