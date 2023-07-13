package org.qbicc.plugin.gc.semispace;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.gc.common.AbstractGc;
import org.qbicc.plugin.gc.common.GcFactory;

/**
 *
 */
public final class SemiSpaceGcFactory implements GcFactory {
    @Override
    public AbstractGc createGc(CompilationContext ctxt, String name) {
        return name.equals("semi") ? new SemiSpaceGc(ctxt) : null;
    }
}
