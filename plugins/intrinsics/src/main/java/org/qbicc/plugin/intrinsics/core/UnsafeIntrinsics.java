package org.qbicc.plugin.intrinsics.core;

import static org.qbicc.graph.CmpAndSwap.Strength.STRONG;
import static org.qbicc.graph.CmpAndSwap.Strength.WEAK;
import static org.qbicc.graph.atomic.AccessModes.*;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.InstanceMethodLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * Intrinsics for the {@code Unsafe} class. There are so many that it needs its own class.
 */
public class UnsafeIntrinsics {
    public static void register(CompilationContext ctxt) {
        registerEmptyLateIntrinsics(ctxt);
        registerUnsafeConstantsIntrinsics(ctxt);
        registerCompareAndExchangeIntrinsics(ctxt);
        registerCompareAndSetIntrinsics(ctxt);
        registerGetAndModIntrinsics(ctxt);
        registerGetIntrinsics(ctxt);
        registerPutIntrinsics(ctxt);
        registerFenceIntrinsics(ctxt);
        registerFieldAndArrayIntrinsics(ctxt);
    }

    private static void registerEmptyLateIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");

        MethodDescriptor classToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(classDesc));
        MethodDescriptor classToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(classDesc));

        // These are both late expanded intrinsics so we can interpret them differently in the ADD phase.
        intrinsics.registerIntrinsic(Phase.ANALYZE, unsafeDesc, "ensureClassInitialized", classToVoid,
            (builder, targetPtr, arguments) -> ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType()));
        intrinsics.registerIntrinsic(Phase.ANALYZE, unsafeDesc, "shouldBeInitialized0", classToBool,
            (builder, instance, targetPtr, arguments) -> ctxt.getLiteralFactory().literalOf(false));
    }

    private static void registerUnsafeConstantsIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/UnsafeConstants");

        MethodDescriptor voidToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());
        MethodDescriptor voidToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        intrinsics.registerIntrinsic(unsafeDesc, "targetAddressSize", voidToInt,
            (builder, targetPtr, arguments) -> ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getSignedInteger32Type(), ctxt.getTypeSystem().getPointerSize()));
        intrinsics.registerIntrinsic(unsafeDesc, "targetBigEndian", voidToBool,
            (builder, targetPtr, arguments) -> ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getEndianness().equals(ByteOrder.BIG_ENDIAN)));
    }

    // Compare and exchange

    private static MethodDescriptor getCompareAndExchangeDesc(ClassContext classContext, TypeDescriptor type) {
        return MethodDescriptor.synthesize(
            classContext,
            type,
            List.of(
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Object"),
                BaseTypeDescriptor.J,
                type,
                type
            )
        );
    }

    private static Value doCompareAndExchange(CompilationContext ctxt, BasicBlockBuilder builder, List<Value> arguments, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        Value obj = arguments.get(0);
        Value offset = arguments.get(1);
        Value expect = arguments.get(2);
        Value update = arguments.get(3);

        ValueType expectType = expect.getType();
        if (expectType instanceof ReferenceType) {
            ObjectType objectType = CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getObjectType();
            expectType = objectType.getReference();
        }
        Value handle = builder.byteOffsetPointer(builder.decodeReference(obj), offset, expectType);
        Value result = builder.cmpAndSwap(handle, expect, update, readMode, writeMode, strength);
        // result is a compound structure; extract the result
        return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, expectType).getMember(0));
    }

    private static void registerCompareAndExchangeIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        // Rely on JDK implementations for smaller-than-int sizes; LLVM does not like misaligned atomics
        // todo: research to discover if some platforms handle misalignment correctly, make it switchable

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeInt", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeIntAcquire", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, GlobalAcquire, SingleOpaque, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeIntRelease", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, SingleOpaque, GlobalRelease, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeLong", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeLongAcquire", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, GlobalAcquire, SingleOpaque, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeLongRelease", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, SingleOpaque, GlobalRelease, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeObject", getCompareAndExchangeDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeObjectAcquire", getCompareAndExchangeDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, GlobalAcquire, SingleOpaque, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeObjectRelease", getCompareAndExchangeDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndExchange(ctxt, builder, arguments, SingleOpaque, GlobalRelease, STRONG));


    }

    // Compare and set

    private static MethodDescriptor getCompareAndSetDesc(ClassContext classContext, TypeDescriptor type) {
        return MethodDescriptor.synthesize(
            classContext,
            BaseTypeDescriptor.Z,
            List.of(
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Object"),
                BaseTypeDescriptor.J,
                type,
                type
            )
        );
    }

    private static Value doCompareAndSet(CompilationContext ctxt, BasicBlockBuilder builder, List<Value> arguments, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        Value obj = arguments.get(0);
        Value offset = arguments.get(1);
        Value expect = arguments.get(2);
        Value update = arguments.get(3);

        ValueType expectType = expect.getType();
        if (expectType instanceof ReferenceType) {
            ObjectType objectType = CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getObjectType();
            expectType = objectType.getReference();
        }
        Value handle = builder.byteOffsetPointer(builder.decodeReference(obj), offset, expectType);
        Value result = builder.cmpAndSwap(handle, expect, update, readMode, writeMode, strength);
        // result is a compound structure; extract the success flag
        return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, expectType).getMember(1));
    }

    private static void registerCompareAndSetIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndSetInt", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndSetLong", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndSetObject", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndSetReference", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetInt", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetIntAcquire", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, GlobalAcquire, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetIntPlain", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SinglePlain, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetIntRelease", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalRelease, WEAK));

        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLong", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLongAcquire", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, GlobalAcquire, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLongPlain", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLongRelease", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalRelease, WEAK));

        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObject", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObjectAcquire", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, GlobalAcquire, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObjectPlain", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObjectRelease", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalRelease, WEAK));

        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetReference", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalSeqCst, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetReferenceAcquire", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, GlobalAcquire, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetReferencePlain", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, SingleOpaque, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetReferenceRelease", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, targetPtr, arguments) -> doCompareAndSet(ctxt, builder, arguments, SingleOpaque, GlobalRelease, WEAK));
    }


    // Get and bin-op

    private static MethodDescriptor getGetAndBinOpDesc(ClassContext classContext, TypeDescriptor type) {
        return MethodDescriptor.synthesize(
            classContext,
            type,
            List.of(
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Object"),
                BaseTypeDescriptor.J,
                type
            )
        );
    }

    private static Value doGetAndModify(CompilationContext ctxt, BasicBlockBuilder builder, ReadModifyWrite.Op op, List<Value> arguments, ReadAccessMode readMode, WriteAccessMode writeMode) {
        Value obj = arguments.get(0);
        Value offset = arguments.get(1);
        Value operand = arguments.get(2);

        ValueType operandType = operand.getType();
        if (operandType instanceof ReferenceType) {
            ObjectType objectType = CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getObjectType();
            operandType = objectType.getReference();
        }
        Value handle = builder.byteOffsetPointer(builder.decodeReference(obj), offset, operandType);
        return builder.readModifyWrite(handle, op, operand, readMode, writeMode);
    }

    private static void registerGetAndModIntrinsics(final CompilationContext ctxt) {
        // Rely on JDK implementations for smaller-than-int sizes; LLVM does not like misaligned atomics
        // todo: research to discover if some platforms handle misalignment correctly, make it switchable

        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");

        for (Map.Entry<String, BaseTypeDescriptor> typeNameAndDesc : Map.of(
            "Int", BaseTypeDescriptor.I,
            "Long", BaseTypeDescriptor.J
        ).entrySet()) {
            for (Map.Entry<String, ReadModifyWrite.Op> nameAndOp : Map.of(
                "Add", ReadModifyWrite.Op.ADD,
                "BitwiseAnd", ReadModifyWrite.Op.BITWISE_AND,
                "BitwiseOr", ReadModifyWrite.Op.BITWISE_OR,
                "BitwiseXor", ReadModifyWrite.Op.BITWISE_XOR,
                "Set", ReadModifyWrite.Op.SET
            ).entrySet()) {
                for (Map.Entry<String, AccessMode[]> suffixAndMode : Map.of(
                    "", new AccessMode[] { GlobalSeqCst, GlobalSeqCst },
                    "Acquire", new AccessMode[] { GlobalAcquire, SingleOpaque },
                    "Release", new AccessMode[] { SingleOpaque, GlobalRelease }
                ).entrySet()) {
                    String name = "getAnd" + nameAndOp.getKey() + typeNameAndDesc.getKey() + suffixAndMode.getKey();
                    MethodDescriptor desc = getGetAndBinOpDesc(classContext, typeNameAndDesc.getValue());
                    intrinsics.registerIntrinsic(unsafeDesc, name, desc, (builder, instance, targetPtr, arguments) ->
                        doGetAndModify(ctxt, builder, nameAndOp.getValue(), arguments, (ReadAccessMode) suffixAndMode.getValue()[0], (WriteAccessMode) suffixAndMode.getValue()[1]));
                }
            }
        }
    }

    // Get

    private static MethodDescriptor getGetOpDesc(ClassContext classContext, TypeDescriptor type) {
        return MethodDescriptor.synthesize(
            classContext,
            type,
            List.of(
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Object"),
                BaseTypeDescriptor.J
            )
        );
    }

    private static void registerGetIntrinsics(final CompilationContext ctxt) {
        // Rely on JDK implementations for smaller-than-int sizes; LLVM does not like misaligned atomics
        // todo: research to discover if some platforms handle misalignment correctly, make it switchable

        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        TypeSystem ts = ctxt.getTypeSystem();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        Map<TypeDescriptor, ValueType> typeLookup = Map.of(
            BaseTypeDescriptor.B, ts.getSignedInteger8Type(),
            BaseTypeDescriptor.S, ts.getSignedInteger16Type(),
            BaseTypeDescriptor.I, ts.getSignedInteger32Type(),
            BaseTypeDescriptor.J, ts.getSignedInteger64Type(),
            BaseTypeDescriptor.Z, ts.getUnsignedInteger8Type(),
            BaseTypeDescriptor.C, ts.getUnsignedInteger16Type(),
            BaseTypeDescriptor.F, ts.getFloat32Type(),
            BaseTypeDescriptor.D, ts.getFloat64Type(),
            objDesc, CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getObjectType().getReference()
        );

        for (Map.Entry<String, TypeDescriptor> typeNameAndDesc : Map.of(
            "Byte", BaseTypeDescriptor.B,
            "Short", BaseTypeDescriptor.S,
            "Int", BaseTypeDescriptor.I,
            "Long", BaseTypeDescriptor.J,
            "Boolean", BaseTypeDescriptor.Z,
            "Char", BaseTypeDescriptor.C,
            "Float", BaseTypeDescriptor.F,
            "Double", BaseTypeDescriptor.D,
            "Object", objDesc,
            "Reference", objDesc
        ).entrySet()) {
            for (Map.Entry<String, ReadAccessMode> suffixAndMode : Map.of(
                "", SinglePlain,
                "Acquire", GlobalAcquire,
                "Opaque", SingleOpaque,
                "Volatile", GlobalSeqCst
            ).entrySet()) {
                String name = "get" + typeNameAndDesc.getKey() + suffixAndMode.getKey();
                MethodDescriptor desc = getGetOpDesc(classContext, typeNameAndDesc.getValue());
                ValueType outputType = typeLookup.get(typeNameAndDesc.getValue());
                intrinsics.registerIntrinsic(unsafeDesc, name, desc, (builder, instance, targetPtr, arguments) -> {
                    Value obj = arguments.get(0);
                    Value offset = arguments.get(1);
                    Value handle = builder.byteOffsetPointer(builder.decodeReference(obj), offset, outputType);
                    return builder.load(handle, suffixAndMode.getValue());
                });
            }
        }
    }

    // Put

    private static MethodDescriptor getPutOpDesc(ClassContext classContext, TypeDescriptor type) {
        return MethodDescriptor.synthesize(
            classContext,
            BaseTypeDescriptor.V,
            List.of(
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Object"),
                BaseTypeDescriptor.J,
                type
            )
        );
    }

    private static void registerPutIntrinsics(final CompilationContext ctxt) {
        // Rely on JDK implementations for smaller-than-int sizes; LLVM does not like misaligned atomics
        // todo: research to discover if some platforms handle misalignment correctly, make it switchable

        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        TypeSystem ts = ctxt.getTypeSystem();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ts.getVoidType());

        Map<TypeDescriptor, ValueType> typeLookup = Map.of(
            BaseTypeDescriptor.B, ts.getSignedInteger8Type(),
            BaseTypeDescriptor.S, ts.getSignedInteger16Type(),
            BaseTypeDescriptor.I, ts.getSignedInteger32Type(),
            BaseTypeDescriptor.J, ts.getSignedInteger64Type(),
            BaseTypeDescriptor.Z, ts.getUnsignedInteger8Type(),
            BaseTypeDescriptor.C, ts.getUnsignedInteger16Type(),
            BaseTypeDescriptor.F, ts.getFloat32Type(),
            BaseTypeDescriptor.D, ts.getFloat64Type(),
            objDesc, CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getObjectType().getReference()
        );

        for (Map.Entry<String, TypeDescriptor> typeNameAndDesc : Map.of(
            "Byte", BaseTypeDescriptor.B,
            "Short", BaseTypeDescriptor.S,
            "Int", BaseTypeDescriptor.I,
            "Long", BaseTypeDescriptor.J,
            "Boolean", BaseTypeDescriptor.Z,
            "Char", BaseTypeDescriptor.C,
            "Float", BaseTypeDescriptor.F,
            "Double", BaseTypeDescriptor.D,
            "Object", objDesc,
            "Reference", objDesc
        ).entrySet()) {
            for (Map.Entry<String, WriteAccessMode> suffixAndMode : Map.of(
                "", SinglePlain,
                "Release", GlobalRelease,
                "Opaque", SingleOpaque,
                "Volatile", GlobalSeqCst
            ).entrySet()) {
                String name = "put" + typeNameAndDesc.getKey() + suffixAndMode.getKey();
                MethodDescriptor desc = getPutOpDesc(classContext, typeNameAndDesc.getValue());
                ValueType valueType = typeLookup.get(typeNameAndDesc.getValue());
                intrinsics.registerIntrinsic(unsafeDesc, name, desc, (builder, instance, targetPtr, arguments) -> {
                    Value obj = arguments.get(0);
                    Value offset = arguments.get(1);
                    Value value = arguments.get(2);
                    if (typeNameAndDesc.getValue().equals(BaseTypeDescriptor.Z)) {
                        value = builder.select(value, ctxt.getLiteralFactory().literalOf((IntegerType) valueType, 1),
                            ctxt.getLiteralFactory().literalOf((IntegerType) valueType, 0));
                    }
                    Value handle = builder.byteOffsetPointer(builder.decodeReference(obj), offset, valueType);
                    builder.store(handle, value, suffixAndMode.getValue());
                    return voidLiteral;
                });
            }
        }
    }

    // Fences

    private static void registerFenceIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");

        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        record FenceIntrinsic(GlobalAccessMode mode) implements InstanceIntrinsic {
            public Value emitIntrinsic(BasicBlockBuilder builder, Value instance, InstanceMethodLiteral targetPtr, List<Value> arguments) {
                builder.fence(mode);
                ClassContext context = builder.element().getEnclosingType().getContext();
                return context.getLiteralFactory().zeroInitializerLiteralOfType(context.getTypeSystem().getVoidType());
            }
        }

        intrinsics.registerIntrinsic(unsafeDesc, "storeFence", emptyToVoid, new FenceIntrinsic(GlobalRelease));
        intrinsics.registerIntrinsic(unsafeDesc, "storeStoreFence", emptyToVoid, new FenceIntrinsic(GlobalStoreStore));
        intrinsics.registerIntrinsic(unsafeDesc, "loadFence", emptyToVoid, new FenceIntrinsic(GlobalAcquire));
        intrinsics.registerIntrinsic(unsafeDesc, "loadLoadFence", emptyToVoid, new FenceIntrinsic(GlobalLoadLoad));
        intrinsics.registerIntrinsic(unsafeDesc, "fullFence", emptyToVoid, new FenceIntrinsic(GlobalSeqCst));
    }

    // Array and field base/offsets

    private static void registerFieldAndArrayIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");

        MethodDescriptor classToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(classDesc));
        MethodDescriptor classStringToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(classDesc, stringDesc));

        //arrayBaseOffset0
        InstanceIntrinsic arrayBaseOffset = (builder, instance, target, arguments) -> {
            CoreClasses coreClasses = CoreClasses.get(ctxt);
            LiteralFactory lf = ctxt.getLiteralFactory();
            // reference array
            FieldElement refArrayContentField = coreClasses.getRefArrayContentField();
            FieldElement booleanArrayContentField = coreClasses.getBooleanArrayContentField();
            FieldElement byteArrayContentField = coreClasses.getByteArrayContentField();
            FieldElement shortArrayContentField = coreClasses.getShortArrayContentField();
            FieldElement intArrayContentField = coreClasses.getIntArrayContentField();
            FieldElement longArrayContentField = coreClasses.getLongArrayContentField();
            FieldElement charArrayContentField = coreClasses.getCharArrayContentField();
            FieldElement floatArrayContentField = coreClasses.getFloatArrayContentField();
            FieldElement doubleArrayContentField = coreClasses.getDoubleArrayContentField();
            // todo: enhance switch to accept any Literal to simplify this considerably
            BlockLabel isRef = new BlockLabel();
            BlockLabel isNotRef = new BlockLabel();
            BlockLabel isBool = new BlockLabel();
            BlockLabel isNotBool = new BlockLabel();
            BlockLabel isByte = new BlockLabel();
            BlockLabel isNotByte = new BlockLabel();
            BlockLabel isShort = new BlockLabel();
            BlockLabel isNotShort = new BlockLabel();
            BlockLabel isInt = new BlockLabel();
            BlockLabel isNotInt = new BlockLabel();
            BlockLabel isLong = new BlockLabel();
            BlockLabel isNotLong = new BlockLabel();
            BlockLabel isChar = new BlockLabel();
            BlockLabel isNotChar = new BlockLabel();
            BlockLabel isFloat = new BlockLabel();
            BlockLabel isNotFloat = new BlockLabel();
            BlockLabel isDouble = new BlockLabel();
            BlockLabel isNotDouble = new BlockLabel();
            Value classDims = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassDimensionField()));
            Value isRefBool = builder.isNe(classDims, lf.zeroInitializerLiteralOfType(classDims.getType()));
            builder.if_(isRefBool, isRef, isNotRef, Map.of());
            builder.begin(isRef);
            builder.return_(builder.offsetOfField(refArrayContentField));
            builder.begin(isNotRef);
            Value typeId = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassTypeIdField()));
            Value isBoolBool = builder.isEq(typeId, lf.literalOfType(booleanArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isBoolBool, isBool, isNotBool, Map.of());
            builder.begin(isBool);
            builder.return_(builder.offsetOfField(booleanArrayContentField));
            builder.begin(isNotBool);
            Value isByteBool = builder.isEq(typeId, lf.literalOfType(byteArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isByteBool, isByte, isNotByte, Map.of());
            builder.begin(isByte);
            builder.return_(builder.offsetOfField(byteArrayContentField));
            builder.begin(isNotByte);
            Value isShortBool = builder.isEq(typeId, lf.literalOfType(shortArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isShortBool, isShort, isNotShort, Map.of());
            builder.begin(isShort);
            builder.return_(builder.offsetOfField(shortArrayContentField));
            builder.begin(isNotShort);
            Value isIntBool = builder.isEq(typeId, lf.literalOfType(intArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isIntBool, isInt, isNotInt, Map.of());
            builder.begin(isInt);
            builder.return_(builder.offsetOfField(intArrayContentField));
            builder.begin(isNotInt);
            Value isLongBool = builder.isEq(typeId, lf.literalOfType(longArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isLongBool, isLong, isNotLong, Map.of());
            builder.begin(isLong);
            builder.return_(builder.offsetOfField(longArrayContentField));
            builder.begin(isNotLong);
            Value isCharBool = builder.isEq(typeId, lf.literalOfType(charArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isCharBool, isChar, isNotChar, Map.of());
            builder.begin(isChar);
            builder.return_(builder.offsetOfField(charArrayContentField));
            builder.begin(isNotChar);
            Value isFloatBool = builder.isEq(typeId, lf.literalOfType(floatArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isFloatBool, isFloat, isNotFloat, Map.of());
            builder.begin(isFloat);
            builder.return_(builder.offsetOfField(floatArrayContentField));
            builder.begin(isNotFloat);
            Value isDoubleBool = builder.isEq(typeId, lf.literalOfType(doubleArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isDoubleBool, isDouble, isNotDouble, Map.of());
            builder.begin(isDouble);
            builder.return_(builder.offsetOfField(doubleArrayContentField));
            builder.begin(isNotDouble);
            MethodElement throwCce = RuntimeMethodFinder.get(ctxt).getMethod("raiseClassCastException");
            throw new BlockEarlyTermination(builder.callNoReturn(lf.literalOf(throwCce), List.of()));
        };

        intrinsics.registerIntrinsic(unsafeDesc, "arrayBaseOffset0", classToInt, arrayBaseOffset);

        //arrayIndexScale0
        InstanceIntrinsic arrayIndexScale = (builder, instance, target, arguments) -> {
            CoreClasses coreClasses = CoreClasses.get(ctxt);
            LiteralFactory lf = ctxt.getLiteralFactory();
            // reference array
            FieldElement booleanArrayContentField = coreClasses.getBooleanArrayContentField();
            FieldElement byteArrayContentField = coreClasses.getByteArrayContentField();
            FieldElement shortArrayContentField = coreClasses.getShortArrayContentField();
            FieldElement intArrayContentField = coreClasses.getIntArrayContentField();
            FieldElement longArrayContentField = coreClasses.getLongArrayContentField();
            FieldElement charArrayContentField = coreClasses.getCharArrayContentField();
            FieldElement floatArrayContentField = coreClasses.getFloatArrayContentField();
            FieldElement doubleArrayContentField = coreClasses.getDoubleArrayContentField();
            // todo: enhance switch to accept any Literal to simplify this considerably
            BlockLabel isRef = new BlockLabel();
            BlockLabel isNotRef = new BlockLabel();
            BlockLabel isBool = new BlockLabel();
            BlockLabel isNotBool = new BlockLabel();
            BlockLabel isByte = new BlockLabel();
            BlockLabel isNotByte = new BlockLabel();
            BlockLabel isShort = new BlockLabel();
            BlockLabel isNotShort = new BlockLabel();
            BlockLabel isInt = new BlockLabel();
            BlockLabel isNotInt = new BlockLabel();
            BlockLabel isLong = new BlockLabel();
            BlockLabel isNotLong = new BlockLabel();
            BlockLabel isChar = new BlockLabel();
            BlockLabel isNotChar = new BlockLabel();
            BlockLabel isFloat = new BlockLabel();
            BlockLabel isNotFloat = new BlockLabel();
            BlockLabel isDouble = new BlockLabel();
            BlockLabel isNotDouble = new BlockLabel();
            Value classDims = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassDimensionField()));
            Value isRefBool = builder.isNe(classDims, lf.zeroInitializerLiteralOfType(classDims.getType()));
            builder.if_(isRefBool, isRef, isNotRef, Map.of());
            builder.begin(isRef);
            builder.return_(lf.literalOf(ctxt.getTypeSystem().getReferenceSize()));
            builder.begin(isNotRef);
            Value typeId = builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), coreClasses.getClassTypeIdField()));
            Value isBoolBool = builder.isEq(typeId, lf.literalOfType(booleanArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isBoolBool, isBool, isNotBool, Map.of());
            builder.begin(isBool);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getBooleanType().getSize()));
            builder.begin(isNotBool);
            Value isByteBool = builder.isEq(typeId, lf.literalOfType(byteArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isByteBool, isByte, isNotByte, Map.of());
            builder.begin(isByte);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger8Type().getSize()));
            builder.begin(isNotByte);
            Value isShortBool = builder.isEq(typeId, lf.literalOfType(shortArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isShortBool, isShort, isNotShort, Map.of());
            builder.begin(isShort);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger16Type().getSize()));
            builder.begin(isNotShort);
            Value isIntBool = builder.isEq(typeId, lf.literalOfType(intArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isIntBool, isInt, isNotInt, Map.of());
            builder.begin(isInt);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger32Type().getSize()));
            builder.begin(isNotInt);
            Value isLongBool = builder.isEq(typeId, lf.literalOfType(longArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isLongBool, isLong, isNotLong, Map.of());
            builder.begin(isLong);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger64Type().getSize()));
            builder.begin(isNotLong);
            Value isCharBool = builder.isEq(typeId, lf.literalOfType(charArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isCharBool, isChar, isNotChar, Map.of());
            builder.begin(isChar);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getUnsignedInteger16Type().getSize()));
            builder.begin(isNotChar);
            Value isFloatBool = builder.isEq(typeId, lf.literalOfType(floatArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isFloatBool, isFloat, isNotFloat, Map.of());
            builder.begin(isFloat);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getFloat32Type().getSize()));
            builder.begin(isNotFloat);
            Value isDoubleBool = builder.isEq(typeId, lf.literalOfType(doubleArrayContentField.getEnclosingType().load().getObjectType()));
            builder.if_(isDoubleBool, isDouble, isNotDouble, Map.of());
            builder.begin(isDouble);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getFloat64Type().getSize()));
            builder.begin(isNotDouble);
            MethodElement throwCce = RuntimeMethodFinder.get(ctxt).getMethod("raiseClassCastException");
            throw new BlockEarlyTermination(builder.callNoReturn(lf.literalOf(throwCce), List.of()));
        };

        intrinsics.registerIntrinsic(unsafeDesc, "arrayIndexScale0", classToInt, arrayIndexScale);
    }

    // remaining TODO:
    //getUncompressedObject
    //shouldBeInitialized0
    //ensureClassInitialized0
}
