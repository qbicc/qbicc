package org.qbicc.plugin.gc.nogc;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.element.InitializerElement;

/**
 *
 */
public class NoGcSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        NoGc.get(ctxt); // Causes NoGc runtime classes to be loaded.
    }
}
