package org.qbicc.tests.integration.utils;

import static org.qbicc.tests.integration.utils.PropertyLookup.getProperty;

/**
 * Example and test apps basic constants
 */
public final class TestConstants {
    public static final String BASE_DIR = PropertyLookup.getBaseDir();

    public static final String MAVEN_COMPILER_RELEASE = getProperty(new String[]{"MAVEN_COMPILER_RELEASE", "maven.compiler.release"}, "11");
}
