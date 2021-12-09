package org.qbicc.plugin.reflection;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.classfile.ConstantPool;
import org.qbicc.type.definition.element.AnnotatedElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * Hub for reflection support.
 */
public final class Reflection {
    private static final AttachmentKey<Reflection> KEY = new AttachmentKey<>();

    private static final byte[] NO_BYTES = new byte[0];

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
    private final ConstructorElement cpCtor;
    private final ConstructorElement fieldCtor;
    private final ConstructorElement methodCtor;
    private final ConstructorElement ctorCtor;

    private Reflection(CompilationContext ctxt) {
        vm = ctxt.getVm();
        noBytes = vm.newByteArray(NO_BYTES);
        ClassContext classContext = ctxt.getBootstrapClassContext();
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
        LoadedTypeDefinition fieldDef = classContext.findDefinedType("java/lang/reflect/Field").load();
        fieldClass = fieldDef.getVmClass();
        fieldCtor = fieldDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 8);
        LoadedTypeDefinition methodDef = classContext.findDefinedType("java/lang/reflect/Method").load();
        methodClass = methodDef.getVmClass();
        methodCtor = methodDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 11);
        LoadedTypeDefinition ctorDef = classContext.findDefinedType("java/lang/reflect/Constructor").load();
        constructorClass = ctorDef.getVmClass();
        ctorCtor = ctorDef.requireSingleConstructor(ce -> ce.getDescriptor().getParameterTypes().size() == 8);
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
        VmArray paramTypesVal = vm.newArrayOf(methodClass.getArrayClass(), paramTypes.size());
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

//    Constructor(Class<T> declaringClass,
//                Class<?>[] parameterTypes,
//                Class<?>[] checkedExceptions,
//                int modifiers,
//                int slot,
//                String signature,
//                byte[] annotations,
//                byte[] parameterAnnotations) {

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
}
