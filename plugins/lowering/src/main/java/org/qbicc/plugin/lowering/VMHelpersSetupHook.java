package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Unconditionally register VMHelper or ObjectModel methods that we may not actually refer to until lowering.
 */
public class VMHelpersSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        RuntimeMethodFinder methodFinder = RuntimeMethodFinder.get(ctxt);
        // Helpers for dynamic type checking
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("arrayStoreCheck"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("checkcastClass"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("checkcastTypeId"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("instanceofClass"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("instanceofTypeId"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("getClassFromObject"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("getClassFromTypeId"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("getSuperClass"));

        // Helpers to create and throw common runtime exceptions
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseAbstractMethodError"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseArithmeticException"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseArrayIndexOutOfBoundsException"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseArrayStoreException"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseClassCastException"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseIncompatibleClassChangeError"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseNegativeArraySizeException"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseNullPointerException"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("raiseUnsatisfiedLinkError"));

        // Object monitors
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("monitorEnter"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("monitorExit"));

        // helper to create j.l.Class instance of an array class at runtime
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("getOrCreateClassForRefArray"));

        // java.lang.Thread
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("JLT_start0"));

        // Helpers for stack walk
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("org/qbicc/runtime/stackwalk/MethodData", "fillStackTraceElements"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackWalker", "getFrameCount"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackWalker", "walkStack"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackFrameCache", "getSourceCodeIndexList"));
        ctxt.registerAutoQueuedElement(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackFrameCache", "visitFrame"));
        ctxt.registerAutoQueuedElement(methodFinder.getConstructor("org/qbicc/runtime/stackwalk/JavaStackFrameCache",
            MethodDescriptor.synthesize(ctxt.getBootstrapClassContext(), BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I))));
    }
}
