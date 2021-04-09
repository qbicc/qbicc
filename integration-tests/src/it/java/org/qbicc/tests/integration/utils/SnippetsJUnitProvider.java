package org.qbicc.tests.integration.utils;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static org.qbicc.tests.integration.utils.TestConstants.BASE_DIR;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Scans integration-tests/snippets directory recursively looking for
 * all .java files. Each .java file must have a corresponding .pattern file
 * with the expected output of the .java program.
 * The content of the .pattern file is compiled into java.util.regex.Pattern.
 */
public class SnippetsJUnitProvider implements ArgumentsProvider {
    private static final Logger LOGGER = Logger.getLogger(SnippetsJUnitProvider.class.getName());

    private final List<Arguments> snippets = new ArrayList<>();

    public SnippetsJUnitProvider() {
        final Path rootPath = FileSystems.getDefault().getPath(BASE_DIR, "integration-tests", "snippets");
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes fileAttributes) {
                    final String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".java")) {
                        try {
                            final Pattern pattern = Pattern.compile(
                                    Files.readString(
                                            Path.of(file.getParent().toString(), fileName.replace(".java", ".pattern")))
                            );
                            LOGGER.debug("FILE NAME: " + fileName + " FILE LOCATION:" + file.getParent() + " PATTERN: " + pattern.pattern());
                            snippets.add(Arguments.of(file, pattern));
                        } catch (IOException | PatternSyntaxException e) {
                            fail("There was an error loading .pattern sister file for " + file + ". Aborting.", e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            fail("There was an error loading snippets in " + rootPath + ". Aborting.", e);
        }
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return snippets.stream();
    }
}
