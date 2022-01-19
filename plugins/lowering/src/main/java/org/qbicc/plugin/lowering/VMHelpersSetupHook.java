package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

import java.util.List;
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

        // TODO: Temporary until there's a qbicc release to let us annotate the methods in Object and then a class lib release with annotated methods.
        RuntimeMethodFinder methodFinder = RuntimeMethodFinder.get(ctxt);
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("java/lang/Object", "monitorEnter"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("java/lang/Object", "monitorExit"));
    }
}
