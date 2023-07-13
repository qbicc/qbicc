package org.qbicc.plugin.gc.common;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * Common utilities and setup for GC.
 */
public final class HeapCommon {

    private HeapCommon() {}

    public static void registerIntrinsics(CompilationContext ctxt) {
        registerHeapIntrinsics(ctxt);
    }

    private static void registerHeapIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        LiteralFactory lf = classContext.getLiteralFactory();
        TypeSystem ts = classContext.getTypeSystem();

        ClassTypeDescriptor heapDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/gc/heap/Heap");

        MethodDescriptor emptyToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of());
        MethodDescriptor emptyToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());

        intrinsics.registerIntrinsic(heapDesc, "getConfiguredMinHeapSize", emptyToLong, (builder, targetPtr, arguments) -> {
            // todo: configuration
            // hard-coded 16MB for now
            return lf.literalOf(16L * (1L << 20));
        });

        intrinsics.registerIntrinsic(heapDesc, "getConfiguredMaxHeapSize", emptyToLong, (builder, targetPtr, arguments) -> {
            // todo: configuration
            // hard-coded 128MB for now
            return lf.literalOf(128L * (1L << 20));
        });

        intrinsics.registerIntrinsic(heapDesc, "getConfiguredHeapAlignment", emptyToLong, (builder, targetPtr, arguments) -> {
            // todo: configuration
            // hard-coded 16MB alignment for now
            return lf.literalOf(1L << 24);
        });

        intrinsics.registerIntrinsic(heapDesc, "getConfiguredObjectAlignment", emptyToInt, (builder, targetPtr, arguments) -> {
            // todo: configuration
            // hard-coded to pointer alignment for now
            return lf.literalOf(ts.getPointerAlignment());
        });
    }
}
