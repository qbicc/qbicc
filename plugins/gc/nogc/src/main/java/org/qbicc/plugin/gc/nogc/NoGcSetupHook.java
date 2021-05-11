package org.qbicc.plugin.gc.nogc;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.element.InitializerElement;

/**
 *
 */
public class NoGcSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        ctxt.registerEntryPoint(NoGc.get(ctxt).getAllocateMethod());
        ctxt.registerEntryPoint(NoGc.get(ctxt).getCopyMethod());
        ctxt.registerEntryPoint(NoGc.get(ctxt).getZeroMethod());

        // Annoying since this clinit should be empty, but we need to make the clinit an entrypoint if we are making its static methods entrypoints.
        InitializerElement clinit = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/gc/nogc/NoGcHelpers").load().getInitializer();
        if (clinit != null) {
            ctxt.registerEntryPoint(clinit);
        }
    }
}
