package cc.quarkus.qcc.tests.integration.utils;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cc.quarkus.qcc.tests.integration.utils.App.BASE_DIR;
import static cc.quarkus.qcc.tests.integration.utils.Commands.isBlank;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Logging and reporting facility, assembling markdown report file.
 */
public class Logs {
    private static final Logger LOGGER = Logger.getLogger(Logs.class.getName());
    private static final Pattern WARN_ERROR_DETECTION_PATTERN = Pattern.compile("(?i:.*(ERROR|WARN|No such file|Not found|unknown).*)");

    public static void checkLog(String testClass, String testMethod, App app, File log) throws IOException {
        final Pattern[] whitelistPatterns = new Pattern[app.whitelistLines.errs.length + WhitelistLogLines.ALL.errs.length];
        System.arraycopy(app.whitelistLines.errs, 0, whitelistPatterns, 0, app.whitelistLines.errs.length);
        System.arraycopy(WhitelistLogLines.ALL.errs, 0, whitelistPatterns, app.whitelistLines.errs.length, WhitelistLogLines.ALL.errs.length);
        try (Scanner sc = new Scanner(log, UTF_8)) {
            Set<String> offendingLines = new HashSet<>();
            while (sc.hasNextLine()) {
                final String line = sc.nextLine();
                final boolean error = WARN_ERROR_DETECTION_PATTERN.matcher(line).matches();
                boolean whiteListed = false;
                if (error) {
                    for (Pattern p : whitelistPatterns) {
                        if (p.matcher(line).matches()) {
                            whiteListed = true;
                            LOGGER.info(log.getName() + " log for " + testMethod + " contains whitelisted error: `" + line + "'");
                            break;
                        }
                    }
                    if (!whiteListed) {
                        offendingLines.add(line);
                    }
                }
            }
            assertTrue(offendingLines.isEmpty(),
                    log.getName() + " log should not contain error or warning lines that are not whitelisted. " +
                            "See integration-tests" + File.separator + "target" + File.separator + "archived-logs" +
                            File.separator + testClass + File.separator + testMethod + File.separator + log.getName() +
                            " and check these offending lines: \n" + String.join("\n", offendingLines));
        }
    }

    public static boolean matchLineByLine(Pattern p, Path log) throws IOException {
        try (Scanner sc = new Scanner(log, UTF_8)) {
            while (sc.hasNextLine()) {
                Matcher m = p.matcher(sc.nextLine());
                if (m.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void archive(String testClass, String testMethod, Path file) throws IOException {
        if (file == null || Files.notExists(file)) {
            return;
        }
        if (isBlank(testClass)) {
            throw new IllegalArgumentException("testClass must not be blank");
        }
        if (isBlank(testMethod)) {
            throw new IllegalArgumentException("testMethod must not be blank");
        }
        final Path destDir = getLogsDir(testClass, testMethod);
        Files.createDirectories(destDir);
        if (Files.isDirectory(file)) {
            final Path destDirSub = Path.of(destDir.toString(), file.getFileName().toString());
            Files.walk(file)
                    .forEach(sourcePath -> {
                        try {
                            Files.copy(sourcePath, destDirSub.resolve(file.relativize(sourcePath)), REPLACE_EXISTING);
                        } catch (IOException ex) {
                            // Silence is golden
                        }
                    });
        } else {
            final String filename = file.getFileName().toString();
            Files.copy(file, Paths.get(destDir.toString(), filename), REPLACE_EXISTING);
        }
    }

    public static void writeReport(String testClass, String testMethod, String text) throws IOException {
        final Path destDir = getLogsDir(testClass, testMethod);
        Files.createDirectories(destDir);
        Files.write(Paths.get(destDir.toString(), "report.md"), text.getBytes(UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        final Path aggregateReport = Paths.get(getLogsDir().toString(), "aggregated-report.md");
        if (Files.notExists(aggregateReport)) {
            Files.write(aggregateReport, ("# Aggregated Report\n\n").getBytes(UTF_8), StandardOpenOption.CREATE);
        }
        Files.write(aggregateReport, text.getBytes(UTF_8), StandardOpenOption.APPEND);
    }

    /**
     * Markdown needs two newlines to make a new paragraph.
     */
    public static void appendln(StringBuilder s, String text) {
        s.append(text);
        s.append("\n\n");
    }

    public static void appendlnSection(StringBuilder s, String text) {
        s.append(text);
        s.append("\n\n---\n");
    }

    public static Path getLogsDir(String testClass, String testMethod) {
        return new File(getLogsDir(testClass).toString() + File.separator + testMethod).toPath();
    }

    public static Path getLogsDir(String testClass) {
        return Path.of(getLogsDir().toString(), testClass);
    }

    public static Path getLogsDir() {
        return Path.of(BASE_DIR, "integration-tests", "target", "archived-logs");
    }
}
