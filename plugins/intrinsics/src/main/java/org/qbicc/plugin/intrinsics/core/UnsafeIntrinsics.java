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
import org.qbicc.graph.BlockEntry;
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
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
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
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");

        MethodDescriptor classToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(classDesc));
        MethodDescriptor classStringToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(classDesc, stringDesc));

        //arrayBaseOffset0
        InstanceIntrinsic arrayBaseOffset = (builder, instance, target, arguments) -> {
            Value clazz = traverseLoads(arguments.get(0));
            CoreClasses coreClasses = CoreClasses.get(ctxt);
            LiteralFactory lf = ctxt.getLiteralFactory();
            ArrayObjectType objectType;
            if (clazz instanceof ClassOf) {
                ClassOf classOf = (ClassOf) clazz;
                Value typeId = classOf.getInput();
                Value dimensions = classOf.getDimensions();
                if (typeId instanceof TypeLiteral) {
                    ObjectType valueType = (ObjectType) ((TypeLiteral) typeId).getValue();
                    if (dimensions instanceof IntegerLiteral) {
                        int dimensionsValue = ((IntegerLiteral) dimensions).intValue();
                        if (dimensionsValue > 0) {
                            // it's a reference array
                            return builder.offsetOfField(coreClasses.getRefArrayContentField());
                        }
                    }
                    if (valueType instanceof ArrayObjectType) {
                        objectType = (ArrayObjectType) valueType;
                        ValueType elementType = objectType.getElementType();
                        if (elementType instanceof WordType) {
                            switch (((WordType) elementType).asPrimitive()) {
                                case BOOLEAN: return builder.offsetOfField(coreClasses.getBooleanArrayContentField());
                                case BYTE: return builder.offsetOfField(coreClasses.getByteArrayContentField());
                                case SHORT: return builder.offsetOfField(coreClasses.getShortArrayContentField());
                                case CHAR: return builder.offsetOfField(coreClasses.getCharArrayContentField());
                                case INT: return builder.offsetOfField(coreClasses.getIntArrayContentField());
                                case FLOAT: return builder.offsetOfField(coreClasses.getFloatArrayContentField());
                                case LONG: return builder.offsetOfField(coreClasses.getLongArrayContentField());
                                case DOUBLE: return builder.offsetOfField(coreClasses.getDoubleArrayContentField());
                                default: {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            ctxt.error(builder.getLocation(), "arrayBaseOffset type argument must be a literal of an array object type");
            return lf.literalOf(0);
        };

        intrinsics.registerIntrinsic(unsafeDesc, "arrayBaseOffset", classToInt, arrayBaseOffset);

        //arrayIndexScale0
        InstanceIntrinsic arrayIndexScale = (builder, instance, target, arguments) -> {
            Value clazz = traverseLoads(arguments.get(0));
            LiteralFactory lf = ctxt.getLiteralFactory();
            if (clazz instanceof ClassOf) {
                ClassOf classOf = (ClassOf) clazz;
                Value typeId = classOf.getInput();
                Value dimensions = classOf.getDimensions();
                if (typeId instanceof TypeLiteral) {
                    ObjectType valueType = (ObjectType) ((TypeLiteral) typeId).getValue();
                    if (dimensions instanceof IntegerLiteral) {
                        int dimensionsValue = ((IntegerLiteral) dimensions).intValue();
                        if (dimensionsValue > 0) {
                            // it's a reference array
                            return lf.literalOf(ctxt.getTypeSystem().getReferenceSize());
                        }
                    }
                    if (valueType instanceof ArrayObjectType) {
                        ArrayObjectType objectType = (ArrayObjectType) valueType;
                        ValueType elementType = objectType.getElementType();
                        if (elementType instanceof WordType) {
                            return lf.literalOf((int) elementType.getSize());
                        }
                    }
                }
            }
            ctxt.error(builder.getLocation(), "arrayIndexScale type argument must be a literal of an array object type");
            return lf.literalOf(0);
        };

        intrinsics.registerIntrinsic(unsafeDesc, "arrayIndexScale", classToInt, arrayIndexScale);

        //objectFieldOffset1
        InstanceIntrinsic objectFieldOffset = (builder, instance, target, arguments) -> {
            Value clazz = traverseLoads(arguments.get(0));
            Value string = traverseLoads(arguments.get(1));
            LiteralFactory lf = ctxt.getLiteralFactory();
            ObjectType objectType;
            if (clazz instanceof ClassOf) {
                ClassOf classOf = (ClassOf) clazz;
                Value typeId = classOf.getInput();
                if (typeId instanceof TypeLiteral) {
                    ValueType valueType = ((TypeLiteral) typeId).getValue();
                    if (valueType instanceof ObjectType) {
                        objectType = (ObjectType) valueType;
                    } else {
                        ctxt.error(builder.getLocation(), "objectFieldOffset type argument must be a literal of an object type");
                        return lf.literalOf(0L);
                    }
                } else {
                    ctxt.error(builder.getLocation(), "objectFieldOffset type argument must be a literal of an object type");
                    return lf.literalOf(0L);
                }
            } else {
                ctxt.error(builder.getLocation(), "objectFieldOffset type argument must be a literal of an object type");
                return lf.literalOf(0L);
            }
            String fieldName;
            if (string instanceof StringLiteral) {
                fieldName = ((StringLiteral) string).getValue();
            } else if (string instanceof ObjectLiteral) {
                VmObject vmObject = ((ObjectLiteral) string).getValue();
                if (! (vmObject instanceof VmString)) {
                    ctxt.error(builder.getLocation(), "objectFieldOffset string argument must be a literal string");
                    return lf.literalOf(0L);
                }
                fieldName = ((VmString) vmObject).getContent();
            } else {
                ctxt.error(builder.getLocation(), "objectFieldOffset string argument must be a literal string");
                return lf.literalOf(0L);
            }
            LoadedTypeDefinition ltd = objectType.getDefinition().load();
            FieldElement field = ltd.findField(fieldName);
            if (field == null) {
                ctxt.error(builder.getLocation(), "No such field \"%s\" on class \"%s\"", fieldName, ltd.getVmClass().getName());
                return lf.literalOf(0L);
            }
            // cast to long to fit method contract
            return builder.extend(builder.offsetOfField(field), (WordType) target.getExecutable().getType().getReturnType());
        };

        intrinsics.registerIntrinsic(unsafeDesc, "objectFieldOffset", classStringToLong, objectFieldOffset);

        // TODO: inspect reflection objects
        //objectFieldOffset0
        //staticFieldOffset0
        //staticFieldBase0
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
