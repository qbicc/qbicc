package org.qbicc.plugin.gc.semispace;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.gc.common.AbstractGc;

/**
 *
 */
public final class SemiSpaceGc extends AbstractGc {

    SemiSpaceGc(CompilationContext ctxt) {
        super(ctxt, "semi");
    }
}
