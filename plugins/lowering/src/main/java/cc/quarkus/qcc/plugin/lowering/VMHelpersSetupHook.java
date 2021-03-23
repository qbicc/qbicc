package cc.quarkus.qcc.plugin.lowering;

import cc.quarkus.qcc.context.CompilationContext;

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

        // Helpers to create and throw common runtime exceptions
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseAbstractMethodError"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseArithmeticException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseArrayIndexOutOfBoundsException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseArrayStoreException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseIncompatibleClassChangeError"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseNegativeArraySizeException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseNullPointerException"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("raiseUnsatisfiedLinkError"));
    }
}