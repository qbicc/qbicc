package org.qbicc.main;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Version constants for qbicc.
 */
public final class Version {
    /**
     * The default class library version which is used if none is given on the command line.
     */
    public static final String CLASSLIB_DEFAULT_VERSION;
    /**
     * The version of qbicc itself.
     */
    public static final String QBICC_VERSION;

    static {
        Properties properties = new Properties();
        InputStream inputStream = Version.class.getResourceAsStream("/main.properties");
        if (inputStream == null) {
            throw new Error("Missing main.properties");
        } else try (inputStream) {
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                try (BufferedReader br = new BufferedReader(reader)) {
                    properties.load(br);
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
        CLASSLIB_DEFAULT_VERSION = properties.getProperty("classlib.default-version");
        QBICC_VERSION = properties.getProperty("qbicc.version");
    }

    private Version() {}
}
