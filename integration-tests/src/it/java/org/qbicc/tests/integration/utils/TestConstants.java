package org.qbicc.tests.integration.utils;

import static org.qbicc.tests.integration.utils.PropertyLookup.getProperty;

import java.nio.file.Path;

/**
 * Example and test apps basic constants
 */
public final class TestConstants {
    public static final String BASE_DIR = PropertyLookup.getBaseDir();

    public static final String MAVEN_COMPILER_RELEASE = getProperty(new String[]{"MAVEN_COMPILER_RELEASE", "maven.compiler.release"}, "11");

    public static final String QBICC_RUNTIME_API_JAR = getProperty(
            new String[]{"QBICC_RUNTIME_API_JAR", "qbicc.runtime.api.jar"},
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "qbicc-runtime-api", "0.1.0-SNAPSHOT", "qbicc-runtime-api-0.1.0-SNAPSHOT.jar")
                    .toString());

    public static final String QBICC_RUNTIME_MAIN_JAR = getProperty(
            new String[]{"QBICC_RUNTIME_MAIN_JAR", "qbicc.runtime.main.jar"},
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "qbicc-runtime-main", "0.1.0-SNAPSHOT", "qbicc-runtime-main-0.1.0-SNAPSHOT.jar")
                    .toString());

    public static final String QBICC_RUNTIME_NOGC_JAR = getProperty(
            new String[]{"QBICC_RUNTIME_NOGC_JAR", "qbicc.runtime.nogc.jar"},
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "qbicc-runtime-gc-nogc", "0.1.0-SNAPSHOT", "qbicc-runtime-gc-nogc-0.1.0-SNAPSHOT.jar")
                    .toString());

    public static final String QBICC_RT_JAVA_BASE_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "rt", "qbicc-rt-java.base", "11.0.1-SNAPSHOT", "qbicc-rt-java.base-11.0.1-SNAPSHOT.jar")
                    .toString();

    public static final String QBICC_RT_UNWIND_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "qbicc-runtime-unwind", "0.1.0-SNAPSHOT", "qbicc-runtime-unwind-0.1.0-SNAPSHOT.jar")
                    .toString();

    public static final String QBICC_RT_POSIX_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "qbicc-runtime-posix", "0.1.0-SNAPSHOT", "qbicc-runtime-posix-0.1.0-SNAPSHOT.jar")
                    .toString();

    public static final String QBICC_RT_LINUX_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "qbicc-runtime-linux", "0.1.0-SNAPSHOT", "qbicc-runtime-linux-0.1.0-SNAPSHOT.jar")
                    .toString();
}
