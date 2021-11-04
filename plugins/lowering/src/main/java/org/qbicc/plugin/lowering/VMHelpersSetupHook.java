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
        ctxt.enqueue(ctxt.getVMHelperMethod("arrayStoreCheck"));
        ctxt.enqueue(ctxt.getVMHelperMethod("checkcast_class"));
        ctxt.enqueue(ctxt.getVMHelperMethod("checkcast_typeId"));
        ctxt.enqueue(ctxt.getVMHelperMethod("instanceof_class"));
        ctxt.enqueue(ctxt.getVMHelperMethod("instanceof_typeId"));
        ctxt.enqueue(ctxt.getVMHelperMethod("get_class"));
        ctxt.enqueue(ctxt.getVMHelperMethod("classof_from_typeid"));
        ctxt.enqueue(ctxt.getVMHelperMethod("get_superclass"));

        // Helpers to create and throw common runtime exceptions
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseAbstractMethodError"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseArithmeticException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseArrayIndexOutOfBoundsException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseArrayStoreException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseClassCastException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseIncompatibleClassChangeError"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseNegativeArraySizeException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseNullPointerException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseUnsatisfiedLinkError"));

        // Object monitors
        ctxt.enqueue(ctxt.getVMHelperMethod("monitor_enter"));
        ctxt.enqueue(ctxt.getVMHelperMethod("monitor_exit"));

        // class initialization
        ctxt.enqueue(ctxt.getVMHelperMethod("initialize_class"));

        // helper to create j.l.Class instance of an array class at runtime
        ctxt.enqueue(ctxt.getOMHelperMethod("get_or_create_class_for_refarray"));

        // java.lang.Thread
        ctxt.enqueue(ctxt.getVMHelperMethod("JLT_start0"));
        ctxt.enqueue(ctxt.getVMHelperMethod("threadWrapper"));

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
