package org.qbicc.plugin.native_;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.machine.probe.Qualifier;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.plugin.core.ConditionEvaluation;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.type.FunctionType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.StructType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.ClassAnnotationValue;
import org.qbicc.type.annotation.IntAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.annotation.type.TypeAnnotation;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.AnyTypeArgument;
import org.qbicc.type.generic.BoundTypeArgument;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.ReferenceTypeSignature;
import org.qbicc.type.generic.Signature;
import org.qbicc.type.generic.TopLevelClassTypeSignature;
import org.qbicc.type.generic.TypeArgument;
import org.qbicc.type.generic.TypeSignature;
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
    final Map<DefinedTypeDefinition, InstanceMethodElement> functionalInterfaceMethods = new ConcurrentHashMap<>();
    final Set<InitializerElement> initializers = ConcurrentHashMap.newKeySet();
    final Map<DefinedTypeDefinition, List<FunctionAndPriority>> globalCtors = new ConcurrentHashMap<>();
    final Map<DefinedTypeDefinition, List<FunctionAndPriority>> globalDtors = new ConcurrentHashMap<>();

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
                    ref.set(resolved = NativeLayout.get(ctxt).getLayoutInfo(validated));
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
        ConditionEvaluation conditionEvaluation = ConditionEvaluation.get(ctxt);
        ValueType resolved = ref.get();
        if (resolved == null) {
            synchronized (ref) {
                resolved = ref.get();
                if (resolved == null) {
                    ValueType yamlType;
                    if (definedType.getSuperClassInternalName().equals(Native.PTR_INT_NAME)) {
                        ref.set(resolved = decodePointerType(definedType));
                    } else if ((yamlType = probeYaml(definedType)) != null) {
                        ref.set(resolved = yamlType);
                    } else {
                        CProbe.Builder pb = CProbe.builder();
                        String simpleName = null;
                        Qualifier q = Qualifier.NONE;
                        boolean incomplete = false;
                        int annotatedAlign = 0;
                        boolean union = definedType.getSuperClassInternalName().equals(Native.UNION_INT_NAME);
                        processEnclosingType(classContext, pb, definedType);
                        ProbeUtils.ProbeProcessor pp = new ProbeUtils.ProbeProcessor(classContext, definedType);
                        for (Annotation annotation : definedType.getInvisibleAnnotations()) {
                            ClassTypeDescriptor annDesc = annotation.getDescriptor();
                            if (pp.processAnnotation(annotation)) {
                                continue;
                            }
                            if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                                if (annDesc.getClassName().equals(Native.ANN_NAME) && simpleName == null) {
                                    if (conditionEvaluation.evaluateConditions(classContext, definedType, annotation)) {
                                        simpleName = ((StringAnnotationValue) annotation.getValue("value")).getString();
                                    }
                                } else if (annDesc.getClassName().equals(Native.ANN_NAME_LIST) && simpleName == null) {
                                    if (annotation.getValue("value") instanceof ArrayAnnotationValue aav) {
                                        int cnt = aav.getElementCount();
                                        for (int i = 0; i < cnt; i ++) {
                                            if (aav.getValue(i) instanceof Annotation nested) {
                                                ClassTypeDescriptor nestedDesc = nested.getDescriptor();
                                                if (nestedDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                                                    if (nestedDesc.getClassName().equals(Native.ANN_NAME)) {
                                                        if (conditionEvaluation.evaluateConditions(classContext, definedType, nested)) {
                                                            simpleName = ((StringAnnotationValue) nested.getValue("value")).getString();
                                                            // stop searching for names
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (annDesc.getClassName().equals(Native.ANN_INCOMPLETE)) {
                                    if (conditionEvaluation.evaluateConditions(classContext, definedType, annotation)) {
                                        incomplete = true;
                                    }
                                } else if (annDesc.getClassName().equals(Native.ANN_ALIGN) && annotatedAlign == 0) {
                                    if (conditionEvaluation.evaluateConditions(classContext, definedType, annotation)) {
                                        annotatedAlign = ((IntAnnotationValue)annotation.getValue("value")).intValue();
                                        if (annotatedAlign == Integer.MAX_VALUE) {
                                            annotatedAlign = ctxt.getTypeSystem().getMaxAlignment();
                                        }
                                    }
                                } else if (annDesc.getClassName().equals(Native.ANN_ALIGN_LIST) && annotatedAlign == 0) {
                                    if (annotation.getValue("value") instanceof ArrayAnnotationValue aav) {
                                        int cnt = aav.getElementCount();
                                        for (int i = 0; i < cnt; i ++) {
                                            if (aav.getValue(i) instanceof Annotation nested) {
                                                ClassTypeDescriptor nestedDesc = nested.getDescriptor();
                                                if (nestedDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                                                    if (nestedDesc.getClassName().equals(Native.ANN_ALIGN)) {
                                                        if (conditionEvaluation.evaluateConditions(classContext, definedType, nested)) {
                                                            annotatedAlign = ((IntAnnotationValue)nested.getValue("value")).intValue();
                                                            if (annotatedAlign == Integer.MAX_VALUE) {
                                                                annotatedAlign = ctxt.getTypeSystem().getMaxAlignment();
                                                            }
                                                            // stop searching for alignments
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (annDesc.getClassName().equals(Native.ANN_ALIGN_AS) && annotatedAlign == 0) {
                                    if (annotation.getValue("value") instanceof ClassAnnotationValue cav) {
                                        if (conditionEvaluation.evaluateConditions(classContext, definedType, annotation)) {
                                            ValueType resolvedType = classContext.resolveTypeFromDescriptor(
                                                cav.getDescriptor(),
                                                definedType,
                                                TypeSignature.synthesize(classContext, definedType.getDescriptor())
                                            );
                                            annotatedAlign = resolvedType.getAlign();
                                        }
                                    }
                                } else if (annDesc.getClassName().equals(Native.ANN_ALIGN_AS_LIST) && annotatedAlign == 0) {
                                    if (annotation.getValue("value") instanceof ArrayAnnotationValue aav) {
                                        int cnt = aav.getElementCount();
                                        for (int i = 0; i < cnt; i ++) {
                                            if (annotatedAlign != 0) {
                                                break;
                                            }
                                            if (aav.getValue(i) instanceof Annotation nested) {
                                                ClassTypeDescriptor nestedDesc = nested.getDescriptor();
                                                if (nestedDesc.packageAndClassNameEquals(Native.NATIVE_PKG, Native.ANN_ALIGN_AS)) {
                                                    if (nested.getValue("value") instanceof ClassAnnotationValue cav) {
                                                        if (conditionEvaluation.evaluateConditions(classContext, definedType, nested)) {
                                                            ValueType resolvedType = classContext.resolveTypeFromDescriptor(
                                                                cav.getDescriptor(),
                                                                definedType,
                                                                TypeSignature.synthesize(classContext, definedType.getDescriptor())
                                                            );
                                                            annotatedAlign = resolvedType.getAlign();
                                                            // stop searching for alignments
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        pp.accept(pb);
                        if (simpleName == null) {
                            String fullName = definedType.getInternalName();
                            int idx = fullName.lastIndexOf('/');
                            simpleName = idx == -1 ? fullName : fullName.substring(idx + 1);
                            idx = simpleName.lastIndexOf('$');
                            simpleName = idx == -1 ? simpleName : simpleName.substring(idx + 1);
                            if (simpleName.startsWith("struct_") && !union) {
                                q = Qualifier.STRUCT;
                                simpleName = simpleName.substring(7);
                            } else if (simpleName.startsWith("union_") && union) {
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
                        if (! union) {
                            eachField: for (int i = 0; i < fc; i ++) {
                                // compound type
                                FieldElement field = vt.getField(i);
                                boolean nameOverridden = false;
                                String fieldName = field.getName();
                                if (! field.isStatic()) {
                                    for (Annotation annotation : field.getInvisibleAnnotations()) {
                                        ClassTypeDescriptor annDesc = annotation.getDescriptor();
                                        if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                                            if (annDesc.getClassName().equals(Native.ANN_NAME) && ! nameOverridden) {
                                                if (conditionEvaluation.evaluateConditions(classContext, definedType, annotation)) {
                                                    fieldName = ((StringAnnotationValue) annotation.getValue("value")).getString();
                                                    nameOverridden = true;
                                                }
                                            } else if (annDesc.getClassName().equals(Native.ANN_NAME_LIST) && ! nameOverridden) {
                                                if (annotation.getValue("value") instanceof ArrayAnnotationValue aav) {
                                                    int cnt = aav.getElementCount();
                                                    for (int j = 0; j < cnt; j ++) {
                                                        if (aav.getValue(j) instanceof Annotation nested) {
                                                            ClassTypeDescriptor nestedDesc = nested.getDescriptor();
                                                            if (nestedDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                                                                if (nestedDesc.getClassName().equals(Native.ANN_NAME)) {
                                                                    if (conditionEvaluation.evaluateConditions(classContext, definedType, nested)) {
                                                                        fieldName = ((StringAnnotationValue) nested.getValue("value")).getString();
                                                                        nameOverridden = true;
                                                                        // stop searching for names
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (annDesc.getClassName().equals(Native.ANN_INCOMPLETE)) {
                                                if (conditionEvaluation.evaluateConditions(classContext, definedType, annotation)) {
                                                    continue eachField;
                                                }
                                            }
                                        }
                                    }
                                    tb.addMember(fieldName);
                                }
                            }
                        }
                        UnionType.Tag utTag = q == Qualifier.NONE ? UnionType.Tag.NONE : UnionType.Tag.UNION;
                        StructType.Tag ctTag = q == Qualifier.NONE ? StructType.Tag.NONE : StructType.Tag.STRUCT;
                        if (incomplete) {
                            // even if they wanted a union, they get a struct with no members
                            resolved = ts.getIncompleteStructType(ctTag, simpleName);
                        } else {
                            CToolChain toolChain = ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY);
                            if (toolChain == null) {
                                ctxt.error("Cannot resolve native type information for %s (no probe available for this platform)", definedType.getInternalName());
                                return ts.getPoisonType();
                            }
                            CProbe.Type probeType = tb.build();
                            pb.probeType(probeType);
                            CProbe probe = pb.build();
                            try {
                                CProbe.Result result = probe.run(toolChain, ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), ctxt);
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
                                            resolved = ts.getStructType(ctTag, simpleName, size, align, List::of);
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
                                            resolved = ts.getStructType(ctTag, simpleName, size, align, List::of);
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
                                            resolved = ts.getStructType(ctTag, simpleName, size, align, List::of);
                                        }
                                    } else if (union) {
                                        resolved = ts.getUnionType(utTag, simpleName, () -> {
                                            ArrayList<UnionType.Member> list = new ArrayList<>();
                                            for (int i = 0; i < fc; i ++) {
                                                FieldElement field = vt.getField(i);
                                                if (! field.isStatic()) {
                                                    ValueType type = field.getType();
                                                    // union type
                                                    list.add(ts.getUnionTypeMember(field.getName(), type));
                                                }
                                            }
                                            return List.copyOf(list);
                                        });
                                    } else {
                                        resolved = ts.getStructType(ctTag, simpleName, size, align, () -> {
                                            ArrayList<StructType.Member> list = new ArrayList<>();
                                            for (int i = 0; i < fc; i ++) {
                                                FieldElement field = vt.getField(i);
                                                if (! field.isStatic()) {
                                                    ValueType type = field.getType();
                                                    // compound type
                                                    String name = field.getName();
                                                    CProbe.Type.Info member = result.getTypeInfoOfMember(probeType, name);
                                                    list.add(ts.getProbedStructTypeMember(name, type, (int) member.getOffset()));
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

    private static final AttachmentKey<Map<String, YamlProbeInfo>> PROBE_INFO_CACHE_KEY = new AttachmentKey<>();

    private Map<String, YamlProbeInfo> getProbeInfo() {
        Map<String, YamlProbeInfo> map = ctxt.getAttachment(PROBE_INFO_CACHE_KEY);
        if (map == null) {
            Platform platform = ctxt.getPlatform();
            String searchName = platform.cpu() + "-" + platform.os() + "-" + platform.abi();
            String path = "/bundles/" + searchName + "/platform-abi-type-info.yaml";
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                searchName = platform.cpu() + "-" + platform.os();
                path = "/bundles/" + searchName + "/platform-abi-type-info.yaml";
                is = getClass().getResourceAsStream(path);
            }
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                ObjectReader objectReader = mapper.readerForMapOf(YamlProbeInfo.class);
                try (InputStream ignored = is) {
                    try (InputStreamReader reader = new InputStreamReader(is)) {
                        try (BufferedReader br = new BufferedReader(reader)) {
                            map = objectReader.readValue(br);
                        }
                    }
                } catch (IOException e) {
                    map = Map.of();
                }
            } else {
                map = Map.of();
            }
            Map<String, YamlProbeInfo> appearing = ctxt.putAttachmentIfAbsent(PROBE_INFO_CACHE_KEY, map);
            if (appearing != null) {
                map = appearing;
            }
        }
        return map;
    }

    private ValueType probeYaml(final DefinedTypeDefinition definedType) {
        // compute host-side name
        String name = null;
        for (Annotation annotation : definedType.getInvisibleAnnotations()) {
            if (annotation.getDescriptor().packageAndClassNameEquals(Native.NATIVE_PKG, Native.ANN_NAME)) {
                name = ((StringAnnotationValue)annotation.getValue("value")).getString();
            }
        }
        if (name == null) {
            String internalName = definedType.getInternalName();
            int idx = internalName.lastIndexOf('/');
            name = idx == -1 ? internalName : internalName.substring(idx + 1);
            idx = name.lastIndexOf('$');
            if (idx != -1) {
                name = name.substring(idx + 1);
            }
        }
        if (name.startsWith("struct_")) {
            name = name.substring(7);
        } else if (name.startsWith("union_")) {
            name = name.substring(6);
        }
        YamlProbeInfo info = getProbeInfo().get(name);
        return info == null ? null : decodeYamlInfo(name, info);
    }

    private ValueType decodeYamlInfo(final String name, final YamlProbeInfo yamlInfo) {
        final TypeSystem ts = ctxt.getTypeSystem();
        return switch (yamlInfo.kind()) {
            case incomplete -> ts.getIncompleteStructType(StructType.Tag.NONE, name);
            case boolean_ -> ts.getBooleanType();
            case struct -> ts.getStructType(switch (yamlInfo.tag()) {
                    case none, union -> StructType.Tag.NONE;
                    case struct -> StructType.Tag.STRUCT;
                }, name, () -> yamlInfo.members().values().stream().map(m ->
                ts.getProbedStructTypeMember(
                    m.name(),
                    decodeYamlInfo(m.type(), getProbeInfo().get(m.type())),
                    m.offset()
                )).toList()
            );
            case union -> ts.getUnionType(switch (yamlInfo.tag()) {
                    case none, struct -> UnionType.Tag.NONE;
                    case union -> UnionType.Tag.UNION;
                }, name, () -> yamlInfo.members().values().stream().map(m ->
                ts.getUnionTypeMember(
                    m.name(),
                    decodeYamlInfo(m.type(), getProbeInfo().get(m.type()))
                )).toList()
            );
            case signed_integer -> switch (yamlInfo.size()) {
                case 1 -> ts.getSignedInteger8Type();
                case 2 -> ts.getSignedInteger16Type();
                case 4 -> ts.getSignedInteger32Type();
                case 8 -> ts.getSignedInteger64Type();
                default -> throw new IllegalArgumentException();
            };
            case unsigned_integer -> switch (yamlInfo.size()) {
                case 1 -> ts.getUnsignedInteger8Type();
                case 2 -> ts.getUnsignedInteger16Type();
                case 4 -> ts.getUnsignedInteger32Type();
                case 8 -> ts.getUnsignedInteger64Type();
                default -> throw new IllegalArgumentException();
            };
            case float_ -> switch (yamlInfo.size()) {
                case 4 -> ts.getFloat32Type();
                case 8 -> ts.getFloat64Type();
                default -> throw new IllegalArgumentException();
            };
            case pointer -> switch (yamlInfo.size()) {
                case 4 -> ts.getVoidType().getPointer();
                case 8 -> ts.getPointerSize() == 8 ? ts.getVoidType().getPointer() : ts.getVoidType().getPointer().asWide();
                default -> throw new IllegalArgumentException();
            };
            case void_ -> ts.getVoidType();
        };
    }

    private void processEnclosingType(ClassContext classContext, CProbe.Builder builder, DefinedTypeDefinition definedType) {
        String enclosingName = definedType.getEnclosingClassInternalName();
        if (enclosingName != null) {
            DefinedTypeDefinition enclosingType = classContext.findDefinedType(enclosingName);
            // enclosing types first so enclosed types can override
            processEnclosingType(classContext, builder, enclosingType);
            ProbeUtils.ProbeProcessor pp = new ProbeUtils.ProbeProcessor(classContext, definedType);
            for (Annotation annotation : enclosingType.getInvisibleAnnotations()) {
                pp.processAnnotation(annotation);
            }
            pp.accept(builder);
        }
    }

    private ValueType decodePointerType(final DefinedTypeDefinition definedType) {
        TypeSystem ts = ctxt.getTypeSystem();
        if (definedType.internalNameEquals("org/qbicc/runtime/CNative$function_ptr")) {
            // xxx - this type will be removed in the future
            return ts.getFunctionType(ts.getVoidType(), List.of()).getPointer();
        }
        Signature signature = definedType.getSignature();
        ClassTypeSignature superClassSignature = signature instanceof ClassSignature cs ? cs.getSuperClassSignature() : null;
        List<TypeArgument> typeArguments = superClassSignature == null ? List.of() : superClassSignature.getTypeArguments();
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
        ValueType pointeeType = classContext.resolveTypeFromDescriptor(bound.asDescriptor(classContext), definedType, bound);
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

    public ValueType getNativeType(final DefinedTypeDefinition def) {
        AtomicReference<ValueType> ref = nativeTypes.get(def);
        return ref == null ? null : ref.get();
    }

    public boolean isNativeType(final DefinedTypeDefinition enclosingType) {
        return nativeTypes.containsKey(enclosingType);
    }

    public FunctionType getInterfaceAsFunctionType(final DefinedTypeDefinition definedType) {
        InstanceMethodElement method = getFunctionalInterfaceMethod(definedType);
        if (method == null) {
            ctxt.error("No functional interface method on \"%s\"", definedType);
            return ctxt.getTypeSystem().getFunctionType(ctxt.getTypeSystem().getVoidType(), List.of());
        }
        // the method type is the invocation type
        return ctxt.getTypeSystem().getFunctionType(method.getType().getReturnType(), method.getType().getParameterTypes());
    }

    public StaticMethodType getInterfaceAsStaticMethodType(final DefinedTypeDefinition definedType) {
        InstanceMethodElement method = getFunctionalInterfaceMethod(definedType);
        if (method == null) {
            ctxt.error("No functional interface method on \"%s\"", definedType);
            return ctxt.getTypeSystem().getStaticMethodType(ctxt.getTypeSystem().getVoidType(), List.of());
        }
        // the method type is the invocation type
        return ctxt.getTypeSystem().getStaticMethodType(method.getType().getReturnType(), method.getType().getParameterTypes());
    }

    public InstanceMethodType getInterfaceAsInstanceMethodType(final DefinedTypeDefinition definedType, final ValueType receiverType) {
        InstanceMethodElement method = getFunctionalInterfaceMethod(definedType);
        if (method == null) {
            ctxt.error("No functional interface method on \"%s\"", definedType);
            return ctxt.getTypeSystem().getInstanceMethodType(ctxt.getTypeSystem().getVoidType(), ctxt.getTypeSystem().getVoidType(), List.of());
        }
        // the method type is the invocation type
        return ctxt.getTypeSystem().getInstanceMethodType(receiverType, method.getType().getReturnType(), method.getType().getParameterTypes());
    }

    public InstanceMethodElement getFunctionalInterfaceMethod(final DefinedTypeDefinition definedType) {
        InstanceMethodElement fiData = functionalInterfaceMethods.get(definedType);
        if (fiData != null) {
            return fiData;
        }
        try {
            fiData = computeFunctionalInterfaceMethod(definedType.load(), new HashSet<>(), null);
        } catch (IllegalArgumentException ignored) {
        }
        if (fiData != null) {
            InstanceMethodElement appearing = functionalInterfaceMethods.putIfAbsent(definedType, fiData);
            if (appearing != null) {
                return appearing;
            }
        } else {
            ctxt.error("Interface \"%s\" is not a functional interface", definedType.getInternalName());
        }
        return fiData;
    }

    private InstanceMethodElement computeFunctionalInterfaceMethod(final LoadedTypeDefinition type, final HashSet<LoadedTypeDefinition> visited, InstanceMethodElement found) {
        if (visited.add(type)) {
            int methodCount = type.getMethodCount();
            for (int i = 0; i < methodCount; i ++) {
                MethodElement method = type.getMethod(i);
                if (method.isAbstract() && method.isPublic() && method instanceof InstanceMethodElement ime) {
                    InstanceMethodType methodType = ime.getType();
                    if (found == null) {
                        found = ime;
                    } else {
                        InstanceMethodType foundType = found.getType();
                        if (! foundType.getReturnType().equals(methodType.getReturnType()) || ! foundType.getParameterTypes().equals(methodType.getParameterTypes())) {
                            throw new IllegalArgumentException();
                        }
                    }
                }
            }
            int intCnt = type.getInterfaceCount();
            for (int i = 0; i < intCnt; i ++) {
                found = computeFunctionalInterfaceMethod(type.getInterface(i), visited, found);
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

    public void registerGlobalConstructor(FunctionElement element, int priority) {
        registerGlobalXtor(globalCtors, element, priority);
    }

    public void registerGlobalDestructor(FunctionElement element, int priority) {
        registerGlobalXtor(globalDtors, element, priority);
    }

    private void registerGlobalXtor(final Map<DefinedTypeDefinition, List<FunctionAndPriority>> map, final FunctionElement element, final int priority) {
        Assert.checkNotNullParam("element", element);
        List<FunctionAndPriority> list = map.computeIfAbsent(element.getEnclosingType(), NativeInfo::createList);
        synchronized (list) {
            list.add(new FunctionAndPriority(element, priority));
        }
    }

    private static List<FunctionAndPriority> createList(final DefinedTypeDefinition ignored) {
        return new ArrayList<>();
    }

    public List<FunctionAndPriority> getGlobalConstructors() {
        return getGlobalXtors(globalCtors);
    }

    public List<FunctionAndPriority> getGlobalDestructors() {
        return getGlobalXtors(globalDtors);
    }

    private List<FunctionAndPriority> getGlobalXtors(Map<DefinedTypeDefinition, List<FunctionAndPriority>> map) {
        ArrayList<FunctionAndPriority> list = new ArrayList<>();
        for (List<FunctionAndPriority> subList : map.values()) {
            synchronized (subList) {
                list.addAll(subList);
            }
        }
        return list;
    }

    public record FunctionAndPriority(FunctionElement function, int priority) {}
}
