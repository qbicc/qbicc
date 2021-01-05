package cc.quarkus.qcc.tests.integration;

import cc.quarkus.qcc.tests.integration.utils.App;
import cc.quarkus.qcc.tests.integration.utils.Commands;
import cc.quarkus.qcc.tests.integration.utils.Logs;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static cc.quarkus.qcc.tests.integration.utils.App.APP_BUILD_OUT_DIR;
import static cc.quarkus.qcc.tests.integration.utils.Commands.builderRoutine;
import static cc.quarkus.qcc.tests.integration.utils.Commands.deleteAppFiles;
import static cc.quarkus.qcc.tests.integration.utils.Commands.wrapUp;
import static cc.quarkus.qcc.tests.integration.utils.Logs.getLogsDir;
import static cc.quarkus.qcc.tests.integration.utils.Logs.matchLineByLine;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Examples and Simple test apps to be built and tested for output.
 */
@Tag("simple-apps")
public class SimpleAppTest {

    private static final Logger LOGGER = Logger.getLogger(SimpleAppTest.class.getName());

    @Test
    public void helloWorld(TestInfo testInfo) throws IOException, InterruptedException {
        final App app = App.HELLO_WORLD;
        LOGGER.info("Testing app: " + app.dir);
        Process process = null;
        Path processLog = null;
        final StringBuilder report = new StringBuilder();
        final Path appDir = Path.of(app.dir);
        final String cn = testInfo.getTestClass().get().getCanonicalName();
        final String mn = testInfo.getTestMethod().get().getName();
        try {
            // Cleanup
            deleteAppFiles(app);
            Files.createDirectories(Path.of(app.dir, "logs"));

            // Build
            processLog = Path.of(app.dir, "logs", "build-and-run.log");
            builderRoutine(app, report, cn, mn, appDir, processLog);

            // Run
            final List<String> cmd = List.of(app.buildAndRunCmd.cmds[app.buildAndRunCmd.cmds.length - 1]);
            process = Commands.runCommand(cmd, appDir.toFile(), processLog.toFile());
            process.waitFor(5, TimeUnit.SECONDS);
            Logs.appendln(report, app.dir);
            Logs.appendlnSection(report, String.join(" ", cmd));

            // Test
            final Pattern p = Pattern.compile("hello world(.*)");
            assertTrue(matchLineByLine(p, processLog), "The output should have matched " + p.pattern());

            Commands.processStopper(process, false);
            Logs.checkLog(cn, mn, app, processLog.toFile());
        } finally {
            wrapUp(process, cn, mn, report, app, processLog, Path.of(app.dir, APP_BUILD_OUT_DIR));
        }
    }


    @Test
    public void branches(TestInfo testInfo) throws IOException, InterruptedException {
        final App app = App.BRANCHES;
        LOGGER.info("Testing app: " + app.dir);
        Process process = null;
        Path processLog = null;
        final StringBuilder report = new StringBuilder();
        final Path appDir = Path.of(app.dir);
        final String cn = testInfo.getTestClass().get().getCanonicalName();
        final String mn = testInfo.getTestMethod().get().getName();
        try {
            // Cleanup
            deleteAppFiles(app);
            Files.createDirectories(Path.of(app.dir, "logs"));

            // Build
            processLog = Path.of(app.dir, "logs", "build-and-run.log");
            builderRoutine(app, report, cn, mn, appDir, processLog);

            // Run
            final List<String> cmd = List.of(app.buildAndRunCmd.cmds[app.buildAndRunCmd.cmds.length - 1]);
            process = Commands.runCommand(cmd, appDir.toFile(), processLog.toFile());
            process.waitFor(5, TimeUnit.SECONDS);
            Logs.appendln(report, app.dir);
            Logs.appendlnSection(report, String.join(" ", cmd));

            // Test
            final Pattern p = Pattern.compile(".*1 1.*");
            assertTrue(matchLineByLine(p, processLog),
                    "File " + getLogsDir(cn, mn) + File.separator + processLog.getFileName() + " should have matched " + p.pattern());
            Commands.processStopper(process, false);
            Logs.checkLog(cn, mn, app, processLog.toFile());
        } finally {
            wrapUp(process, cn, mn, report, app, processLog, Path.of(app.dir, APP_BUILD_OUT_DIR));
        }
    }
}
