package org.qbicc.plugin.native_;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.machine.probe.Qualifier;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.layout.NativeLayout;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.IntAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.annotation.type.TypeAnnotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.AnyTypeArgument;
import org.qbicc.type.generic.BoundTypeArgument;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.MethodSignature;
import org.qbicc.type.generic.ReferenceTypeSignature;
import org.qbicc.type.generic.Signature;
import org.qbicc.type.generic.TopLevelClassTypeSignature;
import org.qbicc.type.generic.TypeArgument;
import org.qbicc.type.generic.TypeParameter;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.generic.TypeVariableSignature;
import org.qbicc.type.generic.Variance;

/**
 *
 */
final class NativeInfo {
    static final AttachmentKey<NativeInfo> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;

    final ClassTypeDescriptor cNativeDesc;
    final ClassTypeDescriptor ptrDesc;
    final ClassTypeDescriptor wordDesc;
    final ClassTypeDescriptor cObjectDesc;

    final Map<TypeDescriptor, Map<String, Map<MethodDescriptor, NativeFunctionInfo>>> nativeFunctions = new ConcurrentHashMap<>();
    final Map<TypeDescriptor, Map<String, Map<MethodDescriptor, MethodElement>>> nativeBindings = new ConcurrentHashMap<>();
    final Map<FieldElement, FieldElement> nativeFieldBindings = new ConcurrentHashMap<>();
    final Map<TypeDescriptor, Map<String, NativeDataInfo>> nativeFields = new ConcurrentHashMap<>();
    final Map<DefinedTypeDefinition, AtomicReference<ValueType>> nativeTypes = new ConcurrentHashMap<>();
    final Map<DefinedTypeDefinition, AtomicReference<ValueType>> internalNativeTypes = new ConcurrentHashMap<>();
    final Map<DefinedTypeDefinition, FunctionalInterfaceData> functionalInterfaceMethods = new ConcurrentHashMap<>();
    final Set<InitializerElement> initializers = ConcurrentHashMap.newKeySet();

    private NativeInfo(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();
        cNativeDesc = ClassTypeDescriptor.parseClassConstant(classContext, ByteBuffer.wrap(Native.C_NATIVE_INT_NAME.getBytes(StandardCharsets.UTF_8)));
        ptrDesc = ClassTypeDescriptor.parseClassConstant(classContext, ByteBuffer.wrap(Native.PTR_INT_NAME.getBytes(StandardCharsets.UTF_8)));
        wordDesc = ClassTypeDescriptor.parseClassConstant(classContext, ByteBuffer.wrap(Native.WORD_INT_NAME.getBytes(StandardCharsets.UTF_8)));
        cObjectDesc = ClassTypeDescriptor.parseClassConstant(classContext, ByteBuffer.wrap(Native.OBJECT_INT_NAME.getBytes(StandardCharsets.UTF_8)));
    }

    static NativeInfo get(final CompilationContext ctxt) {
        NativeInfo nativeInfo = ctxt.getAttachment(KEY);
        if (nativeInfo == null) {
            NativeInfo appearing = ctxt.putAttachmentIfAbsent(KEY, nativeInfo = new NativeInfo(ctxt));
            if (appearing != null) {
                nativeInfo = appearing;
            }
        }
        return nativeInfo;
    }

    ValueType resolveInternalNativeType(final DefinedTypeDefinition definedType) {
        AtomicReference<ValueType> ref = internalNativeTypes.get(definedType);
        if (ref == null) {
            return null;
        }
        ClassContext classContext = definedType.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        ValueType resolved = ref.get();
        if (resolved == null) {
            synchronized (ref) {
                resolved = ref.get();
                if (resolved == null) {
                    LoadedTypeDefinition validated = definedType.load();
                    LayoutInfo layout = NativeLayout.get(ctxt).getLayoutInfo(validated);
                    resolved = layout.getCompoundType();
                    ref.set(resolved);
                }
            }
        }
        return resolved;
    }

    ValueType resolveNativeType(final DefinedTypeDefinition definedType) {
        AtomicReference<ValueType> ref = nativeTypes.get(definedType);
        if (ref == null) {
            return null;
        }
        ClassContext classContext = definedType.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        ValueType resolved = ref.get();
        if (resolved == null) {
            synchronized (ref) {
                resolved = ref.get();
                if (resolved == null) {
                    if (definedType.getSuperClassInternalName().equals(Native.PTR_INT_NAME)) {
                        ref.set(resolved = decodePointerType(definedType));
                    } else {
                        CProbe.Builder pb = CProbe.builder();
                        String simpleName = null;
                        Qualifier q = Qualifier.NONE;
                        boolean incomplete = false;
                        int annotatedAlign = 0;
                        for (Annotation annotation : definedType.getInvisibleAnnotations()) {
                            ClassTypeDescriptor annDesc = annotation.getDescriptor();
                            if (ProbeUtils.processCommonAnnotation(pb, annotation)) {
                                continue;
                            }
                            if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                                if (annDesc.getClassName().equals(Native.ANN_NAME)) {
                                    simpleName = ((StringAnnotationValue) annotation.getValue("value")).getString();
                                } else if (annDesc.getClassName().equals(Native.ANN_INCOMPLETE)) {
                                    incomplete = true;
                                } else if (annDesc.getClassName().equals(Native.ANN_ALIGN)) {
                                    annotatedAlign = ((IntAnnotationValue)annotation.getValue("value")).intValue();
                                    if (annotatedAlign == Integer.MAX_VALUE) {
                                        annotatedAlign = ctxt.getTypeSystem().getMaxAlignment();
                                    }
                                }
                            }
                        }
                        String enclosingName = definedType.getEnclosingClassInternalName();
                        while (enclosingName != null) {
                            DefinedTypeDefinition enclosingType = classContext.findDefinedType(enclosingName);
                            for (Annotation annotation : enclosingType.getInvisibleAnnotations()) {
                                ProbeUtils.processCommonAnnotation(pb, annotation);
                            }
                            enclosingName = enclosingType.getEnclosingClassInternalName();
                        }
                        if (simpleName == null) {
                            String fullName = definedType.getInternalName();
                            int idx = fullName.lastIndexOf('/');
                            simpleName = idx == -1 ? fullName : fullName.substring(idx + 1);
                            idx = simpleName.lastIndexOf('$');
                            simpleName = idx == -1 ? simpleName : simpleName.substring(idx + 1);
                            if (simpleName.startsWith("struct_")) {
                                q = Qualifier.STRUCT;
                                simpleName = simpleName.substring(7);
                            } else if (simpleName.startsWith("union_")) {
                                q = Qualifier.UNION;
                                simpleName = simpleName.substring(6);
                            }
                        }
                        // begin the real work
                        LoadedTypeDefinition vt = definedType.load();
                        int fc = vt.getFieldCount();
                        TypeSystem ts = ctxt.getTypeSystem();
                        CProbe.Type.Builder tb = CProbe.Type.builder();
                        tb.setName(simpleName);
                        tb.setQualifier(q);
                        for (int i = 0; i < fc; i ++) {
                            // compound type
                            FieldElement field = vt.getField(i);
                            if (! field.isStatic()) {
                                tb.addMember(field.getName());
                            }
                        }
                        CompoundType.Tag tag = q == Qualifier.NONE ? CompoundType.Tag.NONE : q == Qualifier.STRUCT ? CompoundType.Tag.STRUCT : CompoundType.Tag.UNION;
                        if (incomplete) {
                            resolved = ts.getIncompleteCompoundType(tag, simpleName);
                        } else {
                            CProbe.Type probeType = tb.build();
                            pb.probeType(probeType);
                            CProbe probe = pb.build();
                            try {
                                CProbe.Result result = probe.run(ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY), ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), ctxt);
                                if (result != null) {
                                    CProbe.Type.Info typeInfo = result.getTypeInfo(probeType);
                                    long size = typeInfo.getSize();
                                    int align = annotatedAlign != 0 ? annotatedAlign : (int) typeInfo.getAlign();
                                    if (typeInfo.isFloating()) {
                                        if (size == 4) {
                                            resolved = ts.getFloat32Type();
                                        } else if (size == 8) {
                                            resolved = ts.getFloat64Type();
                                        } else {
                                            resolved = ts.getCompoundType(tag, simpleName, size, align, List::of);
                                        }
                                    } else if (typeInfo.isSigned()) {
                                        if (size == 1) {
                                            resolved = ts.getSignedInteger8Type();
                                        } else if (size == 2) {
                                            resolved = ts.getSignedInteger16Type();
                                        } else if (size == 4) {
                                            resolved = ts.getSignedInteger32Type();
                                        } else if (size == 8) {
                                            resolved = ts.getSignedInteger64Type();
                                        } else {
                                            resolved = ts.getCompoundType(tag, simpleName, size, align, List::of);
                                        }
                                    } else if (typeInfo.isUnsigned()) {
                                        if (size == 1) {
                                            resolved = ts.getUnsignedInteger8Type();
                                        } else if (size == 2) {
                                            resolved = ts.getUnsignedInteger16Type();
                                        } else if (size == 4) {
                                            resolved = ts.getUnsignedInteger32Type();
                                        } else if (size == 8) {
                                            resolved = ts.getUnsignedInteger64Type();
                                        } else {
                                            resolved = ts.getCompoundType(tag, simpleName, size, align, List::of);
                                        }
                                    } else {
                                        resolved = ts.getCompoundType(tag, simpleName, size, align, () -> {
                                            ArrayList<CompoundType.Member> list = new ArrayList<>();
                                            for (int i = 0; i < fc; i ++) {
                                                FieldElement field = vt.getField(i);
                                                if (! field.isStatic()) {
                                                    ValueType type = field.getType();
                                                    // compound type
                                                    String name = field.getName();
                                                    CProbe.Type.Info member = result.getTypeInfoOfMember(probeType, name);
                                                    list.add(ts.getCompoundTypeMember(name, type, (int) member.getOffset(), 1));
                                                }
                                            }
                                            list.sort(Comparator.naturalOrder());
                                            return List.copyOf(list);
                                        });
                                    }
                                }
                                ref.set(resolved);
                            } catch (IOException e) {
                                ctxt.error(e, "Failed to define native type " + simpleName);
                                return ts.getPoisonType();
                            }
                        }
                    }
                }
            }
        }
        return resolved;
    }

    private ValueType decodePointerType(final DefinedTypeDefinition definedType) {
        TypeSystem ts = ctxt.getTypeSystem();
        ClassSignature signature = definedType.getSignature();
        ClassTypeSignature superClassSignature = signature.getSuperClassSignature();
        List<TypeArgument> typeArguments = superClassSignature.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return ts.getVoidType().getPointer();
        }
        TypeArgument ptrArg = typeArguments.get(0);
        if (ptrArg.equals(AnyTypeArgument.INSTANCE)) {
            return ts.getVoidType().getPointer();
        }
        BoundTypeArgument boundArg = (BoundTypeArgument) ptrArg;
        Variance variance = boundArg.getVariance();
        if (variance != Variance.INVARIANT) {
            ctxt.error(/*location, */ "Invalid pointer type variance");
        }
        ReferenceTypeSignature bound = boundArg.getBound();
        if (bound instanceof TopLevelClassTypeSignature) {
            TopLevelClassTypeSignature tlBound = (TopLevelClassTypeSignature) bound;
            if (tlBound.getPackageName().equals(Native.NATIVE_PKG) && tlBound.getIdentifier().equals("CNative$object")) {
                if (isArgConst(definedType)) {
                    return ts.getVoidType().getPointer().withConstPointee();
                }
            }
        }
        ClassContext classContext = definedType.getContext();
        // todo: acquire type annotation list from supertype description
        ValueType pointeeType = classContext.resolveTypeFromDescriptor(bound.asDescriptor(classContext), definedType, bound, TypeAnnotationList.empty(), TypeAnnotationList.empty());
        return pointeeType.getPointer();
    }

    private boolean isArgConst(DefinedTypeDefinition definedType) {
        Iterator<TypeAnnotation> iterator = definedType.getVisibleTypeAnnotations().onTypeArgument(0).iterator();
        while (iterator.hasNext()) {
            TypeAnnotation next = iterator.next();
            if (next.getAnnotation().getDescriptor().packageAndClassNameEquals(Native.NATIVE_PKG, Native.ANN_CONST)) {
                return true;
            }
        }
        return false;
    }

    static final class FunctionalInterfaceData {
        MethodElement me;
        ClassTypeSignature signature;

        FunctionalInterfaceData(MethodElement me, ClassTypeSignature signature) {
            this.me = me;
            this.signature = signature;
        }

        public ClassTypeSignature getClassTypeSignature() {
            return signature;
        }

        public MethodElement getMethodElement() {
            return me;
        }
    }

    public ValueType getTypeOfFunctionalInterface(final DefinedTypeDefinition definedType, final List<TypeArgument> arguments) {
        FunctionalInterfaceData fiData = getFunctionalInterfaceMethod(definedType);
        if (fiData == null) {
            return ctxt.getTypeSystem().getFunctionType(ctxt.getTypeSystem().getVoidType());
        }

        HashMap<String, ValueType> genericTypeMap = new HashMap();
        MethodElement method = fiData.getMethodElement();

        /* Map generic types to the ValueTypes they represent */
        List<TypeParameter> genericTypeParameters = definedType.getSignature().getTypeParameters();
        for (int i = 0; i < arguments.size(); i++) {
            String genericTypeId = genericTypeParameters.get(i).getIdentifier();
            BoundTypeArgument argument = (BoundTypeArgument) arguments.get(i);
            TopLevelClassTypeSignature argumentTypeSignature = (TopLevelClassTypeSignature)argument.getBound();
            ValueType argumentValue = ctxt.getBootstrapClassContext().resolveTypeFromClassName(argumentTypeSignature.getPackageName(), argumentTypeSignature.getIdentifier());
            genericTypeMap.put(genericTypeId, argumentValue);
        }

        /* compare class signature with method's class signature and make sure all generics have the correct assigned types.
        * The MethodElement's types may have been overridden if its from a super interface. For example:
        *   UnaryOperator extends Function<T, T>, but the class Function has generic names of Function<T, R>.
        *   In this case R should be mapped to the same ValueType as T.
        */
        List<TypeArgument> classTypeParameters = fiData.getClassTypeSignature().getTypeArguments();
        List<TypeParameter> methodsTypeParameters = method.getEnclosingType().getSignature().getTypeParameters();
        for (int i = 0; i < classTypeParameters.size(); i++) {
            TypeParameter methodParam = methodsTypeParameters.get(i);
            ValueType existingArgument = genericTypeMap.get(methodParam.getIdentifier());
            if (existingArgument == null) {
                BoundTypeArgument overridingType = (BoundTypeArgument) classTypeParameters.get(i);
                String identifier = ((TypeVariableSignature)overridingType.getBound()).getIdentifier();
                ValueType mapsTo = genericTypeMap.get(identifier);
                genericTypeMap.put(methodParam.getIdentifier(), mapsTo);
            }
        }

        /* Create the function type. */
        MethodSignature methodSignature = method.getSignature();
        List<TypeSignature> parameterTypeSignatures = methodSignature.getParameterTypes();
        ValueType returnType = genericTypeMap.get(((TypeVariableSignature)methodSignature.getReturnTypeSignature()).getIdentifier());
        ValueType[] parameterTypes = new ValueType[parameterTypeSignatures.size()];
        for (int i = 0; i < parameterTypeSignatures.size(); i++) {
            TypeVariableSignature sig = (TypeVariableSignature)parameterTypeSignatures.get(i);
            ValueType iou = genericTypeMap.get(sig.getIdentifier());
            parameterTypes[i] = iou;
        }
        ValueType functionType = ctxt.getTypeSystem().getFunctionType(returnType, parameterTypes);
        return functionType;
    }

    public FunctionalInterfaceData getFunctionalInterfaceMethod(final DefinedTypeDefinition definedType) {
        FunctionalInterfaceData fiData = functionalInterfaceMethods.get(definedType);
        if (fiData != null) {
            return fiData;
        }
        try {
            fiData = computeFunctionalInterfaceMethod(definedType.load(), definedType.getSignature(), new HashSet<>(), null);
        } catch (IllegalArgumentException ignored) {
        }
        if (fiData != null) {
            FunctionalInterfaceData appearing = functionalInterfaceMethods.putIfAbsent(definedType, fiData);
            if (appearing != null) {
                return appearing;
            }
        } else {
            ctxt.error("Interface \"%s\" is not a functional interface", definedType.getInternalName());
        }
        return fiData;
    }

    private FunctionalInterfaceData computeFunctionalInterfaceMethod(final LoadedTypeDefinition type, final Signature signature, final HashSet<LoadedTypeDefinition> visited, FunctionalInterfaceData found) {
        if (visited.add(type)) {
            int methodCount = type.getMethodCount();
            for (int i = 0; i < methodCount; i ++) {
                MethodElement method = type.getMethod(i);
                if (method.isAbstract() && method.isPublic()) {
                    if (found == null) {
                        found = new FunctionalInterfaceData(method, (ClassTypeSignature) signature);
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
            }
            int intCnt = type.getInterfaceCount();
            for (int i = 0; i < intCnt; i ++) {
                found = computeFunctionalInterfaceMethod(type.getInterface(i), ((ClassSignature)signature).getInterfaceSignatures().get(i), visited, found);
            }
        }
        return found;
    }

    public NativeFunctionInfo getFunctionInfo(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor) {
        return nativeFunctions.getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(descriptor);
    }

    public void registerFunctionInfo(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, NativeFunctionInfo info) {
        nativeFunctions.computeIfAbsent(owner, NativeInfo::newMap).computeIfAbsent(name, NativeInfo::newMap).put(descriptor, info);
    }

    private static <K, V> Map<K, V> newMap(final Object key) {
        return new ConcurrentHashMap<>();
    }

    public boolean registerInitializer(final InitializerElement initializerElement) {
        return initializers.add(initializerElement);
    }

    public void registerNativeBinding(final MethodElement origMethod, final MethodElement nativeMethod) {
        nativeBindings.computeIfAbsent(origMethod.getEnclosingType().getDescriptor(), NativeInfo::newMap)
            .computeIfAbsent(origMethod.getName(), NativeInfo::newMap)
            .put(origMethod.getDescriptor(), nativeMethod);
    }

    public void registerNativeBinding(final FieldElement origField, final FieldElement mappedField) {
        nativeFieldBindings.put(origField, mappedField);
    }

    public MethodElement getNativeBinding(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor) {
        return nativeBindings.getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(descriptor);
    }

    public MethodElement getNativeBinding(final MethodElement original) {
        return getNativeBinding(original.getEnclosingType().getDescriptor(), original.getName(), original.getDescriptor());
    }

    public FieldElement getNativeBinding(final FieldElement original) {
        return nativeFieldBindings.get(original);
    }

    void registerLibrary(String library) {
        Linker.get(ctxt).addLibrary(library);
    }

    public void registerFieldInfo(final TypeDescriptor owner, final String name, final NativeDataInfo info) {
        nativeFields.computeIfAbsent(owner, NativeInfo::newMap).put(name, info);
    }

    public NativeDataInfo getFieldInfo(final TypeDescriptor owner, final String name) {
        return nativeFields.getOrDefault(owner, Map.of()).get(name);
    }
}
