package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;

import java.util.function.Consumer;

/**
 * Load runtime classes that define @AutoQueued methods.
 */
public class VMHelpersSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        String[] rootTypes = new String[] {
            "org/qbicc/runtime/main/VMHelpers",
            "org/qbicc/runtime/main/RuntimeInitializerRunner",
            "org/qbicc/runtime/main/Once",
            "org/qbicc/runtime/stackwalk/MethodData",
            "org/qbicc/runtime/stackwalk/JavaStackFrameCache",
            "org/qbicc/runtime/stackwalk/JavaStackFrameCache"
        };
        for (String type : rootTypes) {
            ctxt.getBootstrapClassContext().findDefinedType(type).load();
        }
    }
}
