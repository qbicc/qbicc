package cc.quarkus.qcc.plugin.gc.nogc;

import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;

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
