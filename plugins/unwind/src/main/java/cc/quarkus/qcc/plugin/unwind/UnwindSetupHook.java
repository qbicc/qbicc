package cc.quarkus.qcc.plugin.unwind;

import cc.quarkus.qcc.context.CompilationContext;

import java.util.function.Consumer;

public class UnwindSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        ctxt.registerEntryPoint(UnwindHelper.get(ctxt).getPersonalityMethod());
    }
}
