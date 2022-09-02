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
import org.qbicc.graph.Extend;
import org.qbicc.graph.Load;
import org.qbicc.graph.MemberSelector;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.WordCastValue;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.Data;
import org.qbicc.object.ModuleSection;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.intrinsics.InstanceIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
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

        ClassTypeDescriptor vmDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/VM");
        ClassTypeDescriptor cNativeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(classContext, objDesc);
        ClassTypeDescriptor nObjDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$object");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");
        ClassTypeDescriptor constCharPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$const_char_ptr");
        ClassTypeDescriptor wordDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$word");
        ClassTypeDescriptor tgDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ThreadGroup");
        ClassTypeDescriptor thrDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        ClassTypeDescriptor strDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        ClassTypeDescriptor boolPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Bool_ptr");

        ClassTypeDescriptor float32ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Float32_ptr");
        ClassTypeDescriptor float64ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Float64_ptr");

        ClassTypeDescriptor uint16ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$uint16_t_ptr");

        ClassTypeDescriptor int8ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int8_t_ptr");
        ClassTypeDescriptor int16ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int16_t_ptr");
        ClassTypeDescriptor int32ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int32_t_ptr");
        ClassTypeDescriptor int64ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int64_t_ptr");

        ClassTypeDescriptor sizeTDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stddef$size_t");

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objArrayTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objArrayDesc));

        StaticIntrinsic typeOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), CoreClasses.get(ctxt).getObjectTypeIdField()));

        intrinsics.registerIntrinsic(cNativeDesc, "typeIdOf", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = CoreClasses.get(ctxt).getRefArrayElementTypeIdField();

        StaticIntrinsic elementTypeOf = (builder, target, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), elementTypeField));

        intrinsics.registerIntrinsic(cNativeDesc, "elementTypeIdOf", objArrayTypeIdDesc, elementTypeOf);

        StaticIntrinsic addrOf = (builder, target, arguments) -> {
            Value value = arguments.get(0);
            if (value instanceof MemberSelector ms) {
                return builder.addressOf(ms.getValueHandle());
            }
            while (value instanceof BitCast || value instanceof Extend || value instanceof Truncate) {
                value = ((WordCastValue)value).getInput();
            }
            if (value instanceof Load load) {
                return builder.addressOf(load.getValueHandle());
            } else {
                ctxt.error(builder.getLocation(), "Cannot take address of value");
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(value.getType().getPointer());
            }
        };

        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int8ptrDesc, List.of(BaseTypeDescriptor.B)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, uint16ptrDesc, List.of(BaseTypeDescriptor.C)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, float64ptrDesc, List.of(BaseTypeDescriptor.D)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, float32ptrDesc, List.of(BaseTypeDescriptor.F)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int32ptrDesc, List.of(BaseTypeDescriptor.I)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int64ptrDesc, List.of(BaseTypeDescriptor.J)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, int16ptrDesc, List.of(BaseTypeDescriptor.S)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, boolPtrDesc, List.of(BaseTypeDescriptor.Z)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(nObjDesc)), addrOf);
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(objDesc)), addrOf);
        // todo: this one is deprecated
        intrinsics.registerIntrinsic(cNativeDesc, "addr_of", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(nObjDesc)), addrOf);

        StaticIntrinsic refToPtr = (builder, target, arguments) -> {
            Value value = arguments.get(0);
            if (value.getType() instanceof ReferenceType rt) {
                return builder.valueConvert(value, rt.getUpperBound().getPointer());
            } else {
                ctxt.error(builder.getLocation(), "Cannot convert non-reference to pointer");
                return ctxt.getLiteralFactory().nullLiteralOfType(ctxt.getTypeSystem().getVoidType().getPointer());
            }
        };

        intrinsics.registerIntrinsic(cNativeDesc, "refToPtr", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(objDesc)), refToPtr);

        StaticIntrinsic ptrToRef = (builder, target, arguments) -> {
            Value value = arguments.get(0);
            if (value.getType() instanceof PointerType pt) {
                if (pt.getPointeeType() instanceof ObjectType ot) {
                    return builder.valueConvert(value, ot.getReference());
                } else {
                    // we don't know the exact type; use Object
                    ReferenceType objRef = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load().getObjectType().getReference();
                    return builder.valueConvert(value, objRef);
                }
            } else {
                ctxt.error(builder.getLocation(), "Cannot convert non-pointer to reference");
                throw new BlockEarlyTermination(builder.unreachable());
            }
        };

        intrinsics.registerIntrinsic(cNativeDesc, "ptrToRef", MethodDescriptor.synthesize(classContext, objDesc, List.of(ptrDesc)), ptrToRef);

        StaticIntrinsic attachNewThread = (builder, target, arguments) -> {
            //java.lang.Thread.nextThreadID
            Value thread = builder.new_(thrDesc);
            // immediately set the thread to be the current thread
            builder.store(builder.staticField(vmDesc, "_qbicc_bound_thread", thrDesc), thread, SingleUnshared);
            // now start initializing
            DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Thread");
            LoadedTypeDefinition jltVal = jlt.load();
            // find all the fields
            FieldElement nameFld = jltVal.findField("name");
            FieldElement tidFld = jltVal.findField("tid");
            FieldElement groupFld = jltVal.findField("group");
            FieldElement threadStatusFld = jltVal.findField("threadStatus");
            FieldElement priorityFld = jltVal.findField("priority");

            ValueHandle threadRef = builder.referenceHandle(thread);
            builder.store(builder.instanceFieldOf(threadRef, nameFld), arguments.get(0), SingleUnshared);
            builder.store(builder.instanceFieldOf(threadRef, groupFld), arguments.get(1), SingleUnshared);
            Value tid = builder.call(builder.staticMethod(thrDesc, "nextThreadID", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of())), List.of());
            builder.store(builder.instanceFieldOf(threadRef, tidFld), tid, SingleUnshared);
            // priority default is Thread.NORM_PRIORITY
            Value normPriority = builder.load(builder.staticField(jltVal.findField("NORM_PRIORITY")), SingleUnshared);
            builder.store(builder.instanceFieldOf(threadRef, priorityFld), normPriority, SingleUnshared);

            // set thread to be running with JVMTI status for RUNNABLE and ALIVE
            builder.store(builder.instanceFieldOf(threadRef, threadStatusFld), ctxt.getLiteralFactory().literalOf(0x05), SingleUnshared);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());
        };

        intrinsics.registerIntrinsic(cNativeDesc, "attachNewThread", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            strDesc, tgDesc
        )), attachNewThread);

        StaticIntrinsic identityStatic = (builder, target, arguments) -> arguments.get(0);

        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.Z)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.I)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.J)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.F)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.D)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "word", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.Z)), identityStatic);

        StaticIntrinsic toUnsigned = (builder, target, arguments) ->
            builder.bitCast(arguments.get(0), ((IntegerType)arguments.get(0).getType()).asUnsigned());

        intrinsics.registerIntrinsic(cNativeDesc, "uword", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.I)), toUnsigned);
        intrinsics.registerIntrinsic(cNativeDesc, "uword", MethodDescriptor.synthesize(classContext, wordDesc, List.of(BaseTypeDescriptor.J)), toUnsigned);

        // bitwise operations

        MethodDescriptor wordWordToWord = MethodDescriptor.synthesize(classContext, wordDesc, List.of(wordDesc, wordDesc));
        MethodDescriptor wordToWord = MethodDescriptor.synthesize(classContext, wordDesc, List.of(wordDesc));

        StaticIntrinsic wordAnd = (builder, target, arguments) -> builder.and(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(cNativeDesc, "wordAnd", wordWordToWord, wordAnd);

        StaticIntrinsic wordOr = (builder, target, arguments) -> builder.or(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(cNativeDesc, "wordOr", wordWordToWord, wordOr);

        StaticIntrinsic wordXor = (builder, target, arguments) -> builder.xor(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(cNativeDesc, "wordXor", wordWordToWord, wordXor);

        StaticIntrinsic wordComp = (builder, target, arguments) -> builder.complement(arguments.get(0));

        intrinsics.registerIntrinsic(cNativeDesc, "wordComp", wordToWord, wordComp);

        StaticIntrinsic sizeof = (builder, target, arguments) -> {
            long size = arguments.get(0).getType().getSize();
            IntegerType returnType = (IntegerType) target.getExecutable().getType().getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, size);
        };

        StaticIntrinsic sizeofClass = (builder, target, arguments) -> {
            Value arg = arguments.get(0);
            long size;
            /* Class should be ClassOf(TypeLiteral) */
            if (arg instanceof ClassOf co && co.getInput() instanceof TypeLiteral input && !(input.getValue() instanceof ObjectType)) {
                size = input.getValue().getSize();
            } else {
                ctxt.error(builder.getLocation(), "unexpected type for sizeof(Class)");
                size = arg.getType().getSize();
            }
            IntegerType returnType = (IntegerType) target.getExecutable().getType().getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, size);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(nObjDesc)), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, nObjDesc))), sizeof);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(classDesc)), sizeofClass);
        intrinsics.registerIntrinsic(cNativeDesc, "sizeofArray", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(ArrayTypeDescriptor.of(classContext, classDesc))), sizeofClass);

        StaticIntrinsic alignof = (builder, target, arguments) -> {
            ValueType argType = arguments.get(0).getType();
            long align;
            if (argType instanceof TypeType) {
                align = ((TypeType) argType).getUpperBound().getAlign();
            } else {
                align = argType.getAlign();
            }
            IntegerType returnType = (IntegerType) target.getExecutable().getType().getReturnType();
            return ctxt.getLiteralFactory().literalOf(returnType, align);
        };

        intrinsics.registerIntrinsic(cNativeDesc, "alignof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(nObjDesc)), alignof);
        intrinsics.registerIntrinsic(cNativeDesc, "alignof", MethodDescriptor.synthesize(classContext, sizeTDesc, List.of(classDesc)), alignof);

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

        intrinsics.registerIntrinsic(cNativeDesc, "zero", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), zero);

        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), zero);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, nObjDesc, List.of(nObjDesc)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.B, List.of(BaseTypeDescriptor.B)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.C, List.of(BaseTypeDescriptor.C)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, List.of(BaseTypeDescriptor.D)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, List.of(BaseTypeDescriptor.F)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.S, List.of(BaseTypeDescriptor.S)), identityStatic);
        intrinsics.registerIntrinsic(cNativeDesc, "auto", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.Z)), identityStatic);

        StaticIntrinsic constant = (builder, target, arguments) ->
            ctxt.getLiteralFactory().constantLiteralOfType(ctxt.getTypeSystem().getPoisonType());

        intrinsics.registerIntrinsic(cNativeDesc, "constant", MethodDescriptor.synthesize(classContext, nObjDesc, List.of()), constant);

        final ConcurrentHashMap<Literal, Data> utf8zCache = new ConcurrentHashMap<>();
        final AtomicInteger cnt = new AtomicInteger();

        StaticIntrinsic utf8z = (builder, target, arguments) -> {
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            String content;
            PointerType returnType = (PointerType) target.getPointeeType().getReturnType();
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
                ExecutableElement currentElement = builder.getCurrentElement();
                ModuleSection section = ctxt.getImplicitSection(currentElement);
                return section.addData(null, "utf8z_" + cnt.incrementAndGet(), bal);
            });
            final IntegerLiteral z = lf.literalOf(0);
            final PointerLiteral global = lf.literalOf(ctxt.getOrAddProgramModule(builder.getCurrentElement().getEnclosingType()).declareData(data).getPointer());
            // get the zeroth array element of the zeroth pointer element of the global
            return builder.addressOf(builder.elementOf(builder.pointerHandle(global), z));
        };

        intrinsics.registerIntrinsic(cNativeDesc, "utf8z", MethodDescriptor.synthesize(classContext, constCharPtrDesc, List.of(strDesc)), utf8z);

        StaticIntrinsic alloca = (builder, target, arguments) -> builder.stackAllocate(ctxt.getTypeSystem().getUnsignedInteger8Type(), arguments.get(0), ctxt.getLiteralFactory().literalOf(1));

        intrinsics.registerIntrinsic(cNativeDesc, "alloca", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(sizeTDesc)), alloca);
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
                if (typeLit instanceof TypeLiteral) {
                    ValueType toType = ((TypeLiteral) typeLit).getValue();
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
            WordType to = (WordType) target.getExecutable().getType().getReturnType();
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
            WordType to = (WordType) target.getExecutable().getType().getReturnType();
            return builder.extend(smartConvert(builder, instance, ctxt.getTypeSystem().getUnsignedInteger8Type(), true), to);
        });
        intrinsics.registerIntrinsic(wordDesc, "ushortValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of()), (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getExecutable().getType().getReturnType();
            return builder.extend(smartConvert(builder, instance, ctxt.getTypeSystem().getUnsignedInteger16Type(), true), to);
        });
        intrinsics.registerIntrinsic(wordDesc, "uintValue", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of()), (builder, instance, target, arguments) -> {
            WordType to = (WordType) target.getExecutable().getType().getReturnType();
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
            builder.load(builder.pointerHandle(instance, arguments.get(0)), SingleUnshared);

        intrinsics.registerIntrinsic(ptrDesc, "get", MethodDescriptor.synthesize(classContext, objDesc, List.of(BaseTypeDescriptor.I)), get);

        InstanceIntrinsic set = (builder, instance, target, arguments) -> {
            builder.store(builder.pointerHandle(instance, arguments.get(0)), arguments.get(1), SingleUnshared);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getExecutable().getType().getReturnType());
        };

        intrinsics.registerIntrinsic(ptrDesc, "set", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(BaseTypeDescriptor.I, objDesc)), set);

        InstanceIntrinsic plus = (builder, instance, target, arguments) -> builder.addressOf(builder.pointerHandle(instance, arguments.get(0)));

        intrinsics.registerIntrinsic(ptrDesc, "plus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.I)), plus);
        intrinsics.registerIntrinsic(ptrDesc, "plus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.J)), plus);

        InstanceIntrinsic minus = (builder, instance, target, arguments) -> builder.addressOf(builder.pointerHandle(instance, builder.negate(arguments.get(0))));

        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(BaseTypeDescriptor.I)), minus);
        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(ptrDiffTDesc)), minus);
        intrinsics.registerIntrinsic(ptrDesc, "minus", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(sizeTDesc)), minus);

        Literal zeroVoid = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getVoidType());

        InstanceIntrinsic sel = (builder, instance, target, arguments) -> builder.selectMember(builder.pointerHandle(instance));
        InstanceIntrinsic selWithType = (builder, instance, target, arguments) -> {
            if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                PointerType pt = typeLiteral.getValue().getPointer();
                return builder.selectMember(builder.pointerHandle(builder.bitCast(instance, pt)));
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
            InstanceIntrinsic load = (builder, instance, target, arguments) -> builder.load(builder.pointerHandle(instance), mode);
            intrinsics.registerIntrinsic(ptrDesc, "load" + name, emptyToObjDesc, load);
            InstanceIntrinsic loadWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    return builder.load(builder.pointerHandle(builder.bitCast(instance, typeLiteral.getValue().getPointer())), mode);
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
                builder.store(builder.pointerHandle(instance), arguments.get(0), mode);
                return zeroVoid;
            };
            intrinsics.registerIntrinsic(ptrDesc, "store" + name, objToVoidDesc, store);
            InstanceIntrinsic storeWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    builder.store(builder.pointerHandle(builder.bitCast(instance, typeLiteral.getValue().getPointer())), arguments.get(1), mode);
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
                Value res = builder.cmpAndSwap(builder.pointerHandle(instance), arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.STRONG);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(1));
            };
            intrinsics.registerIntrinsic(ptrDesc, "compareAndSet" + name, objObjToBoolDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.pointerHandle(builder.bitCast(instance, pt)), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.STRONG);
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
                Value res = builder.cmpAndSwap(builder.pointerHandle(instance), arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.STRONG);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(0));
            };
            intrinsics.registerIntrinsic(ptrDesc, "compareAndSwap" + name, objObjToObjDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.pointerHandle(builder.bitCast(instance, pt)), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.STRONG);
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
                Value res = builder.cmpAndSwap(builder.pointerHandle(instance), arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.WEAK);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(1));
            };
            intrinsics.registerIntrinsic(ptrDesc, "weakCompareAndSet" + name, objObjToBoolDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.pointerHandle(builder.bitCast(instance, pt)), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.WEAK);
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
                Value res = builder.cmpAndSwap(builder.pointerHandle(instance), arguments.get(0), arguments.get(1), readMode, writeMode, CmpAndSwap.Strength.WEAK);
                return builder.extractMember(res, CmpAndSwap.getResultType(ctxt, pt.getPointeeType()).getMember(0));
            };
            intrinsics.registerIntrinsic(ptrDesc, "weakCompareAndSwap" + name, objObjToObjDesc, cas);
            InstanceIntrinsic casWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    Value res = builder.cmpAndSwap(builder.pointerHandle(builder.bitCast(instance, pt)), arguments.get(1), arguments.get(2), readMode, writeMode, CmpAndSwap.Strength.WEAK);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.SET, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSet" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.SET, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.MIN, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSetMin" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.MIN, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.MAX, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSetMax" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.MAX, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.ADD, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndAdd" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.ADD, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.SUB, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndSubtract" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.SUB, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.BITWISE_AND, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseAnd" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.BITWISE_AND, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.BITWISE_OR, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseOr" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.BITWISE_OR, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.BITWISE_XOR, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseXor" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.BITWISE_XOR, arguments.get(1), readMode, writeMode);
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
            InstanceIntrinsic op = (builder, instance, target, arguments) -> builder.readModifyWrite(builder.pointerHandle(instance), ReadModifyWrite.Op.BITWISE_NAND, arguments.get(0), readMode, writeMode);
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseNand" + name, objToObjDesc, op);
            InstanceIntrinsic opWithType = (builder, instance, target, arguments) -> {
                if (arguments.get(0) instanceof ClassOf classOf && classOf.getInput() instanceof TypeLiteral typeLiteral) {
                    PointerType pt = typeLiteral.getValue().getPointer();
                    return builder.readModifyWrite(builder.pointerHandle(builder.bitCast(instance, pt)), ReadModifyWrite.Op.BITWISE_NAND, arguments.get(1), readMode, writeMode);
                } else {
                    ctxt.error(builder.getLocation(), "Pointee argument must be a class literal");
                    return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(instance.getType());
                }
            };
            intrinsics.registerIntrinsic(ptrDesc, "getAndBitwiseNand" + name, classObjToObjDesc, opWithType);
        }
    }

    static Value smartConvert(BasicBlockBuilder builder, Value input, WordType toType, boolean cRules) {
        if (input instanceof MemberSelector ms) {
            return smartConvert(builder, builder.load(ms.getValueHandle(), SinglePlain), toType, cRules);
        }
        CompilationContext ctxt = builder.getCurrentElement().getEnclosingType().getContext().getCompilationContext();
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
        } else if (toType instanceof IntegerType) {
            if (fromType instanceof IntegerType inputType) {
                if (toType.getMinBits() > inputType.getMinBits()) {
                    return builder.extend(input, toType);
                } else if (toType.getMinBits() < inputType.getMinBits()) {
                    return builder.truncate(input, toType);
                } else {
                    return builder.bitCast(input, toType);
                }
            } else if (fromType instanceof WordType) {
                return builder.valueConvert(input, toType);
            } else {
                return input;
            }
        } else if (toType instanceof FloatType) {
            if (fromType instanceof FloatType inputType) {
                if (toType.getMinBits() > inputType.getMinBits()) {
                    return builder.extend(input, toType);
                } else if (toType.getMinBits() < inputType.getMinBits()) {
                    return builder.truncate(input, toType);
                } else {
                    return input;
                }
            } else if (fromType instanceof WordType) {
                return builder.valueConvert(input, toType);
            } else {
                return input;
            }
        } else if (toType instanceof PointerType) {
            if (fromType instanceof PointerType) {
                return builder.bitCast(input, toType);
            } else if (fromType instanceof WordType) {
                return builder.valueConvert(input, toType);
            } else {
                return input;
            }
        } else {
            return builder.valueConvert(input, toType);
        }
    }
}
