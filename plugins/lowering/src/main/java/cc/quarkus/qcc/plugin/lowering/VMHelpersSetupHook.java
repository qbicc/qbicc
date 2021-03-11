package cc.quarkus.qcc.plugin.lowering;

import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;

/**
 * Unconditionally register VMHelper methods that we may not actually refer to until lowering.
 */
public class VMHelpersSetupHook implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("arrayStoreCheck"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("checkcast_class"));
        ctxt.registerEntryPoint(ctxt.getVMHelperMethod("checkcast_typeId"));
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