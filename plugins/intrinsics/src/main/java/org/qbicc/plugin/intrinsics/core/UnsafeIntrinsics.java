package org.qbicc.plugin.intrinsics.core;

import static org.qbicc.graph.CmpAndSwap.Strength.STRONG;
import static org.qbicc.graph.CmpAndSwap.Strength.WEAK;
import static org.qbicc.graph.MemoryAtomicityMode.*;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.Store;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.Variable;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.LoadedTypeDefinition;
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
        registerCompareAndExchangeIntrinsics(ctxt);
        registerCompareAndSetIntrinsics(ctxt);
        registerGetAndModIntrinsics(ctxt);
        registerGetIntrinsics(ctxt);
        registerPutIntrinsics(ctxt);
        registerFenceIntrinsics(ctxt);
        registerPlatformStaticIntrinsics(ctxt);
        registerFieldAndArrayIntrinsics(ctxt);
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

    private static Value doCompareAndExchange(CompilationContext ctxt, BasicBlockBuilder builder, List<Value> arguments, MemoryAtomicityMode atomicityMode, CmpAndSwap.Strength strength) {
        Value obj = arguments.get(0);
        Value offset = arguments.get(1);
        Value expect = arguments.get(2);
        Value update = arguments.get(3);

        ValueType expectType = expect.getType();
        if (expectType instanceof ReferenceType) {
            ObjectType objectType = CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getType();
            expectType = objectType.getReference();
        }
        ValueHandle handle = builder.unsafeHandle(builder.referenceHandle(obj), offset, expectType);
        Value result = builder.cmpAndSwap(handle, expect, update, atomicityMode, MONOTONIC, strength);
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
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, VOLATILE, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeIntAcquire", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, ACQUIRE, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeIntRelease", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, RELEASE, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeLong", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, VOLATILE, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeLongAcquire", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, ACQUIRE, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeLongRelease", getCompareAndExchangeDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, RELEASE, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeObject", getCompareAndExchangeDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, VOLATILE, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeObjectAcquire", getCompareAndExchangeDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, ACQUIRE, STRONG));
        intrinsics.registerIntrinsic(unsafeDesc, "compareAndExchangeObjectRelease", getCompareAndExchangeDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndExchange(ctxt, builder, arguments, RELEASE, STRONG));


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

    private static Value doCompareAndSet(CompilationContext ctxt, BasicBlockBuilder builder, List<Value> arguments, MemoryAtomicityMode atomicityMode, CmpAndSwap.Strength strength) {
        Value obj = arguments.get(0);
        Value offset = arguments.get(1);
        Value expect = arguments.get(2);
        Value update = arguments.get(3);

        ValueType expectType = expect.getType();
        if (expectType instanceof ReferenceType) {
            ObjectType objectType = CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getType();
            expectType = objectType.getReference();
        }
        ValueHandle handle = builder.unsafeHandle(builder.referenceHandle(obj), offset, expectType);
        Value result = builder.cmpAndSwap(handle, expect, update, atomicityMode, MONOTONIC, strength);
        // result is a compound structure; extract the success flag
        return builder.extractMember(result, CmpAndSwap.getResultType(ctxt, expectType).getMember(1));
    }

    private static void registerCompareAndSetIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndSetInt", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, VOLATILE, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndSetLong", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, VOLATILE, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "compareAndSetObject", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, VOLATILE, STRONG));

        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetInt", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, VOLATILE, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetIntAcquire", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, ACQUIRE, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetIntPlain", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, MONOTONIC, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetIntRelease", getCompareAndSetDesc(classContext, BaseTypeDescriptor.I),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, RELEASE, WEAK));

        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLong", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, VOLATILE, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLongAcquire", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, ACQUIRE, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLongPlain", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, MONOTONIC, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetLongRelease", getCompareAndSetDesc(classContext, BaseTypeDescriptor.J),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, RELEASE, WEAK));

        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObject", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, VOLATILE, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObjectAcquire", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, ACQUIRE, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObjectPlain", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, MONOTONIC, WEAK));
        intrinsics.registerIntrinsic(unsafeDesc, "weakCompareAndSetObjectRelease", getCompareAndSetDesc(classContext, objDesc),
            (builder, instance, target, arguments) -> doCompareAndSet(ctxt, builder, arguments, RELEASE, WEAK));
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

    private interface BuilderGetAndModOp {
        Value apply(BasicBlockBuilder builder, ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);
    }

    private static Value doGetAndModify(CompilationContext ctxt, BasicBlockBuilder builder, BuilderGetAndModOp op, List<Value> arguments, MemoryAtomicityMode atomicityMode) {
        Value obj = arguments.get(0);
        Value offset = arguments.get(1);
        Value operand = arguments.get(2);

        ValueType operandType = operand.getType();
        if (operandType instanceof ReferenceType) {
            ObjectType objectType = CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getType();
            operandType = objectType.getReference();
        }
        ValueHandle handle = builder.unsafeHandle(builder.referenceHandle(obj), offset, operandType);
        return op.apply(builder, handle, operand, atomicityMode);
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
            for (Map.Entry<String, BuilderGetAndModOp> nameAndOp : Map.of(
                "Add", BasicBlockBuilder::getAndAdd,
                "BitwiseAnd", BasicBlockBuilder::getAndBitwiseAnd,
                "BitwiseOr", BasicBlockBuilder::getAndBitwiseOr,
                "BitwiseXor", BasicBlockBuilder::getAndBitwiseXor,
                "Set", (BuilderGetAndModOp) BasicBlockBuilder::getAndSet
            ).entrySet()) {
                for (Map.Entry<String, MemoryAtomicityMode> suffixAndMode : Map.of(
                    "", VOLATILE,
                    "Acquire", ACQUIRE,
                    "Release", RELEASE
                ).entrySet()) {
                    String name = "getAnd" + nameAndOp.getKey() + typeNameAndDesc.getKey() + suffixAndMode.getKey();
                    MethodDescriptor desc = getGetAndBinOpDesc(classContext, typeNameAndDesc.getValue());
                    intrinsics.registerIntrinsic(unsafeDesc, name, desc, (builder, instance, target, arguments) ->
                        doGetAndModify(ctxt, builder, nameAndOp.getValue(), arguments, suffixAndMode.getValue()));
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
            BaseTypeDescriptor.I, ts.getSignedInteger32Type(),
            BaseTypeDescriptor.J, ts.getSignedInteger64Type(),
            objDesc, CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getType().getReference()
        );

        for (Map.Entry<String, TypeDescriptor> typeNameAndDesc : Map.of(
            "Int", BaseTypeDescriptor.I,
            "Long", BaseTypeDescriptor.J,
            "Object", objDesc
        ).entrySet()) {
            for (Map.Entry<String, MemoryAtomicityMode> suffixAndMode : Map.of(
                "", UNORDERED,
                "Acquire", ACQUIRE,
                "Opaque", MONOTONIC/*?????*/,
                "Volatile", VOLATILE
            ).entrySet()) {
                String name = "get" + typeNameAndDesc.getKey() + suffixAndMode.getKey();
                MethodDescriptor desc = getGetOpDesc(classContext, typeNameAndDesc.getValue());
                ValueType outputType = typeLookup.get(typeNameAndDesc.getValue());
                intrinsics.registerIntrinsic(unsafeDesc, name, desc, (builder, instance, target, arguments) -> {
                    Value obj = arguments.get(0);
                    Value offset = arguments.get(1);
                    ValueHandle handle = builder.unsafeHandle(builder.referenceHandle(obj), offset, outputType);
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
            BaseTypeDescriptor.I, ts.getSignedInteger32Type(),
            BaseTypeDescriptor.J, ts.getSignedInteger64Type(),
            objDesc, CoreClasses.get(ctxt).getObjectTypeIdField().getEnclosingType().load().getType().getReference()
        );

        for (Map.Entry<String, TypeDescriptor> typeNameAndDesc : Map.of(
            "Int", BaseTypeDescriptor.I,
            "Long", BaseTypeDescriptor.J,
            "Object", objDesc
        ).entrySet()) {
            for (Map.Entry<String, MemoryAtomicityMode> suffixAndMode : Map.of(
                "", UNORDERED,
                "Release", RELEASE,
                "Opaque", MONOTONIC/*?????*/,
                "Volatile", VOLATILE
            ).entrySet()) {
                String name = "put" + typeNameAndDesc.getKey() + suffixAndMode.getKey();
                MethodDescriptor desc = getPutOpDesc(classContext, typeNameAndDesc.getValue());
                ValueType valueType = typeLookup.get(typeNameAndDesc.getValue());
                intrinsics.registerIntrinsic(unsafeDesc, name, desc, (builder, instance, target, arguments) -> {
                    Value obj = arguments.get(0);
                    Value offset = arguments.get(1);
                    Value value = arguments.get(2);
                    ValueHandle handle = builder.unsafeHandle(builder.referenceHandle(obj), offset, valueType);
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
        TypeSystem ts = ctxt.getTypeSystem();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");

        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        Literal voidLiteral = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ts.getVoidType());

        InstanceIntrinsic storeFence = (builder, instance, target, arguments) -> {
            builder.fence(RELEASE);
            return voidLiteral;
        };

        intrinsics.registerIntrinsic(unsafeDesc, "storeFence", emptyToVoid, storeFence);
        intrinsics.registerIntrinsic(unsafeDesc, "storeStoreFence", emptyToVoid, storeFence);

        InstanceIntrinsic loadFence = (builder, instance, target, arguments) -> {
            builder.fence(ACQUIRE);
            return voidLiteral;
        };

        intrinsics.registerIntrinsic(unsafeDesc, "loadFence", emptyToVoid, loadFence);
        intrinsics.registerIntrinsic(unsafeDesc, "loadLoadFence", emptyToVoid, loadFence);

        InstanceIntrinsic fullFence = (builder, instance, target, arguments) -> {
            builder.fence(SEQUENTIALLY_CONSISTENT);
            return voidLiteral;
        };

        intrinsics.registerIntrinsic(unsafeDesc, "fullFence", emptyToVoid, fullFence);
    }

    // Platform static (build time available) information

    private static void registerPlatformStaticIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor unsafeDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/misc/Unsafe");

        MethodDescriptor emptyToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());
        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        InstanceIntrinsic addressSize0 = (builder, instance, target, arguments) -> {
            ClassContext c = builder.getCurrentElement().getEnclosingType().getContext();
            LiteralFactory lf = c.getLiteralFactory();
            return lf.literalOf(c.getTypeSystem().getPointerSize());
        };

        intrinsics.registerIntrinsic(unsafeDesc, "addressSize0", emptyToInt, addressSize0);

        InstanceIntrinsic isBigEndian0 = (builder, instance, target, arguments) -> {
            ClassContext c = builder.getCurrentElement().getEnclosingType().getContext();
            LiteralFactory lf = c.getLiteralFactory();
            return lf.literalOf(c.getTypeSystem().getEndianness() == ByteOrder.BIG_ENDIAN);
        };

        intrinsics.registerIntrinsic(unsafeDesc, "isBigEndian0", emptyToBool, isBigEndian0);

        InstanceIntrinsic unalignedAccess0 = (builder, instance, target, arguments) -> {
            ClassContext c = builder.getCurrentElement().getEnclosingType().getContext();
            LiteralFactory lf = c.getLiteralFactory();
            // TODO: determine whether unaligned accesses are UB for any given platform in LLVM
            return lf.literalOf(false);
        };

        intrinsics.registerIntrinsic(unsafeDesc, "unalignedAccess0", emptyToBool, unalignedAccess0);
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
            Value classDims = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassDimensionField()), UNORDERED);
            Value isRefBool = builder.isNe(classDims, lf.zeroInitializerLiteralOfType(classDims.getType()));
            builder.if_(isRefBool, isRef, isNotRef);
            builder.begin(isRef);
            builder.return_(builder.offsetOfField(refArrayContentField));
            builder.begin(isNotRef);
            Value typeId = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassTypeIdField()), UNORDERED);
            Value isBoolBool = builder.isEq(typeId, lf.literalOfType(booleanArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isBoolBool, isBool, isNotBool);
            builder.begin(isBool);
            builder.return_(builder.offsetOfField(booleanArrayContentField));
            builder.begin(isNotBool);
            Value isByteBool = builder.isEq(typeId, lf.literalOfType(byteArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isByteBool, isByte, isNotByte);
            builder.begin(isByte);
            builder.return_(builder.offsetOfField(byteArrayContentField));
            builder.begin(isNotByte);
            Value isShortBool = builder.isEq(typeId, lf.literalOfType(shortArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isShortBool, isShort, isNotShort);
            builder.begin(isShort);
            builder.return_(builder.offsetOfField(shortArrayContentField));
            builder.begin(isNotShort);
            Value isIntBool = builder.isEq(typeId, lf.literalOfType(intArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isIntBool, isInt, isNotInt);
            builder.begin(isInt);
            builder.return_(builder.offsetOfField(intArrayContentField));
            builder.begin(isNotInt);
            Value isLongBool = builder.isEq(typeId, lf.literalOfType(longArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isLongBool, isLong, isNotLong);
            builder.begin(isLong);
            builder.return_(builder.offsetOfField(longArrayContentField));
            builder.begin(isNotLong);
            Value isCharBool = builder.isEq(typeId, lf.literalOfType(charArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isCharBool, isChar, isNotChar);
            builder.begin(isChar);
            builder.return_(builder.offsetOfField(charArrayContentField));
            builder.begin(isNotChar);
            Value isFloatBool = builder.isEq(typeId, lf.literalOfType(floatArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isFloatBool, isFloat, isNotFloat);
            builder.begin(isFloat);
            builder.return_(builder.offsetOfField(floatArrayContentField));
            builder.begin(isNotFloat);
            Value isDoubleBool = builder.isEq(typeId, lf.literalOfType(doubleArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isDoubleBool, isDouble, isNotDouble);
            builder.begin(isDouble);
            builder.return_(builder.offsetOfField(doubleArrayContentField));
            builder.begin(isNotDouble);
            MethodElement throwCce = RuntimeMethodFinder.get(ctxt).getMethod("raiseClassCastException");
            throw new BlockEarlyTermination(builder.callNoReturn(builder.staticMethod(throwCce, throwCce.getDescriptor(), throwCce.getType()), List.of()));
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
            Value classDims = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassDimensionField()), UNORDERED);
            Value isRefBool = builder.isNe(classDims, lf.zeroInitializerLiteralOfType(classDims.getType()));
            builder.if_(isRefBool, isRef, isNotRef);
            builder.begin(isRef);
            builder.return_(lf.literalOf(ctxt.getTypeSystem().getReferenceSize()));
            builder.begin(isNotRef);
            Value typeId = builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), coreClasses.getClassTypeIdField()), UNORDERED);
            Value isBoolBool = builder.isEq(typeId, lf.literalOfType(booleanArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isBoolBool, isBool, isNotBool);
            builder.begin(isBool);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getBooleanType().getSize()));
            builder.begin(isNotBool);
            Value isByteBool = builder.isEq(typeId, lf.literalOfType(byteArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isByteBool, isByte, isNotByte);
            builder.begin(isByte);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger8Type().getSize()));
            builder.begin(isNotByte);
            Value isShortBool = builder.isEq(typeId, lf.literalOfType(shortArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isShortBool, isShort, isNotShort);
            builder.begin(isShort);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger16Type().getSize()));
            builder.begin(isNotShort);
            Value isIntBool = builder.isEq(typeId, lf.literalOfType(intArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isIntBool, isInt, isNotInt);
            builder.begin(isInt);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger32Type().getSize()));
            builder.begin(isNotInt);
            Value isLongBool = builder.isEq(typeId, lf.literalOfType(longArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isLongBool, isLong, isNotLong);
            builder.begin(isLong);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getSignedInteger64Type().getSize()));
            builder.begin(isNotLong);
            Value isCharBool = builder.isEq(typeId, lf.literalOfType(charArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isCharBool, isChar, isNotChar);
            builder.begin(isChar);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getUnsignedInteger16Type().getSize()));
            builder.begin(isNotChar);
            Value isFloatBool = builder.isEq(typeId, lf.literalOfType(floatArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isFloatBool, isFloat, isNotFloat);
            builder.begin(isFloat);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getFloat32Type().getSize()));
            builder.begin(isNotFloat);
            Value isDoubleBool = builder.isEq(typeId, lf.literalOfType(doubleArrayContentField.getEnclosingType().load().getType()));
            builder.if_(isDoubleBool, isDouble, isNotDouble);
            builder.begin(isDouble);
            builder.return_(lf.literalOf((int)ctxt.getTypeSystem().getFloat64Type().getSize()));
            builder.begin(isNotDouble);
            MethodElement throwCce = RuntimeMethodFinder.get(ctxt).getMethod("raiseClassCastException");
            throw new BlockEarlyTermination(builder.callNoReturn(builder.staticMethod(throwCce, throwCce.getDescriptor(), throwCce.getType()), List.of()));
        };

        intrinsics.registerIntrinsic(unsafeDesc, "arrayIndexScale0", classToInt, arrayIndexScale);
    }

    private static Value traverseLoads(Value value) {
        // todo: modify Load to carry a "known value"?
        if (value instanceof Load) {
            ValueHandle valueHandle = value.getValueHandle();
            if (valueHandle instanceof LocalVariable || valueHandle instanceof Variable && ((Variable) valueHandle).getVariableElement().isFinal()) {
                Node dependency = value;
                while (dependency instanceof OrderedNode) {
                    dependency = ((OrderedNode) dependency).getDependency();
                    if (dependency instanceof Store) {
                        if (dependency.getValueHandle().equals(valueHandle)) {
                            return ((Store) dependency).getValue();
                        }
                    }
                    if (dependency instanceof BlockEntry) {
                        // not resolvable
                        break;
                    }
                }
            }
        }
        return value;
    }

    // remaining TODO:
    //getUncompressedObject
    //shouldBeInitialized0
    //ensureClassInitialized0
}
