package org.qbicc.plugin.gc.common;

import org.qbicc.context.CompilationContext;

/**
 *
 */
public interface GcFactory {
    AbstractGc createGc(CompilationContext ctxt, String name);
}
