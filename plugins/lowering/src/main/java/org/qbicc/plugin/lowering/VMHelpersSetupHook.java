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
        ctxt.enqueue(methodFinder.getMethod("arrayStoreCheck"));
        ctxt.enqueue(methodFinder.getMethod("checkcastClass"));
        ctxt.enqueue(methodFinder.getMethod("checkcastTypeId"));
        ctxt.enqueue(methodFinder.getMethod("instanceofClass"));
        ctxt.enqueue(methodFinder.getMethod("instanceofTypeId"));
        ctxt.enqueue(methodFinder.getMethod("getClassFromObject"));
        ctxt.enqueue(methodFinder.getMethod("getClassFromTypeId"));
        ctxt.enqueue(methodFinder.getMethod("getSuperClass"));

        // Helpers to create and throw common runtime exceptions
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseAbstractMethodError"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseArithmeticException"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseArrayIndexOutOfBoundsException"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseArrayStoreException"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseClassCastException"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseIncompatibleClassChangeError"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseNegativeArraySizeException"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseNullPointerException"));
        ctxt.registerEntryPoint(methodFinder.getMethod("raiseUnsatisfiedLinkError"));

        // Object monitors
        ctxt.enqueue(methodFinder.getMethod("monitorEnter"));
        ctxt.enqueue(methodFinder.getMethod("monitorExit"));

        // helper to create j.l.Class instance of an array class at runtime
        ctxt.enqueue(methodFinder.getMethod("getOrCreateClassForRefArray"));

        // java.lang.Thread
        ctxt.enqueue(methodFinder.getMethod("JLT_start0"));
        ctxt.enqueue(methodFinder.getMethod("threadWrapper"));

        // Helpers for stack walk
        ctxt.enqueue(methodFinder.getMethod("org/qbicc/runtime/stackwalk/MethodData", "fillStackTraceElements"));
        ctxt.enqueue(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackWalker", "getFrameCount"));
        ctxt.enqueue(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackWalker", "walkStack"));
        ctxt.enqueue(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackFrameCache", "getSourceCodeIndexList"));
        ctxt.enqueue(methodFinder.getMethod("org/qbicc/runtime/stackwalk/JavaStackFrameCache", "visitFrame"));
        ctxt.enqueue(methodFinder.getConstructor("org/qbicc/runtime/stackwalk/JavaStackFrameCache",
            MethodDescriptor.synthesize(ctxt.getBootstrapClassContext(), BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I))));
    }
}
