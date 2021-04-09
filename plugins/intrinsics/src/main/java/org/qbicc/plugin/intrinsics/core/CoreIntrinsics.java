package org.qbicc.plugin.intrinsics.core;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.Extend;
import org.qbicc.graph.Load;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.Variable;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.intrinsics.InstanceValueIntrinsic;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.plugin.intrinsics.StaticValueIntrinsic;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.ValidatedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * Core JDK intrinsics.
 */
public final class CoreIntrinsics {
    public static void register(CompilationContext ctxt) {
        registerJavaLangClassIntrinsics(ctxt);
        registerJavaLangStringUTF16Intrinsics(ctxt);
        registerJavaLangClassLoaderIntrinsics(ctxt);
        registerJavaLangSystemIntrinsics(ctxt);
        registerJavaLangThreadIntrinsics(ctxt);
        registerJavaLangThrowableIntrinsics(ctxt);
        registerJavaLangObjectIntrinsics(ctxt);
        registerJavaLangNumberIntrinsics(ctxt);
        registerJavaLangFloatDoubleMathIntrinsics(ctxt);
        registerOrgQbiccRuntimeCNativeIntrinsics(ctxt);
        registerOrgQbiccObjectModelIntrinsics(ctxt);
        registerOrgQbiccRuntimeMainIntrinsics(ctxt);
        registerOrgQbiccRuntimeValuesIntrinsics(ctxt);
        registerJavaLangMathIntrinsics(ctxt);
        registerOrgQbiccRuntimePosixPthreadCastPtr(ctxt);
    }

    private static StaticIntrinsic setVolatile(FieldElement field) {
        return (builder, owner, name, descriptor, arguments) -> builder.store(builder.staticField(field), arguments.get(0), MemoryAtomicityMode.VOLATILE);
    }

    public static void registerJavaLangClassIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor jlsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        MethodDescriptor classToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(jlcDesc));
        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());
        MethodDescriptor emptyToString = MethodDescriptor.synthesize(classContext, jlsDesc, List.of());
        MethodDescriptor stringToClass = MethodDescriptor.synthesize(classContext, jlcDesc, List.of(jlsDesc));
        MethodDescriptor objToObj = MethodDescriptor.synthesize(classContext, jloDesc, List.of(jloDesc));

        // Assertion status

        // todo: this probably belongs in the class libraries rather than here
        StaticValueIntrinsic desiredAssertionStatus0 = (builder, owner, name, descriptor, arguments) ->
            classContext.getLiteralFactory().literalOf(false);

        InstanceValueIntrinsic cast =  (builder, kind, instance, owner, name, descriptor, arguments) -> {
            // TODO: Once we support java.lang.Class literals, we should add a check here to
            //  emit a CheckCast node instead of a call to the helper method if `instance` is a Class literal.
            MethodElement helper = ctxt.getVMHelperMethod("checkcast_class");
            builder.getFirstBuilder().invokeStatic(helper, List.of(arguments.get(0), instance));

            // Generics erasure issue. The return type of Class<T>.cast is T, but it gets wiped to Object.
            // If the result of this cast is actually used as a T, there will be a (redundant) checkcast bytecode following this operation.
            ReferenceType jlot = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").validate().getType().getReference();
            return builder.bitCast(arguments.get(0), jlot);
        };

        StaticIntrinsic registerNatives = (builder, owner, name, descriptor, arguments) -> builder.nop();

        InstanceValueIntrinsic initClassName = (builder, kind, instance, owner, name, descriptor, arguments) -> {
            // not reachable; we always would initialize our class name eagerly
            throw new BlockEarlyTermination(builder.unreachable());
        };

        StaticValueIntrinsic getPrimitiveClass = (builder, owner, name, descriptor, arguments) -> {
            // always called with a string literal
            StringLiteral lit = (StringLiteral) arguments.get(0);
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();
            ValueType type;
            switch (lit.getValue()) {
                case "byte": type = ts.getSignedInteger8Type(); break;
                case "short": type = ts.getSignedInteger16Type(); break;
                case "int": type = ts.getSignedInteger32Type(); break;
                case "long": type = ts.getSignedInteger64Type(); break;

                case "char": type = ts.getUnsignedInteger16Type(); break;

                case "float": type = ts.getFloat32Type(); break;
                case "double": type = ts.getFloat64Type(); break;

                case "boolean": type = ts.getBooleanType(); break;

                case "void": type = ts.getVoidType(); break;

                default: {
                    ctxt.error(builder.getLocation(), "Invalid argument to `getPrimitiveClass`: %s", lit.getValue());
                    throw new BlockEarlyTermination(builder.unreachable());
                }
            }
            return builder.classOf(lf.literalOfType(type));
        };

        //    static native Class<?> getPrimitiveClass(String name);

        intrinsics.registerIntrinsic(jlcDesc, "cast", objToObj, cast);
        intrinsics.registerIntrinsic(jlcDesc, "desiredAssertionStatus0", classToBool, desiredAssertionStatus0);
        intrinsics.registerIntrinsic(jlcDesc, "registerNatives", emptyToVoid, registerNatives);
        intrinsics.registerIntrinsic(jlcDesc, "initClassName", emptyToString, initClassName);
        intrinsics.registerIntrinsic(jlcDesc, "getPrimitiveClass", stringToClass, getPrimitiveClass);
    }

    public static void registerJavaLangStringUTF16Intrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlsu16Desc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StringUTF16");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        //    private static native boolean isBigEndian();

        CProbe probe = CProbe.builder().build();
        try {
            CProbe.Result result = probe.run(ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY), ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), ctxt);
            if (result == null) {
                ctxt.error("Failed to probe target endianness (no exception)");
            } else {
                StaticValueIntrinsic isBigEndian = (builder, owner, name, descriptor, arguments) ->
                    ctxt.getLiteralFactory().literalOf(result.getByteOrder() == ByteOrder.BIG_ENDIAN);

                intrinsics.registerIntrinsic(jlsu16Desc, "isBigEndian", emptyToBool, isBigEndian);
            }
        } catch (IOException e) {
            ctxt.error(e, "Failed to probe target endianness");
        }
    }

    public static void registerJavaLangClassLoaderIntrinsics(CompilationContext ctxt) {

        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jlclDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ClassLoader");

        MethodDescriptor emptyToVoid = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        StaticIntrinsic registerNatives = (builder, owner, name, descriptor, arguments) -> builder.nop();

        intrinsics.registerIntrinsic(jlclDesc, "registerNatives", emptyToVoid, registerNatives);
    }

    public static void registerJavaLangSystemIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor systemDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/System");
        ValidatedTypeDefinition jls = classContext.findDefinedType("java/lang/System").validate();
        ClassTypeDescriptor jloDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor vmDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/VM");

        MethodDescriptor objectToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(jloDesc));

        // Null and no-operation intrinsics

        StaticValueIntrinsic returnNull = (builder, owner, name, descriptor, arguments) ->
            classContext.getLiteralFactory().zeroInitializerLiteralOfType(jls.getClassType().getReference().asNullable());
        intrinsics.registerIntrinsic(systemDesc, "getSecurityManager",
            MethodDescriptor.synthesize(classContext,
                ClassTypeDescriptor.synthesize(classContext,"java/lang/SecurityManager"), List.of()),
            returnNull);

        // System public API

        FieldElement in = jls.findField("in");
        in.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        FieldElement out = jls.findField("out");
        out.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        FieldElement err = jls.findField("err");
        err.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);

        // Setters

        MethodDescriptor setPrintStreamDesc =
            MethodDescriptor.synthesize(classContext,
                BaseTypeDescriptor.V, List.of(ClassTypeDescriptor.synthesize(classContext, "java/io/PrintStream")));

        intrinsics.registerIntrinsic(systemDesc, "setIn", setPrintStreamDesc, setVolatile(in));
        intrinsics.registerIntrinsic(systemDesc, "setOut", setPrintStreamDesc, setVolatile(out));
        intrinsics.registerIntrinsic(systemDesc, "setErr", setPrintStreamDesc, setVolatile(err));

        // arraycopy

        MethodDescriptor arraycopyDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            jloDesc,
            BaseTypeDescriptor.I,
            jloDesc,
            BaseTypeDescriptor.I,
            BaseTypeDescriptor.I
        ));

        StaticIntrinsic arraycopy = (builder, owner, name, descriptor, arguments) ->
            builder.invokeStatic(vmDesc, "arraycopy", descriptor, arguments);

        intrinsics.registerIntrinsic(systemDesc, "arraycopy", arraycopyDesc, arraycopy);

        // identity hash code

        // todo: obviously non-optimal; replace once we have object headers sorted out
        StaticValueIntrinsic identityHashCode = (builder, owner, name, descriptor, arguments) ->
            ctxt.getLiteralFactory().literalOf(0);

        intrinsics.registerIntrinsic(systemDesc, "identityHashCode", objectToIntDesc, identityHashCode);
    }

    public static void registerJavaLangThreadIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        MethodDescriptor returnJlt = MethodDescriptor.synthesize(classContext, jltDesc, List.of());

        StaticValueIntrinsic currentThread = (builder, owner, name, descriptor, arguments) -> builder.currentThread();

        intrinsics.registerIntrinsic(jltDesc, "currentThread", returnJlt, currentThread);
    }

    public static void registerJavaLangThrowableIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor jltDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");
        ClassTypeDescriptor steDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StackTraceElement");
        ArrayTypeDescriptor steArrayDesc = ArrayTypeDescriptor.of(classContext, steDesc);

        ZeroInitializerLiteral zero = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getSignedInteger32Type());

        // todo: temporary, until we have a stack walker

        InstanceValueIntrinsic fillInStackTrace = (builder, kind, instance, owner, name, descriptor, arguments) ->
            instance;

        InstanceValueIntrinsic getStackTrace = (builder, kind, instance, owner, name, descriptor, arguments) ->
            builder.newArray(steArrayDesc, zero);

        intrinsics.registerIntrinsic(jltDesc, "fillInStackTrace", MethodDescriptor.synthesize(classContext, jltDesc, List.of()), fillInStackTrace);
        intrinsics.registerIntrinsic(jltDesc, "getStackTrace", MethodDescriptor.synthesize(classContext, steArrayDesc, List.of()), getStackTrace);
    }

    public static void registerJavaLangNumberIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // Mathematical intrinsics

        ClassTypeDescriptor byteDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Byte");
        ClassTypeDescriptor characterDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Character");
        ClassTypeDescriptor integerDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Integer");
        ClassTypeDescriptor longDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Long");
        ClassTypeDescriptor shortDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Short");

        // binary operations

        MethodDescriptor binaryByteToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.B, BaseTypeDescriptor.B));
        MethodDescriptor binaryCharToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.C, BaseTypeDescriptor.C));
        MethodDescriptor binaryIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.I, BaseTypeDescriptor.I));
        MethodDescriptor binaryLongDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J, BaseTypeDescriptor.J));
        MethodDescriptor binaryShortToIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.S, BaseTypeDescriptor.S));
        MethodDescriptor longIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J, BaseTypeDescriptor.I));

        StaticValueIntrinsic divideUnsigned = (builder, owner, name, descriptor, arguments) ->
            builder.divide(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        StaticValueIntrinsic remainderUnsigned = (builder, owner, name, descriptor, arguments) ->
            builder.remainder(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        intrinsics.registerIntrinsic(integerDesc, "divideUnsigned", binaryIntDesc, divideUnsigned);
        intrinsics.registerIntrinsic(longDesc, "divideUnsigned", binaryLongDesc, divideUnsigned);

        intrinsics.registerIntrinsic(integerDesc, "remainderUnsigned", binaryIntDesc, remainderUnsigned);
        intrinsics.registerIntrinsic(longDesc, "remainderUnsigned", binaryLongDesc, remainderUnsigned);

        StaticValueIntrinsic ror = (builder, owner, name, descriptor, arguments) ->
            builder.ror(arguments.get(0), arguments.get(1));

        StaticValueIntrinsic rol = (builder, owner, name, descriptor, arguments) ->
            builder.rol(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(integerDesc, "rotateRight", binaryIntDesc, ror);
        intrinsics.registerIntrinsic(longDesc, "rotateRight", longIntDesc, ror);

        intrinsics.registerIntrinsic(integerDesc, "rotateLeft", binaryIntDesc, rol);
        intrinsics.registerIntrinsic(longDesc, "rotateLeft", longIntDesc, rol);

        StaticValueIntrinsic compare = (builder, owner, name, descriptor, arguments) ->
            builder.cmp(arguments.get(0), arguments.get(1));
        StaticValueIntrinsic compareUnsigned = (builder, owner, name, descriptor, arguments) ->
            builder.cmp(asUnsigned(builder, arguments.get(0)), asUnsigned(builder, arguments.get(1)));

        intrinsics.registerIntrinsic(byteDesc, "compare", binaryByteToIntDesc, compare);
        intrinsics.registerIntrinsic(byteDesc, "compareUnsigned", binaryByteToIntDesc, compareUnsigned);
        intrinsics.registerIntrinsic(characterDesc, "compare", binaryCharToIntDesc, compare);
        intrinsics.registerIntrinsic(integerDesc, "compare", binaryIntDesc, compare);
        intrinsics.registerIntrinsic(integerDesc, "compareUnsigned", binaryIntDesc, compareUnsigned);
        intrinsics.registerIntrinsic(shortDesc, "compare", binaryShortToIntDesc, compare);
        intrinsics.registerIntrinsic(shortDesc, "compareUnsigned", binaryShortToIntDesc, compareUnsigned);
    }

    private static void registerJavaLangFloatDoubleMathIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        final var ts = ctxt.getTypeSystem();

        // Mathematical intrinsics

        ClassTypeDescriptor floatDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Float");
        ClassTypeDescriptor doubleDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Double");

        MethodDescriptor floatToIntMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(BaseTypeDescriptor.F));
        MethodDescriptor doubleToLongMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.D));

        StaticValueIntrinsic floatToRawIntBits = (builder, owner, name, descriptor, arguments) ->
            builder.bitCast(arguments.get(0), ts.getSignedInteger32Type());
        StaticValueIntrinsic doubleToRawLongBits = (builder, owner, name, descriptor, arguments) ->
            builder.bitCast(arguments.get(0), ts.getSignedInteger64Type());

        intrinsics.registerIntrinsic(floatDesc, "floatToRawIntBits", floatToIntMethodDesc, floatToRawIntBits);
        intrinsics.registerIntrinsic(doubleDesc, "doubleToRawLongBits", doubleToLongMethodDesc, doubleToRawLongBits);

        MethodDescriptor intToFloatMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, List.of(BaseTypeDescriptor.I));
        MethodDescriptor longToDoubleMethodDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, List.of(BaseTypeDescriptor.J));

        StaticValueIntrinsic intBitsToFloat = (builder, owner, name, descriptor, arguments) ->
            builder.bitCast(arguments.get(0), ts.getFloat32Type());
        StaticValueIntrinsic longBitsToDouble = (builder, owner, name, descriptor, arguments) ->
            builder.bitCast(arguments.get(0), ts.getFloat64Type());

        intrinsics.registerIntrinsic(floatDesc, "intBitsToFloat", intToFloatMethodDesc, intBitsToFloat);
        intrinsics.registerIntrinsic(doubleDesc, "longBitsToDouble", longToDoubleMethodDesc, longBitsToDouble);
    }

    static Value asUnsigned(BasicBlockBuilder builder, Value value) {
        IntegerType type = (IntegerType) value.getType();
        return builder.bitCast(value, type.asUnsigned());
    }

    public static void registerJavaLangObjectIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        // Object#getClass()Ljava/lang/Class; --> field read of the "typeId" field
        MethodDescriptor getClassDesc =
            MethodDescriptor.synthesize(classContext,
                ClassTypeDescriptor.synthesize(classContext, "java/lang/Class"), List.of());
        InstanceValueIntrinsic getClassIntrinsic = (builder, kind, instance, owner, name, descriptor, arguments) ->
            builder.classOf(builder.typeIdOf(builder.referenceHandle(instance)));
        intrinsics.registerIntrinsic(classDesc, "getClass", getClassDesc, getClassIntrinsic);
    }

    static Literal literalOf(CompilationContext ctxt, boolean v) {
        return ctxt.getLiteralFactory().literalOf(v);
    }

    static void registerOrgQbiccRuntimeCNativeIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor cNativeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ArrayTypeDescriptor objArrayDesc = ArrayTypeDescriptor.of(classContext, objDesc);
        ClassTypeDescriptor nObjDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$object");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");
        ClassTypeDescriptor tgDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ThreadGroup");
        ClassTypeDescriptor thrDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Thread");
        ClassTypeDescriptor strDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");

        ClassTypeDescriptor boolPtrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Bool_ptr");

        ClassTypeDescriptor float32ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Float32_ptr");
        ClassTypeDescriptor float64ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$_Float64_ptr");

        ClassTypeDescriptor uint16ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$uint16_t_ptr");

        ClassTypeDescriptor int8ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int8_t_ptr");
        ClassTypeDescriptor int16ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int16_t_ptr");
        ClassTypeDescriptor int32ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int32_t_ptr");
        ClassTypeDescriptor int64ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/stdc/Stdint$int64_t_ptr");

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objArrayTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objArrayDesc));

        PointerType voidPtr = ctxt.getTypeSystem().getVoidType().getPointer();

        StaticValueIntrinsic typeOf = (builder, owner, name, descriptor, arguments) ->
            builder.typeIdOf(builder.referenceHandle(arguments.get(0)));

        intrinsics.registerIntrinsic(cNativeDesc, "type_id_of", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = Layout.get(ctxt).getRefArrayElementTypeIdField();

        StaticValueIntrinsic elementTypeOf = (builder, owner, name, descriptor, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), elementTypeField), MemoryAtomicityMode.UNORDERED);

        intrinsics.registerIntrinsic(cNativeDesc, "element_type_id_of", objArrayTypeIdDesc, elementTypeOf);

        StaticValueIntrinsic addrOf = (builder, owner, name, descriptor, arguments) -> {
            Value value = arguments.get(0);
            if (value instanceof BitCast) {
                value = ((BitCast)value).getInput();
            }
            if (value instanceof Extend) {
                value = ((Extend) value).getInput();
            }
            if (value instanceof Load) {
                Load load = (Load) value;
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

        StaticIntrinsic attachNewThread = (builder, owner, name, descriptor, arguments) -> {
            //java.lang.Thread.nextThreadID
            Value thread = builder.new_(thrDesc);
            // immediately set the thread to be the current thread
            builder.store(builder.pointerHandle(ctxt.getCurrentThreadLocalSymbolLiteral()), builder.valueConvert(thread, voidPtr), MemoryAtomicityMode.NONE);
            // now start initializing
            DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Thread");
            ValidatedTypeDefinition jltVal = jlt.validate();
            // find all the fields
            FieldElement nameFld = jltVal.findField("name");
            FieldElement tidFld = jltVal.findField("tid");
            FieldElement groupFld = jltVal.findField("group");
            FieldElement threadStatusFld = jltVal.findField("threadStatus");
            ValueHandle threadRef = builder.referenceHandle(thread);
            builder.store(builder.instanceFieldOf(threadRef, nameFld), arguments.get(0), MemoryAtomicityMode.NONE);
            builder.store(builder.instanceFieldOf(threadRef, groupFld), arguments.get(1), MemoryAtomicityMode.NONE);
            Value tid = builder.invokeValueStatic(thrDesc, "nextThreadID", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of()), List.of());
            builder.store(builder.instanceFieldOf(threadRef, tidFld), tid, MemoryAtomicityMode.NONE);
            // set thread to be running with JVMTI status for RUNNABLE and ALIVE
            builder.store(builder.instanceFieldOf(threadRef, threadStatusFld), ctxt.getLiteralFactory().literalOf(0x05), MemoryAtomicityMode.NONE);
            return builder.nop();
        };

        intrinsics.registerIntrinsic(cNativeDesc, "attachNewThread", MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            strDesc, tgDesc
        )), attachNewThread);
    }

    static void registerOrgQbiccObjectModelIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        Layout layout = Layout.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();

        ClassTypeDescriptor objModDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/ObjectModel");
        ClassTypeDescriptor typeIdDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$type_id");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor clsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        MethodDescriptor objTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(objDesc));
        MethodDescriptor objIntDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(objDesc));
        MethodDescriptor typeIdTypeIdDesc = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(typeIdDesc));
        MethodDescriptor typeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc));
        MethodDescriptor typeIdTypeIdBooleanDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(typeIdDesc, typeIdDesc));
        MethodDescriptor clsTypeId = MethodDescriptor.synthesize(classContext, typeIdDesc, List.of(clsDesc));
        MethodDescriptor clsInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of(clsDesc));

        StaticValueIntrinsic typeOf = (builder, owner, name, descriptor, arguments) ->
            builder.typeIdOf(builder.referenceHandle(arguments.get(0)));
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "type_id_of", objTypeIdDesc, typeOf);

        FieldElement elementTypeField = layout.getRefArrayElementTypeIdField();
        StaticValueIntrinsic elementTypeOf = (builder, owner, name, descriptor, arguments) -> {
            ValueHandle handle = builder.referenceHandle(builder.bitCast(arguments.get(0), elementTypeField.getEnclosingType().validate().getType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, elementTypeField), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "element_type_id_of", objTypeIdDesc, elementTypeOf);

        FieldElement dimensionsField = layout.getRefArrayDimensionsField();
        StaticValueIntrinsic dimensionsOf = (builder, owner, name, descriptor, arguments) -> {
            ValueHandle handle = builder.referenceHandle(builder.bitCast(arguments.get(0), dimensionsField.getEnclosingType().validate().getType().getReference()));
            return builder.load(builder.instanceFieldOf(handle, dimensionsField), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "dimensions_of", objIntDesc, dimensionsOf);

        StaticValueIntrinsic maxSubclassId = (builder, owner, name, descriptor, arguments) -> {
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), arguments.get(0));
            return builder.load(builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("maxSubTypeId")), MemoryAtomicityMode.UNORDERED);
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "max_subclass_type_id_of", typeIdTypeIdDesc, maxSubclassId);

        StaticValueIntrinsic isObject = (builder, owner, name, descriptor, arguments) -> {
            ValidatedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").validate();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlo.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_java_lang_object", typeIdBooleanDesc, isObject);

        StaticValueIntrinsic isCloneable = (builder, owner, name, descriptor, arguments) -> {
            ValidatedTypeDefinition jlc = classContext.findDefinedType("java/lang/Cloneable").validate();
            return builder.isEq(arguments.get(0), lf.literalOfType(jlc.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_java_lang_cloneable", typeIdBooleanDesc, isCloneable);

        StaticValueIntrinsic isSerializable = (builder, owner, name, descriptor, arguments) -> {
            ValidatedTypeDefinition jis = classContext.findDefinedType("java/io/Serializable").validate();
            return builder.isEq(arguments.get(0), lf.literalOfType(jis.getType()));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_java_io_serializable", typeIdBooleanDesc, isSerializable);

        StaticValueIntrinsic isClass = (builder, owner, name, descriptor, arguments) -> {
            ValidatedTypeDefinition jlo = classContext.findDefinedType("java/lang/Object").validate();
            ValueType refArray = layout.getArrayValidatedTypeDefinition("[ref").getType();
            Value isObj = builder.isEq(arguments.get(0), lf.literalOfType(jlo.getType()));
            Value isAboveRef = builder.isLt(lf.literalOfType(refArray), arguments.get(0));
            Value isNotInterface = builder.isLt(arguments.get(0), lf.literalOf(tables.getFirstInterfaceTypeId()));
            return builder.or(isObj, builder.and(isAboveRef, isNotInterface));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_class", typeIdBooleanDesc, isClass);

        StaticValueIntrinsic isInterface = (builder, owner, name, descriptor, arguments) -> {
            return builder.isLe(lf.literalOf(tables.getFirstInterfaceTypeId()), arguments.get(0));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_interface", typeIdBooleanDesc, isInterface);

        StaticValueIntrinsic isPrimArray = (builder, owner, name, descriptor, arguments) -> {
            ValueType firstPrimArray = layout.getArrayValidatedTypeDefinition("[Z").getType();
            ValueType lastPrimArray = layout.getArrayValidatedTypeDefinition("[D").getType();
            return builder.and(builder.isLe(lf.literalOfType(firstPrimArray), arguments.get(0)),
                builder.isGe(arguments.get(0), lf.literalOfType(lastPrimArray)));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_prim_array", typeIdBooleanDesc, isPrimArray);

        StaticValueIntrinsic isRefArray = (builder, owner, name, descriptor, arguments) -> {
            ValueType refArray = layout.getArrayValidatedTypeDefinition("[ref").getType();
            return builder.isEq(arguments.get(0), lf.literalOfType(refArray));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "is_reference_array", typeIdBooleanDesc, isRefArray);

        StaticValueIntrinsic doesImplement = (builder, owner, name, descriptor, arguments) -> {
            Value objTypeId = arguments.get(0);
            Value interfaceTypeId = arguments.get(1);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(builder.getCurrentElement());
            ValueHandle typeIdStruct = builder.elementOf(builder.globalVariable(typeIdGlobal), objTypeId);
            ValueHandle bits = builder.memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            Value adjustedInterfaceTypeId = builder.sub(interfaceTypeId, lf.literalOf(tables.getFirstInterfaceTypeId()));
            Value implementsIdx = builder.shr(builder.bitCast(adjustedInterfaceTypeId, ctxt.getTypeSystem().getUnsignedInteger32Type()), lf.literalOf(3));
            Value implementsBit = builder.and(adjustedInterfaceTypeId, lf.literalOf(7));
            Value dataByte = builder.load(builder.elementOf(bits, implementsIdx), MemoryAtomicityMode.UNORDERED);
            Value mask = builder.truncate(builder.shl(lf.literalOf(1), implementsBit), ctxt.getTypeSystem().getSignedInteger8Type());
            return builder.isEq(mask, builder.and(mask, dataByte));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "does_implement", typeIdTypeIdBooleanDesc, doesImplement);

        StaticValueIntrinsic getDimFromClass = (builder, owner, name, descriptor, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), layout.getClassDimensionField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_dimensions_from_class", clsInt, getDimFromClass);

        StaticValueIntrinsic getTypeIdFromClass = (builder, owner, name, descriptor, arguments) ->
            builder.load(builder.instanceFieldOf(builder.referenceHandle(arguments.get(0)), layout.getClassTypeIdField()), MemoryAtomicityMode.UNORDERED);
        intrinsics.registerIntrinsic(Phase.LOWER, objModDesc, "get_type_id_from_class", clsTypeId, getTypeIdFromClass);
    }

    static void registerOrgQbiccRuntimeValuesIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // descriptors

        ClassTypeDescriptor valsDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Values");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");

        MethodDescriptor objBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(objDesc));
        MethodDescriptor boolBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.Z));
        MethodDescriptor longBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.J));
        MethodDescriptor intBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.I));
        MethodDescriptor shortBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.S));
        MethodDescriptor byteBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.B));
        MethodDescriptor charBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.C));
        MethodDescriptor floatBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.F));
        MethodDescriptor doubleBoolDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(BaseTypeDescriptor.D));

        MethodDescriptor objObjObjDescriptor = MethodDescriptor.synthesize(classContext, objDesc, Collections.nCopies(2, objDesc));
        MethodDescriptor longLongLongDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, Collections.nCopies(2, BaseTypeDescriptor.J));
        MethodDescriptor intIntIntDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, Collections.nCopies(2, BaseTypeDescriptor.I));

        MethodDescriptor objObjVoidDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, Collections.nCopies(2, objDesc));
        MethodDescriptor longLongVoidDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, Collections.nCopies(2, BaseTypeDescriptor.J));
        MethodDescriptor intIntVoidDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, Collections.nCopies(2, BaseTypeDescriptor.I));

        MethodDescriptor objObjDescriptor = MethodDescriptor.synthesize(classContext, objDesc, List.of(objDesc));
        MethodDescriptor longLongDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J));
        MethodDescriptor intIntDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of(BaseTypeDescriptor.J));

        // isConstant

        StaticValueIntrinsic isConstant = (builder, owner, name, descriptor, arguments) -> literalOf(ctxt, arguments.get(0) instanceof Literal);

        intrinsics.registerIntrinsic(valsDesc, "isConstant", objBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", boolBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", longBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", intBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", shortBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", byteBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", charBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", floatBoolDesc, isConstant);
        intrinsics.registerIntrinsic(valsDesc, "isConstant", doubleBoolDesc, isConstant);

        // isAlways*

        StaticValueIntrinsic isAlwaysTrue = (builder, owner, name, descriptor, arguments) -> literalOf(ctxt, arguments.get(0) instanceof BooleanLiteral && ((BooleanLiteral) arguments.get(0)).booleanValue());
        intrinsics.registerIntrinsic(valsDesc, "isAlwaysTrue", boolBoolDesc, isAlwaysTrue);

        StaticValueIntrinsic isAlwaysFalse = (builder, owner, name, descriptor, arguments) -> literalOf(ctxt, arguments.get(0) instanceof BooleanLiteral && ((BooleanLiteral) arguments.get(0)).booleanValue());
        intrinsics.registerIntrinsic(valsDesc, "isAlwaysFalse", boolBoolDesc, isAlwaysFalse);

        // todo: compareAndSwap*

        // getAndSet*

        class GetAndSetIntrinsic implements StaticValueIntrinsic {
            private final MemoryAtomicityMode mode;

            GetAndSetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            public Value emitIntrinsic(BasicBlockBuilder builder, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return arguments.get(0);
                }
                return builder.getAndSet(target, arguments.get(1), mode);
            }
        }

        StaticValueIntrinsic getAndSetVolatile = new GetAndSetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", objObjObjDescriptor, getAndSetVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", longLongLongDescriptor, getAndSetVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetVolatile", intIntIntDescriptor, getAndSetVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticValueIntrinsic getAndSetRelaxed = new GetAndSetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", objObjObjDescriptor, getAndSetRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", longLongLongDescriptor, getAndSetRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getAndSetRelaxed", intIntIntDescriptor, getAndSetRelaxed);

        // set*

        class SetIntrinsic implements StaticIntrinsic {
            private final MemoryAtomicityMode mode;

            SetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Node emitIntrinsic(BasicBlockBuilder builder, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return builder.nop();
                }
                return builder.store(target, arguments.get(1), mode);
            }
        }

        // todo: determine the real atomicity mode for "relaxed"
        StaticIntrinsic setVolatile = new SetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "setVolatile", objObjVoidDescriptor, setVolatile);
        intrinsics.registerIntrinsic(valsDesc, "setVolatile", intIntVoidDescriptor, setVolatile);
        intrinsics.registerIntrinsic(valsDesc, "setVolatile", longLongVoidDescriptor, setVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticIntrinsic setRelaxed = new SetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "setRelaxed", objObjVoidDescriptor, setRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "setRelaxed", intIntVoidDescriptor, setRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "setRelaxed", longLongVoidDescriptor, setRelaxed);

        // todo: exitConstructorBarrier

        // get*

        class GetIntrinsic implements StaticValueIntrinsic {
            private final MemoryAtomicityMode mode;

            GetIntrinsic(MemoryAtomicityMode mode) {
                this.mode = mode;
            }

            @Override
            public Value emitIntrinsic(BasicBlockBuilder builder, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
                ValueHandle target = getTarget(ctxt, builder, arguments.get(0));
                if (target == null) {
                    return arguments.get(0);
                }
                return builder.load(target, mode);
            }
        }

        StaticValueIntrinsic getVolatile = new GetIntrinsic(MemoryAtomicityMode.VOLATILE);

        intrinsics.registerIntrinsic(valsDesc, "getVolatile", objObjDescriptor, getVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getVolatile", intIntDescriptor, getVolatile);
        intrinsics.registerIntrinsic(valsDesc, "getVolatile", longLongDescriptor, getVolatile);

        // todo: determine the real atomicity mode for "relaxed"
        StaticValueIntrinsic getRelaxed = new GetIntrinsic(MemoryAtomicityMode.MONOTONIC);

        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", objObjDescriptor, getRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", intIntDescriptor, getRelaxed);
        intrinsics.registerIntrinsic(valsDesc, "getRelaxed", objObjDescriptor, getRelaxed);
    }

    static void registerOrgQbiccRuntimeMainIntrinsics(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor mainDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/Main");

        ClassTypeDescriptor tgDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/ThreadGroup");
        MethodDescriptor voidVoidDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of());

        // Construct system thread group
        StaticValueIntrinsic sysThrGrp = (builder, owner, name, descriptor, arguments) ->
            builder.invokeConstructor(builder.new_(tgDesc), tgDesc, voidVoidDesc, List.of());

        MethodDescriptor returnTgDesc = MethodDescriptor.synthesize(classContext, tgDesc, List.of());

        intrinsics.registerIntrinsic(mainDesc, "createSystemThreadGroup", returnTgDesc, sysThrGrp);

    }

    static ValueHandle getTarget(CompilationContext ctxt, BasicBlockBuilder builder, Value input) {
        if (input instanceof Load) {
            Load load = (Load) input;
            ValueHandle target = load.getValueHandle();
            // make sure the target is unambiguous
            if (target instanceof Variable) {
                ValueType valueType = target.getValueType();
                if (valueType instanceof PointerType) {
                    ctxt.error(builder.getLocation(), "Ambiguous target for operation; to target the pointer value, use deref(val); to target the variable use addr_of(val)");
                }
            }
            return target;
        } else {
            ctxt.error(builder.getLocation(), "Cannot determine target of operation");
            return null;
        }
    }

    public static void registerJavaLangMathIntrinsics(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor mathDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Math");
        ClassTypeDescriptor strictDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/StrictMath");

        MethodDescriptor intIntIntDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, Collections.nCopies(2, BaseTypeDescriptor.I));
        MethodDescriptor longLongLongDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, Collections.nCopies(2, BaseTypeDescriptor.J));
        MethodDescriptor floatFloatFloatDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.F, Collections.nCopies(2, BaseTypeDescriptor.F));
        MethodDescriptor doubleDoubleDoubleDescriptor = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.D, Collections.nCopies(2, BaseTypeDescriptor.D));

        StaticValueIntrinsic min = (builder, owner, name, descriptor, arguments) ->
            builder.min(arguments.get(0), arguments.get(1));

        StaticValueIntrinsic max = (builder, owner, name, descriptor, arguments) ->
            builder.max(arguments.get(0), arguments.get(1));

        intrinsics.registerIntrinsic(mathDesc, "min", intIntIntDescriptor, min);
        intrinsics.registerIntrinsic(mathDesc, "min", longLongLongDescriptor, min);
        intrinsics.registerIntrinsic(mathDesc, "min", floatFloatFloatDescriptor, min);
        intrinsics.registerIntrinsic(mathDesc, "min", doubleDoubleDoubleDescriptor, min);

        intrinsics.registerIntrinsic(mathDesc, "max", intIntIntDescriptor, max);
        intrinsics.registerIntrinsic(mathDesc, "max", longLongLongDescriptor, max);
        intrinsics.registerIntrinsic(mathDesc, "max", floatFloatFloatDescriptor, max);
        intrinsics.registerIntrinsic(mathDesc, "max", doubleDoubleDoubleDescriptor, max);

        intrinsics.registerIntrinsic(strictDesc, "min", intIntIntDescriptor, min);
        intrinsics.registerIntrinsic(strictDesc, "min", longLongLongDescriptor, min);
        intrinsics.registerIntrinsic(strictDesc, "min", floatFloatFloatDescriptor, min);
        intrinsics.registerIntrinsic(strictDesc, "min", doubleDoubleDoubleDescriptor, min);

        intrinsics.registerIntrinsic(strictDesc, "max", intIntIntDescriptor, max);
        intrinsics.registerIntrinsic(strictDesc, "max", longLongLongDescriptor, max);
        intrinsics.registerIntrinsic(strictDesc, "max", floatFloatFloatDescriptor, max);
        intrinsics.registerIntrinsic(strictDesc, "max", doubleDoubleDoubleDescriptor, max);
    }

    /* Temporary workaround for casting in VMHelpers */
    static void registerOrgQbiccRuntimePosixPthreadCastPtr(final CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor cnativeDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");

        /* intrinsic implementation */
        StaticValueIntrinsic castPtr = (builder, owner, name, descriptor, arguments) -> {
            Value castObject = arguments.get(0);
            Value typeValue = arguments.get(1);
            if (typeValue instanceof ClassOf) {
                ClassOf typeClassOf = (ClassOf)typeValue;
                Value typeInput = typeClassOf.getInput();
                if (typeInput instanceof TypeLiteral) {
                    ValueType type = ((TypeLiteral) typeInput).getValue();
                    PointerType newPointerType = type.getPointer();
                    return builder.bitCast(castObject, newPointerType);
                }
            }
            ctxt.error(builder.getLocation(), "Invalid pointer type.");
            return castObject;
        };

        intrinsics.registerIntrinsic(cnativeDesc, "castPtr", MethodDescriptor.synthesize(classContext, ptrDesc, List.of(ptrDesc, classDesc)), castPtr);
    }
}
