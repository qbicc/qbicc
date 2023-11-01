package org.qbicc.plugin.wasm;

import static org.qbicc.machine.file.wasm.Ops.*;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.WasmLiteral;
import org.qbicc.machine.file.wasm.model.Memory;
import org.qbicc.machine.file.wasm.model.Segment;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.type.PointerType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public final class WasmIntrinsics {
    private WasmIntrinsics() {}

    public static void register(CompilationContext ctxt) {
        registerDataIntrinsics(ctxt);
        registerMemoryAtomicIntrinsics(ctxt);
    }

    private static void registerMemoryAtomicIntrinsics(final CompilationContext ctxt) {
        Wasm wasm = Wasm.get(ctxt);
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor wasmMemoryAtomicDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/wasm/Wasm$memory$atomic");

        StaticIntrinsic wait32 = (builder, targetPtr, arguments) -> {
            LiteralFactory lf = builder.getLiteralFactory();
            TypeSystem ts = builder.getTypeSystem();
            Value addr = arguments.get(0);
            Value expected = arguments.get(1);
            Value timeout = arguments.get(2);
            if (!(addr.getType() instanceof PointerType addrType && addrType.getSize() == 4)) {
                expectedNonWidePointer(ctxt, builder);
                return lf.literalOf(0);
            }
            int as = addrType.addressSpace();
            Memory mem = getMemoryForInteger(as, wasm);
            if (mem == null) {
                wrongAddressSpace(ctxt, builder);
                return lf.literalOf(0);
            }
            SignedIntegerType s32 = ts.getSignedInteger32Type();
            SignedIntegerType s64 = ts.getSignedInteger64Type();
            WasmLiteral wasmLit = lf.literalOfWasm(b -> b.add(memory.atomic.wait32, mem, 0),
                ts.getFunctionType(s32, List.of(addrType, s32, s64)),
                WasmLiteral.Flag.SIDE_EFFECT,
                WasmLiteral.Flag.NO_THROW
            );
            return builder.call(wasmLit, List.of(addr, expected, timeout));
        };

        StaticIntrinsic wait64 = (builder, targetPtr, arguments) -> {
            LiteralFactory lf = builder.getLiteralFactory();
            TypeSystem ts = builder.getTypeSystem();
            Value addr = arguments.get(0);
            Value expected = arguments.get(1);
            Value timeout = arguments.get(2);
            if (!(addr.getType() instanceof PointerType addrType && addrType.getSize() == 4)) {
                expectedNonWidePointer(ctxt, builder);
                return lf.literalOf(0);
            }
            int as = addrType.addressSpace();
            Memory mem = getMemoryForInteger(as, wasm);
            if (mem == null) {
                wrongAddressSpace(ctxt, builder);
                return lf.literalOf(0);
            }
            SignedIntegerType s32 = ts.getSignedInteger32Type();
            SignedIntegerType s64 = ts.getSignedInteger64Type();
            WasmLiteral wasmLit = lf.literalOfWasm(b -> b.add(memory.atomic.wait64, mem, 0),
                ts.getFunctionType(s32, List.of(addrType, s64, s64)),
                WasmLiteral.Flag.SIDE_EFFECT,
                WasmLiteral.Flag.NO_THROW
            );
            return builder.call(wasmLit, List.of(addr, expected, timeout));
        };

        intrinsics.registerIntrinsic(wasmMemoryAtomicDesc, "wait32", wait32);
        intrinsics.registerIntrinsic(wasmMemoryAtomicDesc, "wait64", wait64);

        StaticIntrinsic notify = (builder, targetPtr, arguments) -> {
            LiteralFactory lf = builder.getLiteralFactory();
            TypeSystem ts = builder.getTypeSystem();
            Value addr = arguments.get(0);
            Value waiterCnt = arguments.get(1);
            if (!(addr.getType() instanceof PointerType addrType && addrType.getSize() == 4)) {
                expectedNonWidePointer(ctxt, builder);
                return lf.literalOf(0);
            }
            int as = addrType.addressSpace();
            UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
            Memory mem = getMemoryForInteger(as, wasm);
            if (mem == null) {
                wrongAddressSpace(ctxt, builder);
                return lf.literalOf(u32, 0);
            }
            WasmLiteral wasmLit = lf.literalOfWasm(b -> b.add(memory.atomic.notify, mem, 0),
                ts.getFunctionType(u32, List.of(addrType, u32)),
                WasmLiteral.Flag.SIDE_EFFECT,
                WasmLiteral.Flag.NO_THROW
            );
            return builder.call(wasmLit, List.of(addr, waiterCnt));
        };

        intrinsics.registerIntrinsic(wasmMemoryAtomicDesc, "notify", notify);
    }

    private static Diagnostic wrongAddressSpace(final CompilationContext ctxt, final BasicBlockBuilder builder) {
        return ctxt.error(builder.getLocation(), "Invalid address space for intrinsic operation");
    }

    private static Diagnostic expectedNonWidePointer(final CompilationContext ctxt, final BasicBlockBuilder builder) {
        return ctxt.error(builder.getLocation(), "Expected non-wide pointer as first argument to intrinsic");
    }

    private static void registerDataIntrinsics(final CompilationContext ctxt) {
        Wasm wasm = Wasm.get(ctxt);
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor wasmDataDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/wasm/Wasm$data");

        StaticIntrinsic drop = (builder, targetPtr, arguments) -> {
            LiteralFactory lf = builder.getLiteralFactory();
            TypeSystem ts = builder.getTypeSystem();
            Value arg = arguments.get(0);
            if (!(arg.getType() instanceof PointerType pt && pt.addressSpace() == 258 && pt.getSize() == 1)) {
                ctxt.error(builder.getLocation(), "Expected one-byte pointer in address space 258 as argument to intrinsic");
            }
            if (arg instanceof BitCastLiteral bcl && bcl.getValue() instanceof IntegerLiteral il) {
                Segment segment = getSegmentForInteger(il.intValue(), wasm);
                if (segment == null) {
                    ctxt.error(builder.getLocation(), "Unknown data segment identifier %d", il);
                    return builder.emptyVoid();
                }
                WasmLiteral wasmLit = lf.literalOfWasm(b -> b.add(data.drop, segment),
                    ts.getFunctionType(ts.getVoidType(), List.of(ts.getSignedInteger32Type())),
                    WasmLiteral.Flag.SIDE_EFFECT, WasmLiteral.Flag.NO_THROW
                );
                return builder.call(wasmLit, List.of());
            } else {
                ctxt.error(builder.getLocation(), "Expected constant argument");
                return builder.emptyVoid();
            }
        };

        intrinsics.registerIntrinsic(wasmDataDesc, "drop", drop);
    }

    private static Memory getMemoryForInteger(final int addrSpace, final Wasm wasm) {
        return switch (addrSpace) {
            case 0 -> wasm.globalMemory();
            case 1 -> wasm.heapMemory();
            case 128 -> wasm.threadLocalMemory();
            default -> null;
        };
    }

    private static Segment getSegmentForInteger(final int val, final Wasm wasm) {
        return switch (val) {
            case 0 -> wasm.globalData();
            case 1 -> wasm.initialHeap();
            case 2 -> wasm.initialThreadLocal();
            default -> null;
        };
    }
}
