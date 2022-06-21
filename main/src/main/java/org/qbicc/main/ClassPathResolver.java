package org.qbicc.main;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.DiagnosticContext;
import org.qbicc.driver.ClassPathItem;

/**
 * A class path resolver.
 */
public interface ClassPathResolver {
    void resolveClassPath(DiagnosticContext ctxt, Consumer<ClassPathItem> classPathItemConsumer, final List<ClassPathEntry> paths) throws IOException;
}