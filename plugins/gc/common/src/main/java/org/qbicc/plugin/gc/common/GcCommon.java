package org.qbicc.plugin.gc.common;

import static org.qbicc.graph.CmpAndSwap.Strength.STRONG;
import static org.qbicc.graph.atomic.AccessModes.SingleOpaque;

import java.util.List;
import java.util.Map;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.HeaderBits;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * Common utilities and setup for GC.
 */
public final class GcCommon {
    private static final HeaderBits.Key MARK_BIT = new HeaderBits.Key(1);
    private static final HeaderBits.Key MOVED_BIT = new HeaderBits.Key(1);

    private GcCommon() {}

    public static void reserveMarkBit(final CompilationContext ctxt) {
        HeaderBits.get(ctxt).getHeaderBits(MARK_BIT);
    }

    public static void reserveMovedBit(CompilationContext ctxt) {
        HeaderBits.get(ctxt).getHeaderBits(MOVED_BIT);
    }

    public static void registerIntrinsics(CompilationContext ctxt) {
        registerHeapIntrinsics(ctxt);
        registerGcIntrinsics(ctxt);
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

    private static void registerGcIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        LiteralFactory lf = classContext.getLiteralFactory();
        TypeSystem ts = classContext.getTypeSystem();

        ClassTypeDescriptor gcDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/gc/Gc");
        ClassTypeDescriptor refDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$reference");

        MethodDescriptor refToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(refDesc));

        final HeaderBits headerBits = HeaderBits.get(ctxt);
        final int idx = headerBits.getHeaderBits(MARK_BIT);
        final IntegerLiteral markBitLiteral = lf.literalOf(headerBits.getHeaderType(), 1L << idx);
        final CoreClasses cc = CoreClasses.get(ctxt);
        final InstanceFieldElement headerField = cc.getObjectHeaderField();
        intrinsics.registerIntrinsic(gcDesc, "setHeaderMarkBit", refToBool, (builder, targetPtr, arguments) -> {
            BlockLabel loop = new BlockLabel();
            BlockLabel resume = new BlockLabel();
            BlockLabel cas = new BlockLabel();
            final Value ptr = builder.decodeReference(arguments.get(0));
            final Value hfPtr = builder.instanceFieldOf(ptr, headerField);
            // enter the loop with a load
            builder.goto_(loop, Map.of(Slot.temp(0), builder.load(hfPtr, SingleOpaque)));
            // part 1: check to see if the mark bit is set
            final UnsignedIntegerType headerType = headerBits.getHeaderType();
            builder.begin(loop, sb -> {
                final BlockParameter oldVal = sb.addParam(loop, Slot.temp(0), headerType);
                final Value and = sb.and(oldVal, markBitLiteral);
                final Value isUnmarked = sb.isEq(and, lf.literalOf(headerType, 0));
                sb.if_(isUnmarked, cas, resume, Map.of(Slot.temp(0), oldVal, Slot.temp(1), lf.literalOf(false)));
            });
            // part 2: the mark bit is clear; try to atomically set it, or retry on fail
            builder.begin(cas, sb -> {
                final CompoundType casResultType = CmpAndSwap.getResultType(ctxt, headerType);
                final BlockParameter oldVal = sb.addParam(cas, Slot.temp(0), headerType);
                Value or = sb.or(oldVal, markBitLiteral);
                final Value casResult = sb.cmpAndSwap(hfPtr, oldVal, or, SingleOpaque, SingleOpaque, STRONG);
                final Value witness = sb.extractMember(casResult, casResultType.getMember(0));
                final Value success = sb.extractMember(casResult, casResultType.getMember(1));
                // on success, go to resume with temp 1 = true; on failure, go to loop with temp 0 (oldVal) = witness
                sb.if_(success, resume, loop, Map.of(Slot.temp(0), witness, Slot.temp(1), lf.literalOf(true)));
            });
            // result is in temp 1
            builder.begin(resume);
            return builder.addParam(resume, Slot.temp(1), ts.getBooleanType());
        });
    }
}
