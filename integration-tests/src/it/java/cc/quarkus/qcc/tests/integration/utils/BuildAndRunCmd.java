package cc.quarkus.qcc.tests.integration.utils;

import java.io.File;

import static cc.quarkus.qcc.tests.integration.utils.App.APP_BUILD_OUT_DIR;
import static cc.quarkus.qcc.tests.integration.utils.App.QCCRT_JAVA_BASE_JAR;
import static cc.quarkus.qcc.tests.integration.utils.App.QCC_BOOT_MODULE_PATH;
import static cc.quarkus.qcc.tests.integration.utils.App.QCC_MAIN_JAR;
import static cc.quarkus.qcc.tests.integration.utils.App.QCC_RUNTIME_API_JAR;

/**
 * BuildAndRunCmds
 *
 * The last command is used to run the final binary.
 * All previous commands are used to build it.
 */
public final class BuildAndRunCmd {

    public static final BuildAndRunCmd HELLO_WORLD = new BuildAndRunCmd(new String[][]{
            new String[]{"javac", "-cp",
                    QCC_RUNTIME_API_JAR,
                    "hello/world/Main.java"},
            new String[]{"jar", "cvf", "main.jar", "hello/world/Main.class"},
            new String[]{"java", "-jar", QCC_MAIN_JAR,
                    "--boot-module-path",
                    "main.jar:" + QCC_BOOT_MODULE_PATH,
                    "--output-path",
                    APP_BUILD_OUT_DIR,
                    "hello.world.Main"},
            new String[]{APP_BUILD_OUT_DIR + File.separator + "a.out"}
    });

    public static final BuildAndRunCmd BRANCHES = new BuildAndRunCmd(new String[][]{
            new String[]{"javac", "-cp",
                    QCC_RUNTIME_API_JAR,
                    "mypackage/Main.java"},
            new String[]{"jar", "cvf", "main.jar", "mypackage/Main.class"},
            new String[]{"java", "-jar", QCC_MAIN_JAR,
                    "--boot-module-path",
                    "main.jar:" + QCC_BOOT_MODULE_PATH,
                    "--output-path",
                    APP_BUILD_OUT_DIR,
                    "mypackage.Main"},
            new String[]{APP_BUILD_OUT_DIR + File.separator + "a.out"}
    });

    public final String[][] cmds;

    public BuildAndRunCmd(String[][] cmds) {
        this.cmds = cmds;
    }
}
