package org.qbicc.plugin.intrinsics.core;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.Dereference;
import org.qbicc.graph.Extend;
import org.qbicc.graph.Load;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemberOfUnion;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.WordCastValue;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.Data;
import org.qbicc.object.ModuleSection;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

final class CNativeIntrinsics {

    public static void register(CompilationContext ctxt) {
        registerTopLevelIntrinsics(ctxt);
        registerNObjectIntrinsics(ctxt);
        registerWordIntrinsics(ctxt);
        registerPtrIntrinsics(ctxt);
    }

    private static void registerTopLevelIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor cNativeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor referenceDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$reference");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(classContext, objDesc);
        ClassTypeDescriptor nObjDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$object");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");
        ClassTypeDescriptor functionDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$function");
        ClassTypeDescriptor wordDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$word");
        ClassTypeDescriptor strDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        ClassTypeDescriptor sizeTDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stddef$size_t");

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objArrayTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objArrayDesc));

        MethodDescriptor objToReference = MethodDescriptor.synthesize(classContext, referenceDesc, List.of(objDesc));
        MethodDescriptor wordToReference = MethodDescriptor.synthesize(classContext, referenceDesc, List.of(wordDesc));
        MethodDescriptor toObject = MethodDescriptor.synthesize(classContext, objDesc, List.of());

        intrinsics.registerIntrinsic(referenceDesc, "of", objToReference, (builder, targetPtr, arguments) -> arguments.get(0));
        intrinsics.registerIntrinsic(referenceDesc, "fromWord", wordToReference, (builder, targetPtr, arguments) -> {
            Value wordVal = arguments.get(0);
            WordType wordType = wordVal.getType(WordType.class);
            TypeSystem ts = builder.getTypeSystem();
            ClassObjectType object = (ClassObjectType) builder.getContext().getBootstrapClassContext().resolveTypeFromClassName("java/lang", "Object");
            int refBits = ts.getReferenceSize() * 8;

            if (wordType instanceof IntegerType it) {
                if (wordType.getMinBits() > refBits) {
                    wordVal = builder.truncate(wordVal, it.asSized(refBits));
                }
            } else {
                ctxt.error(builder.getLocation(), "Only integers can currently be converted to references");
                return builder.getLiteralFactory().nullLiteralOfType(object.getReference());
            }
            return builder.bitCast(wordVal, object.getReference());
        });
        intrinsics.registerIntrinsic(referenceDesc, "toObject", toObject, (builder, instance, targetPtr, arguments) -> instance);

        intrinsics.registerIntrinsic(functionDesc, "of", (builder, targetPtr, arguments) -> {
            Value value = arguments.get(0);
            if (value instanceof Dereference d && d.getPointer().getPointeeType() instanceof FunctionType) {
                return value;
            } else {
                builder.getContext().error(builder.getLocation(), "Invalid argument to `function.of()`; expected a dereferenced function pointer but got %s", value);
                throw new BlockEarlyTermination(builder.unreachable());
            }
        });
        intrinsics.registerIntrinsic(functionDesc, "asInvokable", (builder, instance, targetPtr, arguments) -> {
            if (instance instanceof Dereference d && d.getPointer().getPointeeType() instanceof FunctionType) {
                return d.getPointer();
            } else {
                builder.getContext().error(builder.getLocation(), "Invalid receiver for `function.asInvokable()`; expected a dereferenced function pointer but got %s", instance);
                throw new BlockEarlyTermination(builder.unreachable());
            }
        });

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.loadTypeId(arguments.get(0));

        intrinsics.registerIntrinsic(cNativeDesc, "typeIdOf", objTypeIdDesc, typeOf);

        InstanceFieldElement elementTypeField = CoreClasses.get(ctxt).getRefArrayElementTypeIdField();

        StaticIntrinsic elementTypeOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.decodeReference(arguments.get(0)), elementTypeField));

        intrinsics.registerIntrinsic(cNativeDesc, "elementTypeIdOf", objArrayTypeIdDesc, elementTypeOf);

        StaticIntrinsic addrOf = (builder, target, arguments) -> {
            Value value = arguments.get(0);
            if (value instanceof Dereference deref) {
                return deref.getPointer();
            }
            while (value instanceof BitCast || value instanceof Extend || value instanceof Truncate) {
                value = ((WordCastValue)value).getInput();
            }
            if (value instanceof Load load) {
                return load.getPointer();
            } else {
                ctxt.error(builder.getLocation(), "Cannot take address of value");
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(value.getType().getPointer());
            }
        };

        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", addrOf);

        StaticIntrinsic refToPtr = (builder, target, arguments) -> {
            Value value = arguments.get(0);
            if (value.getType() instanceof ReferenceType) {
                return builder.decodeReference(value);
            } else {
                ctxt.error(builder.getLocation(), "Cannot convert non-reference to pointer");
                return ctxt.getLiteralFactory().nullLiteralOfType(ctxt.getTypeSystem().getVoidType().getPointer());
            }
        };

        StaticIntrinsic deref1 = (builder, target, arguments) -> builder.deref(arguments.get(0));
        StaticIntrinsic deref2 = (builder, target, arguments) -> {
            if (arguments.get(1) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                PointerType pt = typeLiteral.getValue().getPointer();
                return builder.deref(builder.bitCast(arguments.get(0), pt));
            } else {
                ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                return builder.emptyVoid();
            }
        };

        intrinsics.registerIntrinsic(cNativeDesc, "deref", MethodDescriptor.synthesize(classContext, objDesc, List.of(ptrDesc)), deref1);
        intrinsics.registerIntrinsic(cNativeDesc, "deref", MethodDescriptor.synthesize(classContext, objDesc, List.of(ptrDesc, classDesc)), deref2);

        intrinsics.registerIntrinsic(cNativeDesc, "refToPtr", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(objDesc)), refToPtr);

        StaticIntrinsic ptrToRef = (builder, target, arguments) -> {
            Value value = arguments.get(0);
            if (value.getType() instanceof PointerType pt) {
                if (pt.getPointeeType() instanceof ObjectType ot) {
                    return builder.encodeReference(value, ot.getReference());
                } else {
                    // we don't know the exact type; use Object
                    ReferenceType objRef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load().getObjectType().getReference();
                    return builder.encodeReference(value, objRef);
                }
            } else {
                ctxt.error(builder.getLocation(), "Cannot convert non-pointer to reference");
                throw new BlockEarlyTermination(builder.unreachable());
            }
        };

        intrinsics.registerIntrinsic(cNativeDesc, "ptrToRef", MethodDescriptor.synthesize(classContext, objDesc, List.of(ptrDesc)), ptrToRef);

        StaticIntrinsic identityStatic = (builder, target, arguments) -> arguments.get(0);

        intrinsics.registerIntrinsic(cNativeDesc, "word", identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "wordExact", identityStatic);

        StaticIntrinsic toUnsigned = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), arguments.get(0).getType(IntegerType.class).asUnsigned());

        intrinsics.registerIntrinsic(cNativeDesc, "uword", toUnsigned);

        // bitwise operations

        StaticIntrinsic wordAnd = (builder, target, arguments) -> builder.and(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(cNativeDesc, "wordAnd", wordAnd);

        StaticIntrinsic wordOr = (builder, target, arguments) -> builder.or(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(cNativeDesc, "wordOr", wordOr);

        StaticIntrinsic wordXor = (builder, target, arguments) -> builder.xor(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(cNativeDesc, "wordXor", wordXor);

        StaticIntrinsic wordComp = (builder, target, arguments) -> builder.complement(arguments.get(0));

        intrinsics.registerIntrinsic(cNativeDesc, "wordComp", wordComp);

        StaticIntrinsic sizeof = (builder, target, arguments) -> {
            long size = arguments.get(0).getType().getSize();
            IntegerType returnType = (IntegerType) target.getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, size);
        };

        StaticIntrinsic sizeofClass = (builder, target, arguments) -> {
            Value arg = arguments.get(0);
            IntegerType returnType = (IntegerType) target.getReturnType();
            long size;
            /* Class should be ClassOf(TypeLiteral) */
            if (arg instanceof ClassOf co && co.getInput() instanceof TypeIdLiteral input) {
                if (input.getValue() instanceof ObjectType) {
                    // literal reference.class
                    size = ctxt.getTypeSystem().getReferenceSize();
                } else {
                    size = input.getValue().getSize();
                }
            } else {
                ctxt.error(builder.getLocation(), "unexpected type for sizeof(Class)");
                size = arg.getType().getSize();
            }
            return ctxt.getLiteralFactory().literalOf(returnType, size);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(nObjDesc)), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, nObjDesc))), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(classDesc)), sizeofClass);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeofArray", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, classDesc))), sizeofClass);

        StaticIntrinsic alignof = (builder, target, arguments) -> {
            ValueType argType = arguments.get(0).getType();
            long align;
            if (argType instanceof TypeIdType) {
                align = ((TypeIdType) argType).getUpperBound().getAlign();
            } else {
                align = argType.getAlign();
            }
            IntegerType returnType = (IntegerType) target.getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, align);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "alignof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(nObjDesc)), alignof);
        intrinsics.registerIntrinsic(cNativeDesc, "alignof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(classDesc)), alignof);

        StaticIntrinsic offsetof = (builder, target, arguments) -> {
            Value objExpr = arguments.get(0);
            long offset = 0;
            if (objExpr instanceof Dereference deref) {
                if (deref.getPointer() instanceof MemberOf memberOf) {
                    offset = memberOf.getMember().getOffset();
                } else if (deref.getPointer() instanceof MemberOfUnion) {
                    ctxt.warning(builder.getLocation(), "offset of union member is always zero");
                } else {
                    ctxt.error(builder.getLocation(), "unexpected argument expression for offsetof(obj.field)");
                }
            } else {
                ctxt.error(builder.getLocation(), "unexpected argument expression for offsetof(obj.field)");
            }
            IntegerType returnType = (IntegerType) target.getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, offset);
        };
        intrinsics.registerIntrinsic(cNativeDesc, "offsetof", offsetof);

        StaticIntrinsic defined = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(! (arguments.get(0) instanceof UndefinedLiteral));

        intrinsics.registerIntrinsic(cNativeDesc, "defined", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), defined);

        StaticIntrinsic isComplete = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().isComplete());

        intrinsics.registerIntrinsic(cNativeDesc, "isComplete", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), isComplete);

        StaticIntrinsic isSigned = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType() instanceof SignedIntegerType);

        intrinsics.registerIntrinsic(cNativeDesc, "isSigned", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), isSigned);

        StaticIntrinsic isUnsigned = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType() instanceof UnsignedIntegerType);

        intrinsics.registerIntrinsic(cNativeDesc, "isUnsigned", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(nObjDesc)), isUnsigned);

        StaticIntrinsic typesAreEquivalent = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().equals(arguments.get(1).getType()));

        intrinsics.registerIntrinsic(cNativeDesc, "typesAreEquivalent", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, Collections.nCopies(2, nObjDesc)), typesAreEquivalent);

        StaticIntrinsic zero = (builder, target, arguments) ->
            ctxt.getLiteralFactory().literalOf(0);

        StaticIntrinsic auto = (builder, target, arguments) ->
            builder.auto(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType()));

        StaticIntrinsic autoInit = (builder, target, arguments) ->
            builder.auto(arguments.get(0));

        intrinsics.registerIntrinsic(cNativeDesc, "zero", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), zero);

        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), auto);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, nObjDesc, List.of(nObjDesc)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.B, List.of(BaseTypeDescriptor.B)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.C, List.of(BaseTypeDescriptor.C)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, List.of(BaseTypeDescriptor.D)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, List.of(BaseTypeDescriptor.F)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.S, List.of(BaseTypeDescriptor.S)), autoInit);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.Z)), autoInit);

        StaticIntrinsic constant = (builder, target, arguments) ->
            ctxt.getLiteralFactory().constantLiteralOfType(ctxt.getTypeSystem().getPoisonType());

        intrinsics.registerIntrinsic(cNativeDesc, "constant", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), constant);

        final ConcurrentHashMap<Literal, Data> utf8zCache = new ConcurrentHashMap<>();
        final AtomicInteger cnt = new AtomicInteger();

        StaticIntrinsic utf8z = (builder, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            String content;
            PointerType returnType = (PointerType) target.getReturnType();
            if (arguments.get(0) instanceof StringLiteral sl) {
                content = sl.getValue();
            } else if (arguments.get(0) instanceof ObjectLiteral ol && ol.getValue() instanceof VmString vs) {
                content = vs.getContent();
            } else {
                ctxt.error(builder.getLocation(), "Argument to CNative.utf8z() must be a string literal");
                return lf.nullLiteralOfType(returnType);
            }
            byte[] bytes = (content.endsWith("\0") ? content : (content + "\0")).getBytes(StandardCharsets.UTF_8);
            assert bytes[bytes.length - 1] == 0;
            Literal literal = lf.literalOf(ts.getArrayType(ts.getNativeCharType(), bytes.length), bytes);
            Data data = utf8zCache.computeIfAbsent(literal, bal -> {
                ExecutableElement currentElement = builder.element();
                ModuleSection section = ctxt.getImplicitSection(currentElement);
                return section.addData(null, "utf8z_" + cnt.incrementAndGet(), bal);
            });
            final IntegerLiteral z = lf.literalOf(0);
            final ProgramObjectLiteral global = lf.literalOf(ctxt.getOrAddProgramModule(builder.element().getEnclosingType()).declareData(data));
            // get the zeroth array element of the zeroth pointer element of the global
            return builder.elementOf(global, z);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "utf8z", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(strDesc)), utf8z);

        StaticIntrinsic alloca = (builder, target, arguments) -> builder.stackAllocate(ctxt.getTypeSystem().getUnsignedInteger8Type(), arguments.get(0), ctxt.getLiteralFactory().literalOf(1));

        intrinsics.registerIntrinsic(cNativeDesc, "alloca", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(sizeTDesc)), alloca);

        StaticIntrinsic cast = ((builder, targetPtr, arguments) -> arguments.get(0));

        intrinsics.registerIntrinsic(cNativeDesc, "cast", MethodDescriptor.synthesize(classContext, objDesc, List.of(objDesc)), cast);

        ClassTypeDescriptor stdcStringDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/String");
        MethodDescriptor memcpyDesc = MethodDescriptor.synthesize(classContext, ptrDesc, List.of(ptrDesc, ptrDesc, sizeTDesc));
        MethodDescriptor ptrPtrToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(ptrDesc, ptrDesc));
        MethodDescriptor ptrPtrLongToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(ptrDesc, ptrDesc, BaseTypeDescriptor.J));

        StaticIntrinsic copy1 = (builder, targetPtr, arguments) -> {
            ValueType destValType = arguments.get(0).getPointeeType();
            ValueType srcValType = arguments.get(1).getPointeeType();
            if (destValType.getSize() != srcValType.getSize()) {
                ctxt.error(builder.getLocation(), "Attempt to copy objects of different sizes (destination size is %d bytes, source size is %d bytes)", destValType.getSize(), srcValType.getSize());
                throw new BlockEarlyTermination(builder.unreachable());
            }
            // for now, just memcpy; todo: Copy node
            Value memcpy = builder.resolveStaticMethod(stdcStringDesc, "memcpy", memcpyDesc);
            builder.call(memcpy, List.of(arguments.get(0), arguments.get(1), ctxt.getLiteralFactory().literalOf(destValType.getSize())));
            return builder.emptyVoid();
        };

        StaticIntrinsic copy2 = (builder, targetPtr, arguments) -> {
            ValueType destValType = arguments.get(0).getPointeeType();
            ValueType srcValType = arguments.get(1).getPointeeType();
            if (destValType.getSize() != srcValType.getSize()) {
                ctxt.error(builder.getLocation(), "Attempt to copy objects of different sizes (destination size is %d bytes, source size is %d bytes)", destValType.getSize(), srcValType.getSize());
                throw new BlockEarlyTermination(builder.unreachable());
            }
            // for now, just memcpy; todo: Copy node
            Value memcpy = builder.resolveStaticMethod(stdcStringDesc, "memcpy", memcpyDesc);
            builder.call(memcpy, List.of(arguments.get(0), arguments.get(1), builder.multiply(arguments.get(2), ctxt.getLiteralFactory().literalOf(destValType.getSize()))));
            return builder.emptyVoid();
        };

        intrinsics.registerIntrinsic(cNativeDesc, "copy", ptrPtrToVoid, copy1);
        intrinsics.registerIntrinsic(cNativeDesc, "copy", ptrPtrLongToVoid, copy2);
    }

    private static void registerNObjectIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor nObjDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$object");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        InstanceIntrinsic castToType = (builder, input, target, arguments) -> {
            Value arg0 = arguments.get(0);
            if (arg0 instanceof ClassOf) {
                Value typeLit = ((ClassOf) arg0).getInput();
                if (typeLit instanceof TypeIdLiteral) {
                    ValueType toType = ((TypeIdLiteral) typeLit).getValue();
                    if (toType instanceof WordType) {
                        return smartConvert(builder, input, (WordType) toType, false);
                    } else {
                        return input;
                    }
                }
            }
            ctxt.error(builder.getLocation(), "Expected class literal as argument to cast");
            return input;
        };

        InstanceIntrinsic identity = (builder, instance, target, arguments) -> instance;

        intrinsics.registerIntrinsic(nObjDesc, "cast", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), identity);
        intrinsics.registerIntrinsic(nObjDesc, "cast", MethodDescriptor.synthesize(classContext, nObjDesc, List.of(classDesc)), castToType);
    }

    private static void registerWordIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor wordDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$word");

        InstanceIntrinsic xxxValue = (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getReturnType();
            return smartConvert(builder, instance, to, true);
        };

        intrinsics.registerIntrinsic(wordDesc, "byteValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.B, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "booleanValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "charValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.C, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "doubleValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "floatValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "intValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "longValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of()), xxxValue);
        intrinsics.registerIntrinsic(wordDesc, "shortValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.S, List.of()), xxxValue);

        intrinsics.registerIntrinsic(wordDesc, "ubyteValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of()), (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getReturnType();
            return builder.extend(smartConvert(builder, instance, ctxt.getTypeSystem().getUnsignedInteger8Type(), true), to);
        });
        intrinsics.registerIntrinsic(wordDesc, "ushortValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of()), (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getReturnType();
            return builder.extend(smartConvert(builder, instance, ctxt.getTypeSystem().getUnsignedInteger16Type(), true), to);
        });
        intrinsics.registerIntrinsic(wordDesc, "uintValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of()), (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getReturnType();
            return builder.extend(smartConvert(builder, instance, ctxt.getTypeSystem().getUnsignedInteger32Type(), true), to);
        });

        InstanceIntrinsic isZero = (builder, instance, target, arguments) -> builder.isEq(instance, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType()));

        intrinsics.registerIntrinsic(wordDesc, "isZero", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isZero);
        intrinsics.registerIntrinsic(wordDesc, "isNull", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isZero);
        intrinsics.registerIntrinsic(wordDesc, "isFalse", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isZero);

        InstanceIntrinsic isNonZero = (builder, instance, target, arguments) -> builder.isNe(instance, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType()));

        intrinsics.registerIntrinsic(wordDesc, "isNonZero", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isNonZero);
        intrinsics.registerIntrinsic(wordDesc, "isNonNull", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isNonZero);
        intrinsics.registerIntrinsic(wordDesc, "isTrue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of()), isNonZero);

        InstanceIntrinsic isLt = (builder, instance, target, arguments) -> builder.isLt(instance, arguments.get(0));
        InstanceIntrinsic isGt = (builder, instance, target, arguments) -> builder.isGt(instance, arguments.get(0));
        InstanceIntrinsic isLe = (builder, instance, target, arguments) -> builder.isLe(instance, arguments.get(0));
        InstanceIntrinsic isGe = (builder, instance, target, arguments) -> builder.isGe(instance, arguments.get(0));

        intrinsics.registerIntrinsic(wordDesc, "isLt", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(wordDesc)), isLt);
        intrinsics.registerIntrinsic(wordDesc, "isGt", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(wordDesc)), isGt);
        intrinsics.registerIntrinsic(wordDesc, "isLe", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(wordDesc)), isLe);
        intrinsics.registerIntrinsic(wordDesc, "isGe", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(wordDesc)), isGe);
    }

    private static void registerPtrIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");
        ClassTypeDescriptor ptrDiffTDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stddef$ptrdiff_t");
        ClassTypeDescriptor sizeTDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stddef$size_t");

        MethodDescriptor emptyToObjDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of());
        MethodDescriptor classToObjDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of(classDesc));
        MethodDescriptor objToVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(objDesc));
        MethodDescriptor classObjToVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(classDesc, objDesc));
        MethodDescriptor objObjToBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(objDesc, objDesc));
        MethodDescriptor classObjObjToBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(classDesc, objDesc, objDesc));
        MethodDescriptor objObjToObjDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of(objDesc, objDesc));
        MethodDescriptor classObjObjToObjDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of(classDesc, objDesc, objDesc));
        MethodDescriptor objToObjDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of(objDesc));
        MethodDescriptor classObjToObjDesc = MethodDescriptor.synthesize(classContext, objDesc, List.of(classDesc, objDesc));

        InstanceIntrinsic identity = (builder, instance, target, arguments) -> instance;

        intrinsics.registerIntrinsic(ptrDesc, "asArray", MethodDescriptor.synthesize(classContext, ArrayTypeDescriptor.of(classContext, objDesc), List.of()), identity);

        InstanceIntrinsic get = (builder, instance, target, arguments) ->
            builder.load(builder.offsetPointer(instance, arguments.get(0)), SingleUnshared);

        intrinsics.registerIntrinsic(ptrDesc, "get", MethodDescriptor.synthesize(classContext, objDesc, List.of(BaseTypeDescriptor.I)), get);

        InstanceIntrinsic set = (builder, instance, target, arguments) -> {
            builder.store(builder.offsetPointer(instance, arguments.get(0)), arguments.get(1), SingleUnshared);
            return builder.emptyVoid();
        };

        intrinsics.registerIntrinsic(ptrDesc, "set", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I, objDesc)), set);

        InstanceIntrinsic plus = (builder, instance, target, arguments) -> builder.offsetPointer(instance, arguments.get(0));

        intrinsics.registerIntrinsic(ptrDesc, "plus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.I)), plus);
        intrinsics.registerIntrinsic(ptrDesc, "plus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.J)), plus);

        InstanceIntrinsic minus = (builder, instance, target, arguments) -> builder.offsetPointer(instance, builder.negate(arguments.get(0)));

        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.I)), minus);
        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(ptrDiffTDesc)), minus);
        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(sizeTDesc)), minus);

        InstanceIntrinsic ptrPtrMinus = (builder, instance, targetPtr, arguments) -> builder.pointerDifference(instance, arguments.get(0));

        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDiffTDesc, List.of(ptrDesc)), ptrPtrMinus);

        Literal zeroVoid = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        InstanceIntrinsic sel = (builder, instance, target, arguments) -> builder.deref(instance);
        InstanceIntrinsic selWithType = (builder, instance, target, arguments) -> {
            if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                PointerType pt = typeLiteral.getValue().getPointer();
                return builder.deref(builder.bitCast(instance, pt));
            } else {
                ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                return zeroVoid;
            }
        };

        intrinsics.registerIntrinsic(ptrDesc, "sel", emptyToObjDesc, sel);
        intrinsics.registerIntrinsic(ptrDesc, "sel", classToObjDesc, selWithType);

        // memory accesses

        Map<String, ReadAccessMode> readModeMap = Map.of(
            "Unshared", SingleUnshared,
            "Plain", SinglePlain,
            "Opaque", SingleOpaque,
            "SingleAcquire", SingleAcquire,
            "SingleRelease", SinglePlain,
            "Acquire", GlobalAcquire,
            "Release", GlobalPlain,
            "", GlobalSeqCst,
            "Volatile", GlobalSeqCst
        );

        Map<String, WriteAccessMode> writeModeMap = Map.of(
            "Unshared", SingleUnshared,
            "Plain", SinglePlain,
            "Opaque", SingleOpaque,
            "SingleAcquire", SinglePlain,
            "SingleRelease", SingleRelease,
            "Acquire", GlobalPlain,
            "Release", GlobalRelease,
            "", GlobalSeqCst,
            "Volatile", GlobalSeqCst
        );

        // loadXxx()

        for (String name : List.of("Unshared", "Plain", "Opaque", "SingleAcquire", "Acquire", "Volatile")) {
            ReadAccessMode mode = readModeMap.get(name);
            InstanceIntrinsic load = (builder, instance, target, arguments) -> builder.load(instance, mode);
            intrinsics.registerIntrinsic(ptrDesc, "load" + name, emptyToObjDesc, load);
            InstanceIntrinsic loadWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    return builder.load(builder.bitCast(instance, typeLiteral.getValue().getPointer()), mode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "load" + name, classToObjDesc, loadWithType);
        }

        // storeXxx()

        for (String name : List.of("Unshared", "Plain", "Opaque", "SingleRelease", "Release", "Volatile")) {
            WriteAccessMode mode = writeModeMap.get(name);
            InstanceIntrinsic store = (builder, instance, target, arguments) -> {
                builder.store(instance, arguments.get(0), mode);
                return zeroVoid;
            };
            intrinsics.registerIntrinsic(ptrDesc, "store" + name, objToVoidDesc, store);
            InstanceIntrinsic storeWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    builder.store(builder.bitCast(instance, typeLiteral.getValue().getPointer()), arguments.get(1), mode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                }
                return zeroVoid;
            };
            intrinsics.registerIntrinsic(ptrDesc, "store" + name, classObjToVoidDesc, storeWithType);
        }

        // compareAndSetXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic cas = (builder, instance, target, arguments) -> {
                PointerType pt = (PointerType) instance.getType();
                Value res = builder.cmpAndSwap(instance, arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.STRONG);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(1));
            };
            intrinsics.registerIntrinsic(ptrDesc, "compareAndSet" + name, objObjToBoolDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.bitCast(instance, pt), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.STRONG);
                    return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(1));
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "compareAndSet" + name, classObjObjToBoolDesc, casWithType);
        }

        // compareAndSwapXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic cas = (builder, instance, target, arguments) -> {
                PointerType pt = (PointerType) instance.getType();
                Value res = builder.cmpAndSwap(instance, arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.STRONG);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(0));
            };
            intrinsics.registerIntrinsic(ptrDesc, "compareAndSwap" + name, objObjToObjDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.bitCast(instance, pt), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.STRONG);
                    return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(0));
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "compareAndSwap" + name, classObjObjToObjDesc, casWithType);
        }

        // weakCompareAndSetXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic cas = (builder, instance, target, arguments) -> {
                PointerType pt = (PointerType) instance.getType();
                Value res = builder.cmpAndSwap(instance, arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.WEAK);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(1));
            };
            intrinsics.registerIntrinsic(ptrDesc, "weakCompareAndSet" + name, objObjToBoolDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.bitCast(instance, pt), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.WEAK);
                    return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(1));
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "weakCompareAndSet" + name, classObjObjToBoolDesc, casWithType);
        }

        // weakCompareAndSwapXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic cas = (builder, instance, target, arguments) -> {
                PointerType pt = (PointerType) instance.getType();
                Value res = builder.cmpAndSwap(instance, arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.WEAK);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(0));
            };
            intrinsics.registerIntrinsic(ptrDesc, "weakCompareAndSwap" + name, objObjToObjDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.bitCast(instance, pt), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.WEAK);
                    return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(0));
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "weakCompareAndSwap" + name, classObjObjToObjDesc, casWithType);
        }

        // getAndSetXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.SET, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSet" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.SET, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndSet" + name, classObjToObjDesc, opWithType);
        }

        // getAndSetMinXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.MIN, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSetMin" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.MIN, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndSetMin" + name, classObjToObjDesc, opWithType);
        }

        // getAndSetMaxXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.MAX, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSetMax" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.MAX, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndSetMax" + name, classObjToObjDesc, opWithType);
        }

        // getAndAddXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.ADD, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndAdd" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.ADD, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndAdd" + name, classObjToObjDesc, opWithType);
        }

        // getAndSubtractXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.SUB, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSubtract" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.SUB, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndSubtract" + name, classObjToObjDesc, opWithType);
        }

        // getAndBitwiseAndXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.BITWISE_AND, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseAnd" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.BITWISE_AND, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseAnd" + name, classObjToObjDesc, opWithType);
        }

        // getAndBitwiseOrXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.BITWISE_OR, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseOr" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.BITWISE_OR, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseOr" + name, classObjToObjDesc, opWithType);
        }

        // getAndBitwiseXorXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.BITWISE_XOR, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseXor" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.BITWISE_XOR, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseXor" + name, classObjToObjDesc, opWithType);
        }

        // getAndBitwiseNandXxx()

        for (String name : List.of("Opaque", "Acquire", "Release", "")) {
            ReadAccessMode readMode = readModeMap.get(name);
            WriteAccessMode writeMode = writeModeMap.get(name);
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(instance, ReadModifyWrite.Op.BITWISE_NAND, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseNand" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeIdLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.bitCast(instance, pt), ReadModifyWrite.Op.BITWISE_NAND, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseNand" + name, classObjToObjDesc, opWithType);
        }
    }

    static Value smartConvert(BasicBlockBuilder builder, Value input, WordType toType, boolean cRules) {
        if (input instanceof Dereference deref) {
            return smartConvert(builder, builder.load(deref.getPointer(), SinglePlain), toType, cRules);
        }
        CompilationContext ctxt = builder.element().getEnclosingType().getContext().getCompilationContext();
        ValueType fromType = input.getType();
        // work out the behavior based on input and output types
        if (toType instanceof BooleanType) {
            if (fromType instanceof BooleanType) {
                return input;
            } else if (cRules) {
                // in this case we want != 0 behavior like C
                return builder.isNe(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()));
            } else {
                // in this case we want bit cast behavior
                return builder.truncate(input, toType);
            }
        } else if (toType instanceof IntegerType it) {
            if (fromType instanceof IntegerType inputType) {
                if (toType.getMinBits() > inputType.getMinBits()) {
                    return builder.extend(input, toType);
                } else if (toType.getMinBits() < inputType.getMinBits()) {
                    return builder.truncate(input, toType);
                } else {
                    return builder.bitCast(input, toType);
                }
            } else if (fromType instanceof FloatType) {
                return builder.fpToInt(input, it);
            } else if (fromType instanceof PointerType || fromType instanceof ReferenceType) {
                return builder.bitCast(input, toType);
            } else {
                return input;
            }
        } else if (toType instanceof FloatType ft) {
            if (fromType instanceof FloatType inputType) {
                if (toType.getMinBits() > inputType.getMinBits()) {
                    return builder.extend(input, toType);
                } else if (toType.getMinBits() < inputType.getMinBits()) {
                    return builder.truncate(input, toType);
                } else {
                    return input;
                }
            } else if (fromType instanceof IntegerType) {
                return builder.intToFp(input, ft);
            } else if (fromType instanceof WordType) {
                return builder.bitCast(input, toType);
            } else {
                return input;
            }
        } else if (toType instanceof PointerType pt) {
            if (fromType instanceof PointerType || fromType instanceof IntegerType) {
                return builder.bitCast(input, toType);
            } else if (fromType instanceof ReferenceType) {
                return builder.decodeReference(input, pt);
            } else {
                return builder.bitCast(input, toType);
            }
        } else {
            // give it our best try
            return builder.bitCast(input, toType);
        }
    }
}
