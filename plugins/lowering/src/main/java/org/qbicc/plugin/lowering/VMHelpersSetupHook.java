package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.util.function.Consumer;

/**
 * Unconditionally register VMHelper methods that we may not actually refer to until lowering.
 */
public class VMHelpersSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        // Helpers for dynamic type checking
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("arrayStoreCheck"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("checkcast_class"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("checkcast_typeId"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("instanceof_class"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("instanceof_typeId"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("get_class"));

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
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("monitor_enter"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("monitor_exit"));

        // class initialization
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("initialize_class"));
    }
}
