package cc.quarkus.qcc.tests.integration.utils;

import java.io.File;
import java.nio.file.Path;

import static cc.quarkus.qcc.tests.integration.utils.Commands.getProperty;

/**
 * Example and test apps basic build commands and flags
 */
public final class App {
    public static final String BASE_DIR = Commands.getBaseDir();

    public static final String QCC_RUNTIME_API_JAR = getProperty(
            new String[]{"QCC_RUNTIME_API_JAR", "qcc.runtime.api.jar"},
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "cc", "quarkus", "qcc-runtime-api", "1.0.0-SNAPSHOT", "qcc-runtime-api-1.0.0-SNAPSHOT.jar")
                    .toString());

    public static final String QCC_RUNTIME_MAIN_JAR = getProperty(
            new String[]{"QCC_RUNTIME_MAIN_JAR", "qcc.runtime.main.jar"},
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "cc", "quarkus", "qcc-runtime-main", "1.0.0-SNAPSHOT", "qcc-runtime-main-1.0.0-SNAPSHOT.jar")
                    .toString());

    public static final String QCC_MAIN_JAR = getProperty(
            new String[]{"QCC_MAIN_JAR", "qcc.main.jar"},
            Path.of(BASE_DIR, "main", "target", "qcc-main-1.0.0-SNAPSHOT.jar")
                    .toString());

    public static final String QCCRT_JAVA_BASE_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "cc", "quarkus", "qccrt-java.base", "11.0.1-SNAPSHOT", "qccrt-java.base-11.0.1-SNAPSHOT.jar")
                    .toString();

    public static final String QCCRT_UNWIND_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "cc", "quarkus", "qcc-runtime-unwind", "1.0.0-SNAPSHOT", "qcc-runtime-unwind-1.0.0-SNAPSHOT.jar")
                    .toString();

    public static final String QCC_BOOT_MODULE_PATH = getProperty(
            new String[]{"QCC_BOOT_MODULE_PATH", "qcc.boot.module.path"},
            QCCRT_JAVA_BASE_JAR + ":" + QCCRT_UNWIND_JAR + ":" + QCC_RUNTIME_API_JAR + ":" + QCC_RUNTIME_MAIN_JAR);

    public static final String APP_BUILD_OUT_DIR = "out";

    public static final App HELLO_WORLD = new App(BASE_DIR + File.separator + "examples" + File.separator + "helloworld",
            WhitelistLogLines.HELLO_WORLD,
            BuildAndRunCmd.HELLO_WORLD
    );

    public static final App BRANCHES = new App(BASE_DIR + File.separator + "integration-tests" + File.separator + "apps" + File.separator + "branches",
            WhitelistLogLines.NONE,
            BuildAndRunCmd.BRANCHES
    );

    public final String dir;
    public final WhitelistLogLines whitelistLines;
    public final BuildAndRunCmd buildAndRunCmd;

    public App(String dir, WhitelistLogLines whitelistLines, BuildAndRunCmd buildAndRunCmd) {
        this.dir = dir;
        this.whitelistLines = whitelistLines;
        this.buildAndRunCmd = buildAndRunCmd;
    }
}
