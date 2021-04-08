package cc.quarkus.qcc.tests.integration.utils;

import static cc.quarkus.qcc.tests.integration.utils.PropertyLookup.getProperty;

import java.nio.file.Path;

/**
 * Example and test apps basic constants
 */
public final class TestConstants {
    public static final String BASE_DIR = PropertyLookup.getBaseDir();

    public static final String MAVEN_COMPILER_RELEASE = getProperty(new String[]{"MAVEN_COMPILER_RELEASE", "maven.compiler.release"}, "11");

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

    public static final String QCC_RUNTIME_NOGC_JAR = getProperty(
            new String[]{"QCC_RUNTIME_NOGC_JAR", "qcc.runtime.nogc.jar"},
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "cc", "quarkus", "qcc-runtime-gc-nogc", "1.0.0-SNAPSHOT", "qcc-runtime-gc-nogc-1.0.0-SNAPSHOT.jar")
                    .toString());


    public static final String QCCRT_JAVA_BASE_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "org", "qbicc", "rt", "qbicc-rt-java.base", "11.0.1-SNAPSHOT", "qbicc-rt-java.base-11.0.1-SNAPSHOT.jar")
                    .toString();

    public static final String QCCRT_UNWIND_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "cc", "quarkus", "qcc-runtime-unwind", "1.0.0-SNAPSHOT", "qcc-runtime-unwind-1.0.0-SNAPSHOT.jar")
                    .toString();


    public static final String QCCRT_POSIX_JAR =
            Path.of(System.getProperty("user.home"),
                    ".m2", "repository", "cc", "quarkus", "qcc-runtime-posix", "1.0.0-SNAPSHOT", "qcc-runtime-posix-1.0.0-SNAPSHOT.jar")
                    .toString();



}
