package org.qbicc.plugin.gc.nogc;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;

/**
 *
 */
public class NoGcSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        ctxt.registerEntryPoint(NoGc.get(ctxt).getAllocateMethod());
        ctxt.registerEntryPoint(NoGc.get(ctxt).getCopyMethod());
        ctxt.registerEntryPoint((NoGc.get(ctxt).getZeroMethod()));
    }
}
