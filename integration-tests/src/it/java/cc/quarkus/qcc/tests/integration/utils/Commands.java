package cc.quarkus.qcc.tests.integration.utils;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cc.quarkus.qcc.tests.integration.utils.App.APP_BUILD_OUT_DIR;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utilities to run and stop processes, clean dirs etc.
 */
public class Commands {
    private static final Logger LOGGER = Logger.getLogger(Commands.class.getName());

    public static String getProperty(String[] alternatives, String defaultValue) {
        String prop = null;
        for (String p : alternatives) {
            String env = System.getenv().get(p);
            if (!isBlank(env)) {
                prop = env;
                break;
            }
            String sys = System.getProperty(p);
            if (!isBlank(sys)) {
                prop = sys;
                break;
            }
        }
        if (prop == null) {
            LOGGER.info("Failed to detect any of " + String.join(", ", alternatives) +
                    " as env or sys props, defaulting to " + defaultValue);
            return defaultValue;
        }
        return prop;
    }

    public static String getBaseDir() {
        final String env = System.getenv().get("basedir");
        final String sys = System.getProperty("basedir");
        if (!isBlank(env)) {
            return new File(env).getParent();
        }
        if (isBlank(sys)) {
            throw new IllegalArgumentException("Unable to determine project.basedir.");
        }
        return new File(sys).getParent();
    }

    public static void delete(String glob, Path... paths) {
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);
        for (Path path : paths) {
            try {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .filter(matcher::matches)
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                //Silence is golden
            }
        }
    }

    public static void builderRoutine(int steps, App app, StringBuilder report, String cn, String mn, Path appDir, Path processLog) throws InterruptedException {
        // The last command is reserved for running it
        assertTrue(app.buildAndRunCmd.cmds.length > 1);
        Logs.appendln(report, "# " + cn + ", " + mn);
        for (int i = 0; i < steps; i++) {
            // We cannot run commands in parallel, we need them to follow one after another
            final ExecutorService buildService = Executors.newFixedThreadPool(1);
            final List<String> cmd = List.of(app.buildAndRunCmd.cmds[i]);
            buildService.submit(new Commands.ProcessRunner(appDir, processLog, cmd, 10)); // might take a long time....
            Logs.appendln(report, (new Date()).toString());
            Logs.appendln(report, appDir.toString());
            Logs.appendlnSection(report, String.join(" ", cmd));
            buildService.shutdown();
            buildService.awaitTermination(10, TimeUnit.MINUTES); // Native image build might take a long time....
        }
        assertTrue(processLog.toFile().exists());
    }

    public static void builderRoutine(App app, StringBuilder report, String cn, String mn, Path appDir, Path processLog) throws InterruptedException {
        builderRoutine(app.buildAndRunCmd.cmds.length - 1, app, report, cn, mn, appDir, processLog);
    }

    public static void deleteAppFiles(App app) {
        delete("glob:**/{target," + APP_BUILD_OUT_DIR + ",logs}**", Path.of(app.dir));
        delete("glob:**/*.{class,jar}", Path.of(app.dir));
    }

    public static void wrapUp(Process process, String cn, String mn, StringBuilder report, App app, Path... toArchive)
            throws InterruptedException, IOException {
        // Make sure processes are down even if there was an exception / failure
        if (process != null) {
            Commands.processStopper(process, true);
        }
        // Archive logs no matter what
        for (Path fileToArchive : toArchive) {
            Logs.archive(cn, mn, fileToArchive);
        }
        Logs.writeReport(cn, mn, report.toString());
        deleteAppFiles(app);
    }

    /**
     * There might be this weird glitch where native-image command completes
     * but the FS does not appear to have the resulting binary ready and executable for the
     * next process *immediately*. Hence this small wait that mitigates this glitch.
     *
     * Note that nothing happens at the end of the timeout and the TS hopes for the best.
     *
     * @param command
     * @param directory
     */
    public static void waitForExecutable(List<String> command, File directory) {
        long now = System.currentTimeMillis();
        final long startTime = now;
        while (now - startTime < 1000) {
            if (new File(directory.getAbsolutePath() + File.separator + command.get(command.size() - 1)).canExecute()) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            now = System.currentTimeMillis();
        }
    }

    public static Process runCommand(List<String> command, File directory, File logFile, File input) throws IOException {
        waitForExecutable(command, directory);
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        final Map<String, String> envA = processBuilder.environment();
        envA.put("PATH", System.getenv("PATH"));
        processBuilder.directory(directory)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        if (input != null) {
            processBuilder.redirectInput(input);
        }
        return processBuilder.start();
    }

    public static Process runCommand(List<String> command, File directory, File logFile) throws IOException {
        return runCommand(command, directory, logFile, null);
    }

    public static void pidKiller(long pid, boolean force) {
        try {
            Runtime.getRuntime().exec(new String[]{"kill", force ? "-9" : "-15", Long.toString(pid)});
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void processStopper(Process p, boolean force) throws InterruptedException {
        p.children().forEach(child -> {
            if (child.supportsNormalTermination()) {
                child.destroy();
            }
            pidKiller(child.pid(), force);
        });
        if (p.supportsNormalTermination()) {
            p.destroy();
            p.waitFor(3, TimeUnit.MINUTES);
        }
        pidKiller(p.pid(), force);
    }

    public static class ProcessRunner implements Runnable {
        final Path directory;
        final Path log;
        final List<String> command;
        final long timeoutMinutes;

        public ProcessRunner(Path directory, Path log, List<String> command, long timeoutMinutes) {
            this.directory = directory;
            this.log = log;
            this.command = command;
            this.timeoutMinutes = timeoutMinutes;
        }

        @Override
        public void run() {
            final ProcessBuilder pb = new ProcessBuilder(command);
            final Map<String, String> env = pb.environment();
            env.put("PATH", System.getenv("PATH"));
            pb.directory(directory.toFile());
            pb.redirectErrorStream(true);
            Process p = null;
            try {
                if (!log.toFile().exists()) {
                    Files.createFile(log);
                }
                Files.write(log,
                        ("Command: " + String.join(" ", command) + "\n").getBytes(), StandardOpenOption.APPEND);
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
                p = pb.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Objects.requireNonNull(p, "command " + command + " not found/invalid")
                        .waitFor(timeoutMinutes, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static boolean isBlank(final String s) {
        return s == null || s.isBlank();
    }
}
