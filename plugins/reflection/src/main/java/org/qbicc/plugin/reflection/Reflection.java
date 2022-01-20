package org.qbicc.plugin.reflection;

import static org.qbicc.graph.atomic.AccessModes.GlobalRelease;
import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.VmThrowableClass;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.classfile.ConstantPool;
import org.qbicc.type.definition.element.AnnotatedElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.methodhandle.MethodHandleKind;

/**
 * Hub for reflection support.
 */
public final class Reflection {
    private static final int IS_METHOD = 1 << 16;
    private static final int IS_CONSTRUCTOR = 1 << 17;
    private static final int IS_FIELD = 1 << 18;
    private static final int IS_TYPE = 1 << 19;
    private static final int CALLER_SENSITIVE = 1 << 20;
    private static final int TRUSTED_FINAL = 1 << 21;

    private static final int KIND_SHIFT = 24;

    private static final int KIND_GET_FIELD = MethodHandleKind.GET_FIELD.getId();
    private static final int KIND_GET_STATIC = MethodHandleKind.GET_STATIC.getId();
    private static final int KIND_PUT_FIELD = MethodHandleKind.PUT_FIELD.getId();
    private static final int KIND_PUT_STATIC = MethodHandleKind.PUT_STATIC.getId();
    private static final int KIND_INVOKE_VIRTUAL = MethodHandleKind.INVOKE_VIRTUAL.getId();
    private static final int KIND_INVOKE_STATIC = MethodHandleKind.INVOKE_STATIC.getId();
    private static final int KIND_INVOKE_SPECIAL = MethodHandleKind.INVOKE_SPECIAL.getId();
    private static final int KIND_NEW_INVOKE_SPECIAL = MethodHandleKind.NEW_INVOKE_SPECIAL.getId();
    private static final int KIND_INVOKE_INTERFACE = MethodHandleKind.INVOKE_INTERFACE.getId();

    private static final int KIND_MASK = 0xf;

    private static final AttachmentKey<Reflection> KEY = new AttachmentKey<>();

    private static final byte[] NO_BYTES = new byte[0];

    private final CompilationContext ctxt;

    private final Map<VmClass, VmObject> cpMap = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredFields = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredPublicFields = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredMethods = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredPublicMethods = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredConstructors = new ConcurrentHashMap<>();
    private final Map<VmClass, VmReferenceArray> declaredPublicConstructors = new ConcurrentHashMap<>();
    private final Map<VmObject, ConstantPool> cpObjs = new ConcurrentHashMap<>();
    private final Map<AnnotatedElement, VmArray> annotatedElements = new ConcurrentHashMap<>();
    private final Map<Element, VmObject> reflectionObjects = new ConcurrentHashMap<>();
    private final Map<LoadedTypeDefinition, VmArray> annotatedTypes = new ConcurrentHashMap<>();
    private final VmArray noBytes;
    private final Vm vm;
    private final VmClass cpClass;
    private final VmClass classClass;
    private final VmClass fieldClass;
    private final VmClass methodClass;
    private final VmClass constructorClass;
    private final VmClass rmnClass;
    private final VmClass memberNameClass;
    private final VmThrowableClass linkageErrorClass;
    private final VmThrowableClass invocationTargetExceptionClass;
    private final VmClass byteClass;
    private final VmClass shortClass;
    private final VmClass integerClass;
    private final VmClass longClass;
    private final VmClass characterClass;
    private final VmClass floatClass;
    private final VmClass doubleClass;
    private final VmClass booleanClass;
    private final ConstructorElement cpCtor;
    private final ConstructorElement fieldCtor;
    private final ConstructorElement methodCtor;
    private final ConstructorElement ctorCtor;
    private final ConstructorElement rmnCtor;
    // byte refKind, Class<?> defClass, String name, Object(Class|MethodType) type
    private final ConstructorElement memberName4Ctor;

    // reflection fields
    // MemberName
    private final FieldElement memberNameClazzField; // Class
    private final FieldElement memberNameNameField; // String
    private final FieldElement memberNameTypeField; // Object
    private final FieldElement memberNameFlagsField; // int
    private final FieldElement memberNameMethodField; // ResolvedMethodName
    private final FieldElement memberNameIndexField; // int (injected)
    // Field
    private final FieldElement fieldClazzField; // Class
    private final FieldElement fieldSlotField; // int
    private final FieldElement fieldNameField; // String
    private final FieldElement fieldTypeField; // Class
    // Method
    private final FieldElement methodClazzField; // Class
    private final FieldElement methodSlotField; // int
    private final FieldElement methodNameField; // String
    private final FieldElement methodReturnTypeField; // Class
    private final FieldElement methodParameterTypesField; // Class[]
    // Method
    private final FieldElement ctorClazzField; // Class
    private final FieldElement ctorSlotField; // int
    private final FieldElement ctorParameterTypesField; // Class[]
    // ResolvedMethodName (injected fields)
    private final FieldElement rmnIndexField; // int
    private final FieldElement rmnClazzField; // Class
    // MethodType
    private final FieldElement methodTypePTypesField; // Class[]
    private final FieldElement methodTypeRTypeField; // Class

    // box type fields
    private final FieldElement byteValueField;
    private final FieldElement shortValueField;
    private final FieldElement integerValueField;
    private final FieldElement longValueField;
    private final FieldElement characterValueField;
    private final FieldElement floatValueField;
    private final FieldElement doubleValueField;
    private final FieldElement booleanValueField;

    private Reflection(CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // Field injections (*early*)
        Patcher patcher = Patcher.get(ctxt);

        patcher.addField(classContext, "java/lang/invoke/MemberName", "index", BaseTypeDescriptor.I, this::resolveIndexField, 0, 0);

        patcher.addField(classContext, "java/lang/invoke/ResolvedMethodName", "index", BaseTypeDescriptor.I, this::resolveIndexField, 0, 0);
        patcher.addField(classContext, "java/lang/invoke/ResolvedMethodName", "clazz", ClassTypeDescriptor.synthesize(classContext, "java/lang/Class"), this::resolveClazzField, 0, 0);

        // VM
        vm = ctxt.getVm();
        noBytes = vm.newByteArray(NO_BYTES);
        // ConstantPool
        LoadedTypeDefinition cpDef = classContext.findDefinedType("jdk/internal/reflect/ConstantPool").load();
        cpClass = cpDef.getVmClass();
        cpCtor = cpDef.getConstructor(0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getIntAt0")), this::getIntAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getLongAt0")), this::getLongAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getFloatAt0")), this::getFloatAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getDoubleAt0")), this::getDoubleAt0);
        vm.registerInvokable(cpDef.requireSingleMethod(me -> me.nameEquals("getUTF8At0")), this::getUTF8At0);
        // Class
        LoadedTypeDefinition classDef = classContext.findDefinedType("java/lang/Class").load();
        classClass = classDef.getVmClass();
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getRawAnnotations")), this::getClassRawAnnotations);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getDeclaredFields0")), this::getClassDeclaredFields0);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getDeclaredMethods0")), this::getClassDeclaredMethods0);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getDeclaredConstructors0")), this::getClassDeclaredConstructors0);
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getSimpleBinaryName")), (thread, target, args) -> {
            if (target instanceof VmPrimitiveClass) {
                return null;
            }
            NestedClassElement enc = ((VmClass) target).getTypeDefinition().getEnclosingNestedClass();
            return enc == null ? null : vm.intern(enc.getName());
        });
        vm.registerInvokable(classDef.requireSingleMethod(me -> me.nameEquals("getConstantPool")), (thread, target, args) -> {
            // force the object to be created
            getConstantPoolForClass((VmClass) target);
            // return the CP object
            return cpObjs.get(target);
        });
        LoadedTypeDefinition fieldDef = classContext.findDefinedType("java/lang/reflect/Field").load();
        fieldClass = fieldDef.getVmClass();
        fieldCtor = fieldDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 8);
        LoadedTypeDefinition methodDef = classContext.findDefinedType("java/lang/reflect/Method").load();
        methodClass = methodDef.getVmClass();
        methodCtor = methodDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 11);
        LoadedTypeDefinition ctorDef = classContext.findDefinedType("java/lang/reflect/Constructor").load();
        constructorClass = ctorDef.getVmClass();
        ctorCtor = ctorDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 8);
        LoadedTypeDefinition mhnDef = classContext.findDefinedType("java/lang/invoke/MethodHandleNatives").load();
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("init")), this::methodHandleNativesInit);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("resolve")), this::methodHandleNativesResolve);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("objectFieldOffset")), this::methodHandleNativesObjectFieldOffset);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("staticFieldBase")), this::methodHandleNativesStaticFieldBase);
        vm.registerInvokable(mhnDef.requireSingleMethod(me -> me.nameEquals("staticFieldOffset")), this::methodHandleNativesStaticFieldOffset);
        LoadedTypeDefinition nativeCtorAccImplDef = classContext.findDefinedType("jdk/internal/reflect/NativeConstructorAccessorImpl").load();
        vm.registerInvokable(nativeCtorAccImplDef.requireSingleMethod(me -> me.nameEquals("newInstance0")), this::nativeConstructorAccessorImplNewInstance0);
        LoadedTypeDefinition nativeMethodAccImplDef = classContext.findDefinedType("jdk/internal/reflect/NativeMethodAccessorImpl").load();
        vm.registerInvokable(nativeMethodAccImplDef.requireSingleMethod(me -> me.nameEquals("invoke0")), this::nativeMethodAccessorImplInvoke0);
        // MemberName
        LoadedTypeDefinition memberNameDef = classContext.findDefinedType("java/lang/invoke/MemberName").load();
        memberNameClazzField = memberNameDef.findField("clazz");
        memberNameNameField = memberNameDef.findField("name");
        memberNameTypeField = memberNameDef.findField("type");
        memberNameFlagsField = memberNameDef.findField("flags");
        memberNameMethodField = memberNameDef.findField("method");
        memberNameIndexField = memberNameDef.findField("index");
        MethodDescriptor memberName4Desc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(
            BaseTypeDescriptor.B,
            classDef.getDescriptor(),
            ClassTypeDescriptor.synthesize(classContext, "java/lang/String"),
            ClassTypeDescriptor.synthesize(classContext, "java/lang/Object")
        ));
        memberNameClass = memberNameDef.getVmClass();
        memberName4Ctor = memberNameDef.resolveConstructorElement(memberName4Desc);
        // Field
        fieldClazzField = fieldDef.findField("clazz");
        fieldSlotField = fieldDef.findField("slot");
        fieldNameField = fieldDef.findField("name");
        fieldTypeField = fieldDef.findField("type");
        // Method
        methodClazzField = methodDef.findField("clazz");
        methodSlotField = methodDef.findField("slot");
        methodNameField = methodDef.findField("name");
        methodReturnTypeField = methodDef.findField("returnType");
        methodParameterTypesField = methodDef.findField("parameterTypes");
        // Constructor
        ctorClazzField = ctorDef.findField("clazz");
        ctorSlotField = ctorDef.findField("slot");
        ctorParameterTypesField = ctorDef.findField("parameterTypes");

        // ResolvedMethodName
        LoadedTypeDefinition rmnDef = classContext.findDefinedType("java/lang/invoke/ResolvedMethodName").load();
        rmnCtor = rmnDef.getConstructor(0);
        rmnClass = rmnDef.getVmClass();
        rmnIndexField = rmnDef.findField("index");
        rmnClazzField = rmnDef.findField("clazz");

        // Exceptions & errors
        LoadedTypeDefinition leDef = classContext.findDefinedType("java/lang/LinkageError").load();
        linkageErrorClass = (VmThrowableClass) leDef.getVmClass();
        LoadedTypeDefinition iteDef = classContext.findDefinedType("java/lang/reflect/InvocationTargetException").load();
        invocationTargetExceptionClass = (VmThrowableClass) iteDef.getVmClass();

        // MethodType
        LoadedTypeDefinition mtDef = classContext.findDefinedType("java/lang/invoke/MethodType").load();
        methodTypeRTypeField = mtDef.findField("rtype");
        methodTypePTypesField = mtDef.findField("ptypes");

        // box types
        LoadedTypeDefinition byteDef = classContext.findDefinedType("java/lang/Byte").load();
        byteClass = byteDef.getVmClass();
        byteValueField = byteDef.findField("value");
        LoadedTypeDefinition shortDef = classContext.findDefinedType("java/lang/Short").load();
        shortClass = shortDef.getVmClass();
        shortValueField = shortDef.findField("value");
        LoadedTypeDefinition integerDef = classContext.findDefinedType("java/lang/Integer").load();
        integerClass = integerDef.getVmClass();
        integerValueField = integerDef.findField("value");
        LoadedTypeDefinition longDef = classContext.findDefinedType("java/lang/Long").load();
        longClass = longDef.getVmClass();
        longValueField = longDef.findField("value");
        LoadedTypeDefinition characterDef = classContext.findDefinedType("java/lang/Character").load();
        characterClass = characterDef.getVmClass();
        characterValueField = characterDef.findField("value");
        LoadedTypeDefinition floatDef = classContext.findDefinedType("java/lang/Float").load();
        floatClass = floatDef.getVmClass();
        floatValueField = floatDef.findField("value");
        LoadedTypeDefinition doubleDef = classContext.findDefinedType("java/lang/Double").load();
        doubleClass = doubleDef.getVmClass();
        doubleValueField = doubleDef.findField("value");
        LoadedTypeDefinition booleanDef = classContext.findDefinedType("java/lang/Boolean").load();
        booleanClass = booleanDef.getVmClass();
        booleanValueField = booleanDef.findField("value");
    }

    public static Reflection get(CompilationContext ctxt) {
        Reflection instance = ctxt.getAttachment(KEY);
        if (instance == null) {
            instance = new Reflection(ctxt);
            Reflection appearing = ctxt.putAttachmentIfAbsent(KEY, instance);
            if (appearing != null) {
                instance = appearing;
            }
        }
        return instance;
    }

    ConstantPool getConstantPoolForClass(VmClass vmClass) {
        return cpObjs.computeIfAbsent(cpMap.computeIfAbsent(vmClass, this::makePoolObj), Reflection::makePool);
    }

    VmArray getAnnotations(AnnotatedElement element) {
        VmArray bytes = annotatedElements.get(element);
        if (bytes != null) {
            return bytes;
        }
        List<Annotation> annotations = element.getVisibleAnnotations();
        if (annotations.isEmpty()) {
            annotatedElements.putIfAbsent(element, noBytes);
            return noBytes;
        }
        VmClass vmClass = element.getEnclosingType().load().getVmClass();
        ConstantPool constantPool = getConstantPoolForClass(vmClass);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        synchronized (constantPool) {
            for (Annotation annotation : annotations) {
                annotation.deparseTo(os, constantPool);
            }
        }
        VmArray appearing = annotatedElements.putIfAbsent(element, vm.newByteArray(os.toByteArray()));
        if (appearing != null) {
            bytes = appearing;
        }
        return bytes;
    }

    private VmObject makePoolObj(final Object ignored) {
        return vm.newInstance(cpClass, cpCtor, List.of());
    }

    private static ConstantPool makePool(final Object ignored) {
        return new ConstantPool();
    }

    private Object getIntAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Integer.valueOf(constantPool.getIntConstant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getLongAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Long.valueOf(constantPool.getLongConstant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getFloatAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Float.valueOf(Float.intBitsToFloat(constantPool.getIntConstant(((Number) objects.get(0)).intValue())));
        }
    }

    private Object getDoubleAt0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return Long.valueOf(constantPool.getLongConstant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getUTF8At0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        ConstantPool constantPool = cpObjs.get(vmObject);
        assert constantPool != null; // the method would not be reachable otherwise
        synchronized (constantPool) {
            return vm.intern(constantPool.getUtf8Constant(((Number) objects.get(0)).intValue()));
        }
    }

    private Object getClassRawAnnotations(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        VmClass vmClass = (VmClass) vmObject;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        VmArray bytes = annotatedTypes.get(def);
        if (bytes != null) {
            return bytes;
        }
        List<Annotation> annotations = def.getVisibleAnnotations();
        if (annotations.isEmpty()) {
            annotatedTypes.putIfAbsent(def, noBytes);
            return noBytes;
        }
        ConstantPool constantPool = getConstantPoolForClass(vmClass);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        synchronized (constantPool) {
            for (Annotation annotation : annotations) {
                annotation.deparseTo(os, constantPool);
            }
        }
        VmArray appearing = annotatedTypes.putIfAbsent(def, vm.newByteArray(os.toByteArray()));
        if (appearing != null) {
            bytes = appearing;
        }
        return bytes;
    }

    private Object getClassDeclaredFields0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        return getClassDeclaredFields((VmClass) vmObject, ((Boolean) objects.get(0)).booleanValue());
    }

    private VmObject getField(FieldElement field) {
        VmObject vmObject = reflectionObjects.get(field);
        if (vmObject != null) {
            return vmObject;
        }
        // field might get mutated; note it here so we don't cache it unduly
        if (! field.isStatic()) {
            field.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        }
        VmClass declaringClass = field.getEnclosingType().load().getVmClass();
        vmObject = vm.newInstance(fieldClass, fieldCtor, Arrays.asList(
            declaringClass,
            vm.intern(field.getName()),
            vm.getClassForDescriptor(declaringClass.getClassLoader(), field.getTypeDescriptor()),
            Integer.valueOf(field.getModifiers() & 0x1fff),
            Boolean.FALSE,
            Integer.valueOf(field.getIndex()),
            vm.intern(field.getTypeSignature().toString()),
            getAnnotations(field)
        ));
        VmObject appearing = reflectionObjects.putIfAbsent(field, vmObject);
        return appearing != null ? appearing : vmObject;
    }

    private VmReferenceArray getClassDeclaredFields(final VmClass vmClass, final boolean publicOnly) {
        VmReferenceArray result;
        Map<VmClass, VmReferenceArray> map = publicOnly ? this.declaredPublicFields : this.declaredFields;
        result = map.get(vmClass);
        if (result != null) {
            return result;
        }
        int total = 0;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        for (int i = 0; i < def.getFieldCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            if (! publicOnly || def.getField(i).isPublic()) {
                total += 1;
            }
        }
        VmReferenceArray pubFields = vm.newArrayOf(fieldClass, total);
        int pubIdx = 0;
        for (int i = 0; i < def.getFieldCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            FieldElement field = def.getField(i);
            if (! publicOnly || field.isPublic()) {
                pubFields.getMemory().storeRef(pubFields.getArrayElementOffset(pubIdx++), getField(field), SinglePlain);
            }
        }
        VmReferenceArray appearing = map.putIfAbsent(vmClass, pubFields);
        return appearing != null ? appearing : pubFields;
    }

    private Object getClassDeclaredMethods0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        return getClassDeclaredMethods((VmClass) vmObject, ((Boolean) objects.get(0)).booleanValue());
    }

    private VmObject getMethod(MethodElement method) {
        VmObject vmObject = reflectionObjects.get(method);
        if (vmObject != null) {
            return vmObject;
        }
        VmClass declaringClass = method.getEnclosingType().load().getVmClass();
        VmClassLoader classLoader = declaringClass.getClassLoader();
        MethodDescriptor desc = method.getDescriptor();
        List<TypeDescriptor> paramTypes = desc.getParameterTypes();
        VmArray paramTypesVal = vm.newArrayOf(classClass, paramTypes.size());
        for (int j = 0; j < paramTypes.size(); j ++) {
            paramTypesVal.getMemory().storeRef(paramTypesVal.getArrayElementOffset(j), vm.getClassForDescriptor(classLoader, paramTypes.get(j)), SinglePlain);
        }
        // annotation default
        VmArray dv;
        AnnotationValue defaultValue = method.getDefaultValue();
        if (defaultValue == null) {
            dv = noBytes;
        } else {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ConstantPool cp = getConstantPoolForClass(declaringClass);
            synchronized (cp) {
                defaultValue.deparseValueTo(os, cp);
            }
            dv = vm.newByteArray(os.toByteArray());
        }
        vmObject = vm.newInstance(methodClass, methodCtor, Arrays.asList(
            declaringClass,
            vm.intern(method.getName()),
            paramTypesVal,
            vm.getClassForDescriptor(classLoader, method.getDescriptor().getReturnType()),
            // TODO: checked exceptions
            vm.newArrayOf(classClass.getArrayClass(), 0),
            Integer.valueOf(method.getModifiers() & 0x1fff),
            Integer.valueOf(method.getIndex()),
            vm.intern(method.getSignature().toString()),
            getAnnotations(method),
            // TODO: param annotations
            noBytes,
            dv
        ));
        VmObject appearing = reflectionObjects.putIfAbsent(method, vmObject);
        return appearing != null ? appearing : vmObject;
    }

    private VmReferenceArray getClassDeclaredMethods(final VmClass vmClass, final boolean publicOnly) {
        VmReferenceArray result;
        Map<VmClass, VmReferenceArray> map = publicOnly ? this.declaredPublicMethods : this.declaredMethods;
        result = map.get(vmClass);
        if (result != null) {
            return result;
        }
        int total = 0;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        for (int i = 0; i < def.getMethodCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            if (! publicOnly || def.getMethod(i).isPublic()) {
                total += 1;
            }
        }
        VmReferenceArray pubMethods = vm.newArrayOf(methodClass, total);
        int pubIdx = 0;
        for (int i = 0; i < def.getMethodCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            MethodElement method = def.getMethod(i);
            if (! publicOnly || method.isPublic()) {
                pubMethods.getMemory().storeRef(pubMethods.getArrayElementOffset(pubIdx++), getMethod(method), SinglePlain);
            }
        }
        VmReferenceArray appearing = map.putIfAbsent(vmClass, pubMethods);
        return appearing != null ? appearing : pubMethods;
    }

    private Object getClassDeclaredConstructors0(final VmThread vmThread, final VmObject vmObject, final List<Object> objects) {
        return getClassDeclaredConstructors((VmClass) vmObject, ((Boolean) objects.get(0)).booleanValue());
    }

    private VmObject getConstructor(ConstructorElement constructor) {
        VmObject vmObject = reflectionObjects.get(constructor);
        if (vmObject != null) {
            return vmObject;
        }
        VmClass declaringClass = constructor.getEnclosingType().load().getVmClass();
        VmClassLoader classLoader = declaringClass.getClassLoader();
        MethodDescriptor desc = constructor.getDescriptor();
        List<TypeDescriptor> paramTypes = desc.getParameterTypes();
        VmArray paramTypesVal = vm.newArrayOf(constructorClass.getArrayClass(), paramTypes.size());
        for (int j = 0; j < paramTypes.size(); j ++) {
            paramTypesVal.getMemory().storeRef(paramTypesVal.getArrayElementOffset(j), vm.getClassForDescriptor(classLoader, paramTypes.get(j)), SinglePlain);
        }
        vmObject = vm.newInstance(constructorClass, ctorCtor, Arrays.asList(
            declaringClass,
            paramTypesVal,
            // TODO: checked exceptions
            vm.newArrayOf(classClass.getArrayClass(), 0),
            Integer.valueOf(constructor.getModifiers() & 0x1fff),
            Integer.valueOf(constructor.getIndex()),
            vm.intern(constructor.getSignature().toString()),
            getAnnotations(constructor),
            // TODO: param annotations
            noBytes
        ));
        VmObject appearing = reflectionObjects.putIfAbsent(constructor, vmObject);
        return appearing != null ? appearing : vmObject;
    }

    private VmReferenceArray getClassDeclaredConstructors(final VmClass vmClass, final boolean publicOnly) {
        VmReferenceArray result;
        Map<VmClass, VmReferenceArray> map = publicOnly ? this.declaredPublicConstructors : this.declaredConstructors;
        result = map.get(vmClass);
        if (result != null) {
            return result;
        }
        int total = 0;
        LoadedTypeDefinition def = vmClass.getTypeDefinition();
        for (int i = 0; i < def.getConstructorCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            if (! publicOnly || def.getConstructor(i).isPublic()) {
                total += 1;
            }
        }
        VmReferenceArray pubConstructors = vm.newArrayOf(constructorClass, total);
        int pubIdx = 0;
        for (int i = 0; i < def.getConstructorCount(); i++) {
            if (def.hasAllModifiersOf(ClassFile.I_ACC_NO_REFLECT)) {
                continue;
            }
            ConstructorElement constructor = def.getConstructor(i);
            if (! publicOnly || constructor.isPublic()) {
                pubConstructors.getMemory().storeRef(pubConstructors.getArrayElementOffset(pubIdx++), getConstructor(constructor), SinglePlain);
            }
        }
        VmReferenceArray appearing = map.putIfAbsent(vmClass, pubConstructors);
        return appearing != null ? appearing : pubConstructors;
    }

    private Object methodHandleNativesInit(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject self = (VmObject) args.get(0);
        VmObject target = (VmObject) args.get(1);
        VmClass targetClass = target.getVmClass();
        // target may be a reflection method, field, or constructor
        LoadedTypeDefinition targetClassDef = targetClass.getTypeDefinition();
        String targetClassIntName = targetClassDef.getInternalName();
        if (targetClassIntName.equals("java/lang/reflect/Field")) {
            initMethodHandleField(self, target, false);
            return null;
        }
        if (targetClassIntName.equals("java/lang/reflect/Method")) {
            initMethodHandleMethod(self, target);
            return null;
        }
        if (targetClassIntName.equals("java/lang/reflect/Constructor")) {
            initMethodHandleCtor(self, target);
            return null;
        }
        throw new IllegalStateException("Unknown reflection object kind");
    }

    private void initMethodHandleMethod(final VmObject memberName, final VmObject methodTarget) {
        // First read values from the Method object

        // get the method's enclosing class
        VmClass clazzVal = (VmClass) methodTarget.getMemory().loadRef(methodTarget.indexOf(methodClazzField), SinglePlain);
        // get the method index
        int index = methodTarget.getMemory().load32(methodTarget.indexOf(methodSlotField), SinglePlain);

        // find the referenced method
        MethodElement refMethod = clazzVal.getTypeDefinition().getMethod(index);
        int flags = refMethod.getModifiers() & 0x1fff;

        // TODO: hard-coded only when called from `init`; add support for other uses
        flags |= IS_METHOD;
        if (refMethod.isStatic()) {
            flags |= KIND_INVOKE_STATIC << KIND_SHIFT;
        } else {
            flags |= KIND_INVOKE_SPECIAL << KIND_SHIFT;
        }
        if (false /* refMethod.isCallerSensitive() */) {
            flags |= CALLER_SENSITIVE;
        }

        // Now initialize the corresponding MemberName fields

        // Create a ResolvedMethodName for the resolved method
        // todo: switch to resolved method once we are resolving
        MethodElement resolved = refMethod;

        VmObject rmn = vm.newInstance(rmnClass, rmnCtor, List.of());

        rmn.getMemory().storeRef(rmn.indexOf(rmnClazzField), resolved.getEnclosingType().load().getVmClass(), SinglePlain);
        rmn.getMemory().store32(rmn.indexOf(rmnIndexField), resolved.getIndex(), SinglePlain);

        // set the member name flags
        memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), flags, SinglePlain);
        // set the member name's field holder
        memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), clazzVal, SinglePlain);
        // set the member name's method
        memberName.getMemory().storeRef(memberName.indexOf(memberNameMethodField), rmn, SinglePlain);
        // set the member name index
        memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), refMethod.getIndex(), SinglePlain);
        // all done
        return;
    }

    private void initMethodHandleCtor(final VmObject memberName, final VmObject methodTarget) {
        // First read values from the Constructor object

        // get the ctor's enclosing class
        VmClass clazzVal = (VmClass) methodTarget.getMemory().loadRef(methodTarget.indexOf(ctorClazzField), SinglePlain);
        // get the ctor index
        int index = methodTarget.getMemory().load32(methodTarget.indexOf(ctorSlotField), SinglePlain);

        // find the referenced ctor
        ConstructorElement refCtor = clazzVal.getTypeDefinition().getConstructor(index);
        int flags = refCtor.getModifiers() & 0x1fff;

        flags |= IS_CONSTRUCTOR | KIND_INVOKE_SPECIAL << KIND_SHIFT;

        // Now initialize the corresponding MemberName fields

        // Create a ResolvedMethodName for the ctor
        VmObject rmn = vm.newInstance(rmnClass, rmnCtor, List.of());

        rmn.getMemory().storeRef(rmn.indexOf(rmnClazzField), refCtor.getEnclosingType().load().getVmClass(), SinglePlain);
        rmn.getMemory().store32(rmn.indexOf(rmnIndexField), refCtor.getIndex(), SinglePlain);

        // set the member name flags
        memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), flags, SinglePlain);
        // set the member name's field holder
        memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), clazzVal, SinglePlain);
        // set the member name's "method"
        memberName.getMemory().storeRef(memberName.indexOf(memberNameMethodField), rmn, SinglePlain);
        // set the member name index
        memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), refCtor.getIndex(), SinglePlain);
        // all done
        return;
    }

    private void initMethodHandleField(final VmObject memberName, final VmObject fieldTarget, boolean isSetter) {
        // First read values from the Field object

        // get the field's enclosing class
        VmClass clazzVal = (VmClass) fieldTarget.getMemory().loadRef(fieldTarget.indexOf(fieldClazzField), SinglePlain);
        // get the field index
        int index = fieldTarget.getMemory().load32(fieldTarget.indexOf(fieldSlotField), SinglePlain);
        // get the field name
        VmString nameVal = (VmString) fieldTarget.getMemory().loadRef(fieldTarget.indexOf(fieldNameField), SinglePlain);
        // get the field type class
        VmClass fieldClazzVal = (VmClass) fieldTarget.getMemory().loadRef(fieldTarget.indexOf(fieldTypeField), SinglePlain);

        // find the referenced field
        FieldElement refField = clazzVal.getTypeDefinition().getField(index);
        int flags = refField.getModifiers() & 0x1fff;

        // set up flags
        flags |= IS_FIELD;
        if (refField.isStatic()) {
            if (isSetter) {
                flags |= KIND_PUT_STATIC << KIND_SHIFT;
            } else {
                flags |= KIND_GET_STATIC << KIND_SHIFT;
            }
        } else {
            if (isSetter) {
                flags |= KIND_PUT_FIELD << KIND_SHIFT;
            } else {
                flags |= KIND_GET_FIELD << KIND_SHIFT;
            }
        }
        if (refField.isReallyFinal()) {
            flags |= TRUSTED_FINAL;
        }

        // Now initialize the corresponding MemberName fields

        // set the member name flags
        memberName.getMemory().store32(memberName.indexOf(memberNameFlagsField), flags, SinglePlain);
        // set the member name's field holder
        memberName.getMemory().storeRef(memberName.indexOf(memberNameClazzField), clazzVal, SinglePlain);
        // set the member name's name
        memberName.getMemory().storeRef(memberName.indexOf(memberNameNameField), nameVal, SinglePlain);
        // set the member name's type to the field type
        memberName.getMemory().storeRef(memberName.indexOf(memberNameTypeField), fieldClazzVal, SinglePlain);
        // set the member name index
        memberName.getMemory().store32(memberName.indexOf(memberNameIndexField), refField.getIndex(), SinglePlain);
        // all done
        return;
    }

    private Object methodHandleNativesResolve(final VmThread thread, final VmObject ignored, final List<Object> args) {
        Vm vm = thread.getVM();
        VmObject memberName = (VmObject) args.get(0);
        VmClass caller = (VmClass) args.get(1);
        boolean speculativeResolve = (((Number) args.get(2)).intValue() & 1) != 0;
        VmClass clazz = (VmClass) memberName.getMemory().loadRef(memberName.indexOf(memberNameClazzField), SinglePlain);
        VmString name = (VmString) memberName.getMemory().loadRef(memberName.indexOf(memberNameNameField), SinglePlain);
        VmObject type = memberName.getMemory().loadRef(memberName.indexOf(memberNameTypeField), SinglePlain);
        int flags = memberName.getMemory().load32(memberName.indexOf(memberNameFlagsField), SinglePlain);
        int kind = (flags >> KIND_SHIFT) & KIND_MASK;
        if (clazz == null || name == null || type == null) {
            throw new Thrown(linkageErrorClass.newInstance("Null name or class"));
        }
        ClassContext classContext = clazz.getTypeDefinition().getContext();
        // determine what kind of thing we're resolving
        if ((flags & IS_FIELD) != 0) {
            // find a field with the given name
            FieldElement resolved = clazz.getTypeDefinition().findField(name.getContent());
            if (resolved == null && ! speculativeResolve) {
                throw new Thrown(linkageErrorClass.newInstance("No such field: " + clazz.getName() + "#" + name.getContent()));
            }
            if (resolved == null) {
                return null;
            }
            VmClass fieldTypeClass = vm.getClassForDescriptor(clazz.getClassLoader(), resolved.getTypeDescriptor());
            // create a member name
            VmObject result = vm.newInstance(memberNameClass, memberName4Ctor, List.of(
                Byte.valueOf((byte) kind),
                resolved.getEnclosingType().load().getVmClass(),
                vm.intern(resolved.getName()),
                fieldTypeClass
            ));
            result.getMemory().store32(memberNameClass.indexOf(memberNameIndexField), resolved.getIndex(), GlobalRelease);
            return result;
        } else if ((flags & IS_TYPE) != 0) {
            throw new Thrown(linkageErrorClass.newInstance("Not sure what to do for resolving a type"));
        } else {
            // some kind of exec element
            MethodDescriptor desc = createFromMethodType(classContext, type);
            if (((flags & IS_CONSTRUCTOR) != 0)) {
                int idx = clazz.getTypeDefinition().findConstructorIndex(desc);
                if (idx == -1) {
                    if (! speculativeResolve) {
                        throw new Thrown(linkageErrorClass.newInstance("No such constructor: " + name.getContent() + ":" + desc.toString()));
                    }
                    return null;
                }
                if (kind != KIND_NEW_INVOKE_SPECIAL) {
                    throw new Thrown(linkageErrorClass.newInstance("Unknown handle kind"));
                }
                ConstructorElement resolved = clazz.getTypeDefinition().getConstructor(idx);
                // create a member name
                VmObject result = vm.newInstance(memberNameClass, memberName4Ctor, List.of(
                    Byte.valueOf((byte) kind),
                    resolved.getEnclosingType().load().getVmClass(),
                    vm.intern("<init>"),
                    type
                ));
                result.getMemory().store32(memberNameClass.indexOf(memberNameIndexField), resolved.getIndex(), GlobalRelease);
                return result;
            } else if (((flags & IS_METHOD) != 0)){
                // resolve
                MethodElement resolved;
                // todo: consider visibility, caller
                if (kind == KIND_INVOKE_STATIC) {
                    // use virtual algorithm to find static
                    resolved = clazz.getTypeDefinition().resolveMethodElementVirtual(name.getContent(), desc);
                    // todo: ICCE check...
                } else if (kind == KIND_INVOKE_INTERFACE) {
                    // interface also uses virtual resolution - against the target class
                    resolved = clazz.getTypeDefinition().resolveMethodElementVirtual(name.getContent(), desc);
                    // todo: ICCE check...
                } else if (kind == KIND_INVOKE_SPECIAL) {
                    resolved = clazz.getTypeDefinition().resolveMethodElementExact(name.getContent(), desc);
                    // todo: ICCE check...
                } else if (kind == KIND_INVOKE_VIRTUAL) {
                    resolved = clazz.getTypeDefinition().resolveMethodElementVirtual(name.getContent(), desc);
                    // todo: ICCE check...
                } else {
                    throw new Thrown(linkageErrorClass.newInstance("Unknown handle kind"));
                }
                if (resolved == null) {
                    if (! speculativeResolve) {
                        throw new Thrown(linkageErrorClass.newInstance("No such method: " + clazz.getName() + "#" + name.getContent() + ":" + desc.toString()));
                    }
                    return null;
                }
                // create a member name
                VmObject result = vm.newInstance(memberNameClass, memberName4Ctor, List.of(
                    Byte.valueOf((byte) kind),
                    resolved.getEnclosingType().load().getVmClass(),
                    vm.intern(resolved.getName()),
                    type
                ));
                result.getMemory().store32(memberNameClass.indexOf(memberNameIndexField), resolved.getIndex(), GlobalRelease);
                return result;
            } else {
                throw new Thrown(linkageErrorClass.newInstance("Unknown resolution request"));
            }
        }
    }

    MethodDescriptor createFromMethodType(ClassContext classContext, VmObject methodType) {
        VmClass mtClass = methodType.getVmClass();
        if (! mtClass.getName().equals("java.lang.invoke.MethodType")) {
            throw new Thrown(linkageErrorClass.newInstance("Type argument is of wrong class"));
        }
        VmClass rtype = (VmClass) methodType.getMemory().loadRef(methodType.indexOf(methodTypeRTypeField), SinglePlain);
        if (rtype == null) {
            throw new Thrown(linkageErrorClass.newInstance("MethodType has null return type"));
        }
        VmArray ptypes = (VmArray) methodType.getMemory().loadRef(methodType.indexOf(methodTypePTypesField), SinglePlain);
        if (ptypes == null) {
            throw new Thrown(linkageErrorClass.newInstance("MethodType has null param types"));
        }
        int pcnt = ptypes.getLength();
        ArrayList<TypeDescriptor> paramTypes = new ArrayList<>(pcnt);
        for (int i = 0; i < pcnt; i ++) {
            VmClass clazz = (VmClass) ptypes.getMemory().loadRef(ptypes.getArrayElementOffset(i), SinglePlain);
            paramTypes.add(clazz.getDescriptor());
        }
        return MethodDescriptor.synthesize(classContext, rtype.getDescriptor(), paramTypes);
    }

    private Object methodHandleNativesObjectFieldOffset(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject name = (VmObject) args.get(0);
        // assume resolved
        VmClass clazz = (VmClass) name.getMemory().loadRef(name.indexOf(memberNameClazzField), SinglePlain);
        int idx = name.getMemory().load32(name.indexOf(memberNameIndexField), SinglePlain);
        if (idx == 0) {
            // todo: breakpoint
            idx = 0;
        }
        FieldElement field = clazz.getTypeDefinition().getField(idx);
        LayoutInfo layoutInfo;
        if (field.isStatic()) {
            throw new Thrown(linkageErrorClass.newInstance("Wrong field kind"));
        } else {
            layoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(field.getEnclosingType());
        }
        return Long.valueOf(layoutInfo.getMember(field).getOffset());
    }

    private Object methodHandleNativesStaticFieldBase(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject name = (VmObject) args.get(0);
        // assume resolved
        VmClass clazz = (VmClass) name.getMemory().loadRef(name.indexOf(memberNameClazzField), SinglePlain);
        int idx = name.getMemory().load32(name.indexOf(memberNameIndexField), SinglePlain);
        if (idx == 0) {
            // todo: breakpoint
            idx = 0;
        }
        FieldElement field = clazz.getTypeDefinition().getField(idx);
        LayoutInfo layoutInfo;
        if (field.isStatic()) {
            return field.getEnclosingType().load().getVmClass().getStaticFieldBase();
        } else {
            throw new Thrown(linkageErrorClass.newInstance("Wrong field kind"));
        }
    }

    private Object methodHandleNativesStaticFieldOffset(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject name = (VmObject) args.get(0);
        // assume resolved
        VmClass clazz = (VmClass) name.getMemory().loadRef(name.indexOf(memberNameClazzField), SinglePlain);
        int idx = name.getMemory().load32(name.indexOf(memberNameIndexField), SinglePlain);
        if (idx == 0) {
            // todo: breakpoint
            idx = 0;
        }
        FieldElement field = clazz.getTypeDefinition().getField(idx);
        LayoutInfo layoutInfo;
        if (field.isStatic()) {
            layoutInfo = Layout.get(ctxt).getStaticLayoutInfo(field.getEnclosingType());
        } else {
            throw new Thrown(linkageErrorClass.newInstance("Wrong field kind"));
        }
        return Long.valueOf(layoutInfo == null ? 0 : layoutInfo.getMember(field).getOffset());
    }

    private Object nativeConstructorAccessorImplNewInstance0(final VmThread thread, final VmObject ignored, final List<Object> args) {
        VmObject ctor = (VmObject) args.get(0);
        VmArray argsArray = (VmArray) args.get(1);
        // unbox the hyper-boxed arguments
        List<Object> unboxed;
        if (argsArray == null) {
            unboxed = List.of();
        } else {
            int argCnt = argsArray.getLength();
            unboxed = new ArrayList<>(argCnt);
            for (int i = 0; i < argCnt; i ++) {
                unboxed.add(unbox(argsArray.getMemory().loadRef(argsArray.getArrayElementOffset(i), SinglePlain)));
            }
        }
        // the enclosing class
        VmClass clazz = (VmClass) ctor.getMemory().loadRef(ctor.indexOf(ctorClazzField), SinglePlain);
        int slot = ctor.getMemory().load32(ctor.indexOf(ctorSlotField), SinglePlain);
        ConstructorElement ce = clazz.getTypeDefinition().getConstructor(slot);
        ctxt.enqueue(ce);
        try {
            return vm.newInstance(clazz, ce, unboxed);
        } catch (Thrown thrown) {
            throw new Thrown(invocationTargetExceptionClass.newInstance(thrown.getThrowable()));
        }
    }

    private Object nativeMethodAccessorImplInvoke0(final VmThread thread, final VmObject ignored, final List<Object> args) {
        //    private static native Object invoke0(Method m, Object obj, Object[] args);
        VmObject method = (VmObject) args.get(0);
        VmObject receiver = (VmObject) args.get(1);
        VmArray argsArray = (VmArray) args.get(2);
        // unbox the hyper-boxed arguments
        List<Object> unboxed;
        if (argsArray == null) {
            unboxed = List.of();
        } else {
            int argCnt = argsArray.getLength();
            unboxed = new ArrayList<>(argCnt);
            for (int i = 0; i < argCnt; i ++) {
                unboxed.add(unbox(argsArray.getMemory().loadRef(argsArray.getArrayElementOffset(i), SinglePlain)));
            }
        }
        // the enclosing class
        VmClass clazz = (VmClass) method.getMemory().loadRef(method.indexOf(methodClazzField), SinglePlain);
        int slot = method.getMemory().load32(method.indexOf(methodSlotField), SinglePlain);
        MethodElement me = clazz.getTypeDefinition().getMethod(slot);
        ctxt.enqueue(me);
        try {
            if (me.isStatic()) {
                return vm.invokeExact(me, null, unboxed);
            } else {
                return vm.invokeVirtual(me, receiver, unboxed);
            }
        } catch (Thrown thrown) {
            throw new Thrown(invocationTargetExceptionClass.newInstance(thrown.getThrowable()));
        }
    }

    /**
     * Unbox one single hyper-boxed argument.
     *
     * @param boxed the hyper-boxed argument
     * @return the boxed equivalent
     */
    // todo: move to an autoboxing plugin?
    private Object unbox(final VmObject boxed) {
        VmClass vmClass = boxed.getVmClass();
        if (vmClass == byteClass) {
            return Byte.valueOf((byte) boxed.getMemory().load8(boxed.indexOf(byteValueField), SinglePlain));
        } else if (vmClass == shortClass) {
            return Short.valueOf((short) boxed.getMemory().load16(boxed.indexOf(shortValueField), SinglePlain));
        } else if (vmClass == integerClass) {
            return Integer.valueOf(boxed.getMemory().load32(boxed.indexOf(integerValueField), SinglePlain));
        } else if (vmClass == longClass) {
            return Long.valueOf(boxed.getMemory().load64(boxed.indexOf(longValueField), SinglePlain));
        } else if (vmClass == characterClass) {
            return Character.valueOf((char) boxed.getMemory().load16(boxed.indexOf(characterValueField), SinglePlain));
        } else if (vmClass == floatClass) {
            return Float.valueOf(boxed.getMemory().loadFloat(boxed.indexOf(floatValueField), SinglePlain));
        } else if (vmClass == doubleClass) {
            return Double.valueOf(boxed.getMemory().loadDouble(boxed.indexOf(doubleValueField), SinglePlain));
        } else if (vmClass == booleanClass) {
            return Boolean.valueOf((boxed.getMemory().load8(boxed.indexOf(booleanValueField), SinglePlain) & 1) != 0);
        } else {
            // plain object
            return boxed;
        }
    }

    /**
     * Resolve the injected {@code index} field of {@code ResolvedMethodName} or {@code MemberName}.
     *
     * @param index the field index (ignored)
     * @param typeDef the type definition (ignored)
     * @param builder the field builder
     * @return the resolved field
     */
    private FieldElement resolveIndexField(final int index, final DefinedTypeDefinition typeDef, final FieldElement.Builder builder) {
        builder.setEnclosingType(typeDef);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT);
        builder.setSignature(BaseTypeSignature.I);
        return builder.build();
    }

    /**
     * Resolve the injected {@code clazz} field of {@code ResolvedMethodName}.
     *
     * @param index the field index (ignored)
     * @param typeDef the type definition (ignored)
     * @param builder the field builder
     * @return the resolved field
     */
    private FieldElement resolveClazzField(final int index, final DefinedTypeDefinition typeDef, final FieldElement.Builder builder) {
        builder.setEnclosingType(typeDef);
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT);
        builder.setSignature(TypeSignature.synthesize(typeDef.getContext(), builder.getDescriptor()));
        return builder.build();
    }

}
