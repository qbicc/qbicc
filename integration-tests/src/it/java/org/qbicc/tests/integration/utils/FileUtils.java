package org.qbicc.tests.integration.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 *
 */
public final class FileUtils {
    public static final boolean cleanTarget = Boolean.parseBoolean(System.getProperty("qbicc.test.clean-target", "false"));

    public static void deleteRecursively(Path path) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path subPath : stream) {
                    deleteRecursively(subPath);
                }
            } catch (IOException ignored) {
            }
        } else {
            try {
                Files.delete(path);
            } catch (IOException ignored) {
            }
        }
    }
}
