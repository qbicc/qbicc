package org.qbicc.plugin.patcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.qbicc.context.ClassContext;
import org.qbicc.plugin.core.ConditionEvaluation;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
final class PatchedTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final ClassContext classContext;
    private final ClassContextPatchInfo contextInfo;
    private final DefinedTypeDefinition.Builder delegate;
    private String internalName;
    private Map<String, FieldPatchInfo> addedFields;
    private Map<String, Map<MethodDescriptor, MethodPatchInfo>> addedMethods;
    private Map<MethodDescriptor, ConstructorPatchInfo> addedConstructors;

    private ClassPatchInfo classPatchInfo;

    PatchedTypeBuilder(ClassContext classContext, ClassContextPatchInfo contextInfo, DefinedTypeDefinition.Builder delegate) {
        this.classContext = classContext;
        this.contextInfo = contextInfo;
        this.delegate = delegate;
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void setName(String internalName) {
        if (contextInfo.isPatchClass(internalName)) {
            throw new IllegalStateException("A patch class was found for loading: " + internalName);
        }
        classPatchInfo = contextInfo.get(internalName);
        this.internalName = internalName;
        if (classPatchInfo != null) {
            // no further changes may be registered
            synchronized (classPatchInfo) {
                classPatchInfo.commit();
                // record all injected members in case they're already present
                List<FieldPatchInfo> injectedFields = classPatchInfo.getInjectedFields();
                addedFields = mapOf(injectedFields, FieldPatchInfo::getName);
                List<ConstructorPatchInfo> injectedConstructors = classPatchInfo.getInjectedConstructors();
                addedConstructors = mapOf(injectedConstructors, ConstructorPatchInfo::getDescriptor);
                List<MethodPatchInfo> injectedMethods = classPatchInfo.getInjectedMethods();
                addedMethods = mapOf(injectedMethods, MethodPatchInfo::getName, MethodPatchInfo::getDescriptor);
            }
        }
        getDelegate().setName(internalName);
    }

    @Override
    public void setNestHost(String nestHost) {
        if (contextInfo.isPatchClass(nestHost)) {
            // skip it
            return;
        }
        getDelegate().setNestHost(nestHost);
    }

    @Override
    public void addNestMember(String nestMember) {
        if (contextInfo.isPatchClass(nestMember)) {
            // skip it
            return;
        }
        getDelegate().addNestMember(nestMember);
    }

    @Override
    public void setInitializer(InitializerResolver resolver, int index) {
        ClassPatchInfo classPatchInfo = this.classPatchInfo;
        if (classPatchInfo != null) {
            InitializerPatchInfo patchInfo;
            synchronized (classPatchInfo) {
                if (classPatchInfo.isDeletedInitializer()) {
                    getDelegate().setInitializer(resolver, -1);
                    return;
                }
                patchInfo = classPatchInfo.getReplacementInitializerInfo();
            }
            ConditionEvaluation ce = ConditionEvaluation.get(classContext.getCompilationContext());
            if (patchInfo == null || !ce.evaluateConditions(classContext, patchInfo, patchInfo.getAnnotation())) {
                getDelegate().setInitializer(resolver, index);
            } else {
                getDelegate().setInitializer(patchInfo.getInitializerResolver(), patchInfo.getIndex());
            }
        } else {
            getDelegate().setInitializer(resolver, index);
        }
    }

    @Override
    public void setInvisibleAnnotations(List<Annotation> annotations) {
        ClassPatchInfo classPatchInfo = this.classPatchInfo;
        if (classPatchInfo != null) {
            synchronized (classPatchInfo) {
                List<Annotation> added = classPatchInfo.getAddedClassAnnotations();
                if (!added.isEmpty()) {
                    ArrayList<Annotation> tmp = new ArrayList<>();
                    tmp.addAll(annotations);
                    tmp.addAll(added);
                    annotations = tmp;
                }
            }
        }
        getDelegate().setInvisibleAnnotations(annotations);
    }

    @Override
    public void addField(FieldResolver resolver, int index, String name, TypeDescriptor descriptor) {
        ClassPatchInfo classPatchInfo = this.classPatchInfo;
        if (classPatchInfo != null) {
            ConditionEvaluation ce = ConditionEvaluation.get(classContext.getCompilationContext());
            FieldPatchInfo patchInfo;
            FieldPatchInfo annotateInfo;
            FieldPatchInfo injectedInfo;
            RuntimeInitializerPatchInfo initInfo;
            synchronized (classPatchInfo) {
                FieldDeleteInfo delInfo = classPatchInfo.getDeletedFieldInfo(name, descriptor);
                if (delInfo != null && ce.evaluateConditions(classContext, delInfo, delInfo.getAnnotation())) {
                    // skip completely
                    return;
                }
                patchInfo = classPatchInfo.getReplacementFieldInfo(name, descriptor);
                injectedInfo = addedFields.get(name);
                annotateInfo = classPatchInfo.getAnnotatedFieldInfo(name, descriptor);
                initInfo = classPatchInfo.getRuntimeInitFieldInfo(name, descriptor);
            }
            if (patchInfo != null && ce.evaluateConditions(classContext, patchInfo, patchInfo.getAnnotation())) {
                resolver = new PatcherFieldResolver(patchInfo);
                index = patchInfo.getIndex();
            } else if (injectedInfo != null && ce.evaluateConditions(classContext, injectedInfo, injectedInfo.getAnnotation())) {
                classContext.getCompilationContext().warning("Injected field %s was already present on %s (replacing)", name, internalName);
                return;
            }
            if (annotateInfo != null && ce.evaluateConditions(classContext, annotateInfo, annotateInfo.getAnnotation())) {
                resolver = new AnnotationAddingResolver(annotateInfo.getAddedAnnotations(), resolver);
            }
            if (initInfo != null && ce.evaluateConditions(classContext, initInfo, initInfo.getAnnotation())) {
                resolver = new PatcherFieldRuntimeInitResolver(initInfo, resolver);
            }
            getDelegate().addField(resolver, index, name, descriptor);
        } else {
            getDelegate().addField(resolver, index, name, descriptor);
        }
    }

    @Override
    public void addConstructor(ConstructorResolver resolver, int index, MethodDescriptor descriptor) {
        ClassPatchInfo classPatchInfo = this.classPatchInfo;
        if (classPatchInfo != null) {
            ConditionEvaluation ce = ConditionEvaluation.get(classContext.getCompilationContext());
            ConstructorPatchInfo constructorInfo;
            ConstructorPatchInfo annotateInfo;
            ConstructorPatchInfo injectedInfo;
            synchronized (classPatchInfo) {
                ConstructorDeleteInfo delInfo = classPatchInfo.getDeletedConstructorInfo(descriptor);
                if (delInfo != null && ce.evaluateConditions(classContext, delInfo, delInfo.getAnnotation())) {
                    // skip completely
                    return;
                }
                constructorInfo = classPatchInfo.getReplacementConstructorInfo(descriptor);
                injectedInfo = addedConstructors.get(descriptor);
                annotateInfo = classPatchInfo.getAnnotatedConstructorInfo(descriptor);
            }
            if (constructorInfo != null && ce.evaluateConditions(classContext, constructorInfo, constructorInfo.getAnnotation())) {
                resolver = new PatcherConstructorResolver(constructorInfo);
                index = constructorInfo.getIndex();
            } else if (injectedInfo != null && ce.evaluateConditions(classContext, injectedInfo, injectedInfo.getAnnotation())) {
                classContext.getCompilationContext().warning("Injected constructor %s was already present on %s (replacing)", descriptor, internalName);
                return;
            }
            if (annotateInfo != null && ce.evaluateConditions(classContext, annotateInfo, annotateInfo.getAnnotation())) {
                resolver = new AnnotationAddingResolver(annotateInfo.getAddedAnnotations(), resolver);
            }
            getDelegate().addConstructor(resolver, index, descriptor);
        } else {
            getDelegate().addConstructor(resolver, index, descriptor);
        }
    }

    @Override
    public void addMethod(MethodResolver resolver, int index, String name, MethodDescriptor descriptor) {
        ClassPatchInfo classPatchInfo = this.classPatchInfo;
        if (classPatchInfo != null) {
            ConditionEvaluation ce = ConditionEvaluation.get(classContext.getCompilationContext());
            MethodPatchInfo methodInfo;
            MethodPatchInfo annotateInfo;
            MethodPatchInfo injectedInfo;
            MethodBodyPatchInfo bodyPatchInfo;
            synchronized (classPatchInfo) {
                MethodDeleteInfo delInfo = classPatchInfo.getDeletedMethodInfo(name, descriptor);
                if (delInfo != null && ce.evaluateConditions(classContext, delInfo, delInfo.getAnnotation())) {
                    // skip completely
                    return;
                }
                methodInfo = classPatchInfo.getReplacementMethodInfo(name, descriptor);
                injectedInfo = addedMethods.getOrDefault(name, Map.of()).get(descriptor);
                annotateInfo = classPatchInfo.getAnnotatedMethodInfo(name, descriptor);
                bodyPatchInfo = classPatchInfo.getReplacementMethodBodyInfo(name, descriptor);
            }
            if (methodInfo != null && ce.evaluateConditions(classContext, methodInfo, methodInfo.getAnnotation())) {
                resolver = methodInfo.getMethodResolver();
                index = methodInfo.getIndex();
            } else if (injectedInfo != null) {
                classContext.getCompilationContext().warning("Injected method %s%s was already present on %s (replacing)", name, descriptor, internalName);
                return;
            }
            if (annotateInfo != null && ce.evaluateConditions(classContext, annotateInfo, annotateInfo.getAnnotation())) {
                resolver = new AnnotationAddingResolver(annotateInfo.getAddedAnnotations(), resolver);
            }
            if (bodyPatchInfo != null) {
                resolver = new MethodBodyReplacingResolver(bodyPatchInfo.getMethodBodyFactory(), bodyPatchInfo.getIndex(), resolver);
            }
            getDelegate().addMethod(resolver, index, name, descriptor);
        } else {
            getDelegate().addMethod(resolver, index, name, descriptor);
        }
    }

    @Override
    public DefinedTypeDefinition build() {
        // add injected members
        ClassPatchInfo classPatchInfo = this.classPatchInfo;
        if (classPatchInfo != null) {
            ConditionEvaluation ce = ConditionEvaluation.get(classContext.getCompilationContext());
            synchronized (classPatchInfo) {
                for (FieldPatchInfo fieldInfo : classPatchInfo.getInjectedFields()) {
                    // inject
                    FieldResolver resolver = new PatcherFieldResolver(fieldInfo);
                    RuntimeInitializerPatchInfo initInfo = classPatchInfo.getRuntimeInitFieldInfo(fieldInfo.getName(), fieldInfo.getDescriptor());
                    if (initInfo != null) {
                        resolver = new PatcherFieldRuntimeInitResolver(initInfo, resolver);
                    }
                    if (ce.evaluateConditions(classContext, fieldInfo, fieldInfo.getAnnotation())) {
                        getDelegate().addField(resolver, fieldInfo.getIndex(), fieldInfo.getName(), fieldInfo.getDescriptor());
                    }
                }
                for (ConstructorPatchInfo ctorInfo : classPatchInfo.getInjectedConstructors()) {
                    // inject
                    if (ce.evaluateConditions(classContext, ctorInfo, ctorInfo.getAnnotation())) {
                        getDelegate().addConstructor(new PatcherConstructorResolver(ctorInfo), ctorInfo.getIndex(), ctorInfo.getDescriptor());
                    }
                }
                for (MethodPatchInfo methodInfo : classPatchInfo.getInjectedMethods()) {
                    // inject
                    if (ce.evaluateConditions(classContext, methodInfo, methodInfo.getAnnotation())) {
                        getDelegate().addMethod(new PatcherMethodResolver(methodInfo), methodInfo.getIndex(), methodInfo.getName(), methodInfo.getDescriptor());
                    }
                }
            }
        }
        return getDelegate().build();
    }

    static class PatcherMethodResolver implements MethodResolver {
        private final MethodPatchInfo methodInfo;

        PatcherMethodResolver(final MethodPatchInfo methodInfo) {
            this.methodInfo = methodInfo;
        }

        @Override
        public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
            MethodElement methodElement = methodInfo.getMethodResolver().resolveMethod(index, enclosing, builder);
            methodElement.setModifierFlags(methodInfo.getAdditionalModifiers());
            return methodElement;
        }
    }

    static class PatcherConstructorResolver implements ConstructorResolver {
        private final ConstructorPatchInfo constructorInfo;

        PatcherConstructorResolver(final ConstructorPatchInfo constructorInfo) {
            this.constructorInfo = constructorInfo;
        }

        @Override
        public ConstructorElement resolveConstructor(int index, DefinedTypeDefinition enclosing, ConstructorElement.Builder builder) {
            ConstructorElement constructorElement = constructorInfo.getConstructorResolver().resolveConstructor(index, enclosing, builder);
            constructorElement.setModifierFlags(constructorInfo.getAdditionalModifiers());
            return constructorElement;
        }
    }

    static class PatcherFieldResolver implements FieldResolver {
        private final FieldPatchInfo fieldInfo;

        PatcherFieldResolver(final FieldPatchInfo fieldInfo) {
            this.fieldInfo = fieldInfo;
        }

        @Override
        public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
            FieldElement fieldElement = fieldInfo.getFieldResolver().resolveField(index, enclosing, builder);
            fieldElement.setModifierFlags(fieldInfo.getAdditionalModifiers());
            return fieldElement;
        }
    }

    static class PatcherFieldRuntimeInitResolver implements FieldResolver {
        private final RuntimeInitializerPatchInfo initInfo;
        private final FieldResolver fieldResolver;

        PatcherFieldRuntimeInitResolver(final RuntimeInitializerPatchInfo initInfo, FieldResolver fieldResolver) {
            this.initInfo = initInfo;
            this.fieldResolver = fieldResolver;
        }

        @Override
        public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
            InitializerElement rtInit = initInfo.getInitializerResolver().resolveInitializer(initInfo.getInitializerResolverIndex(), enclosing, InitializerElement.builder());
            builder.setRunTimeInitializer(rtInit);
            return fieldResolver.resolveField(index, enclosing, builder);
        }
    }

    static class AnnotationAddingResolver implements FieldResolver, MethodResolver, ConstructorResolver {
        private final List<Annotation> additions;
        private final ConstructorResolver constructorResolver;
        private final FieldResolver fieldResolver;
        private final MethodResolver methodResolver;

        AnnotationAddingResolver(final List<Annotation> additions, ConstructorResolver constructorResolver, FieldResolver fieldResolver, MethodResolver methodResolver) {
            this.additions = additions;
            this.constructorResolver = constructorResolver;
            this.fieldResolver = fieldResolver;
            this.methodResolver = methodResolver;
        }

        AnnotationAddingResolver(final List<Annotation> additions, ConstructorResolver constructorResolver) {
            this(additions, constructorResolver, null, null);
        }

        AnnotationAddingResolver(final List<Annotation> additions, FieldResolver fieldResolver) {
            this(additions, null, fieldResolver, null);
        }

        AnnotationAddingResolver(final List<Annotation> additions, MethodResolver methodResolver) {
            this(additions, null, null, methodResolver);
        }

        @Override
        public ConstructorElement resolveConstructor(int index, DefinedTypeDefinition enclosing, ConstructorElement.Builder builder) {
            builder.addInvisibleAnnotations(additions);
            return constructorResolver.resolveConstructor(index, enclosing, builder);
        }

        @Override
        public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
            builder.addInvisibleAnnotations(additions);
            return fieldResolver.resolveField(index, enclosing, builder);
        }

        @Override
        public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
            builder.addInvisibleAnnotations(additions);
            return methodResolver.resolveMethod(index, enclosing, builder);
        }
    }

    static class MethodBodyReplacingResolver implements MethodResolver {
        private final MethodBodyFactory methodBodyFactory;
        private final int index;
        private final MethodResolver resolver;

        MethodBodyReplacingResolver(final MethodBodyFactory methodBodyFactory, final int index, final MethodResolver resolver) {
            this.methodBodyFactory = methodBodyFactory;
            this.index = index;
            this.resolver = resolver;
        }

        @Override
        public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
            return resolver.resolveMethod(index, enclosing, new MethodElement.Builder.Delegating() {
                @Override
                public MethodElement.Builder getDelegate() {
                    return builder;
                }

                @Override
                public MethodElement build() {
                    MethodElement.Builder delegate = getDelegate();
                    delegate.setMethodBodyFactory(methodBodyFactory, MethodBodyReplacingResolver.this.index);
                    return delegate.build();
                }
            });
        }
    }

    /**
     * Utility to create a compact map from a compact list.
     *
     * @param list the list of values
     * @param mapper the key mapping function
     * @param <K> the key type
     * @param <V> the value type
     * @return the map
     */
    static <K, V> Map<K, V> mapOf(List<V> list, Function<V, K> mapper) {
        Iterator<V> iterator = list.iterator();
        if (! iterator.hasNext()) {
            return Map.of();
        }
        V v1 = iterator.next();
        K k1 = mapper.apply(v1);
        if (! iterator.hasNext()) {
            return Map.of(k1, v1);
        }
        V v2 = iterator.next();
        K k2 = mapper.apply(v2);
        if (! iterator.hasNext()) {
            return Map.of(k1, v1, k2, v2);
        }
        V v3 = iterator.next();
        K k3 = mapper.apply(v3);
        if (! iterator.hasNext()) {
            return Map.of(k1, v1, k2, v2, k3, v3);
        }
        V v4 = iterator.next();
        K k4 = mapper.apply(v4);
        if (! iterator.hasNext()) {
            return Map.of(k1, v1, k2, v2, k3, v3, k4, v4);
        }
        V v5 = iterator.next();
        K k5 = mapper.apply(v5);
        if (! iterator.hasNext()) {
            return Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        }
        HashMap<K, V> map = new HashMap<>(list.size());
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        while (iterator.hasNext()) {
            V v = iterator.next();
            map.put(mapper.apply(v), v);
        }
        return map;
    }

    static <K, L, V> Map<K, Map<L, V>> mapOf(List<V> list, Function<V, K> outerMapper, Function<V, L> innerMapper) {
        Iterator<V> iterator = list.iterator();
        if (! iterator.hasNext()) {
            return Map.of();
        }
        V v1 = iterator.next();
        K k1 = outerMapper.apply(v1);
        L l1 = innerMapper.apply(v1);
        if (! iterator.hasNext()) {
            return Map.of(k1, Map.of(l1, v1));
        }
        V v2 = iterator.next();
        K k2 = outerMapper.apply(v2);
        L l2 = innerMapper.apply(v2);
        if (! iterator.hasNext()) {
            if (k2.equals(k1)) {
                return Map.of(k1, Map.of(l1, v1, l2, v2));
            } else {
                return Map.of(k1, Map.of(l1, v1), k2, Map.of(l2, v2));
            }
        }
        V v3 = iterator.next();
        K k3 = outerMapper.apply(v3);
        L l3 = innerMapper.apply(v3);
        if (! iterator.hasNext()) {
            if (k3.equals(k2) && k2.equals(k1)) {
                return Map.of(k1, Map.of(l1, v1, l2, v2, l3, v3));
            } else if (k3.equals(k2)) {
                return Map.of(k1, Map.of(l1, v1), k2, Map.of(l2, v2, l3, v3));
            } else if (k2.equals(k1)) {
                return Map.of(k1, Map.of(l1, v1, l2, v2), k3, Map.of(l3, v3));
            } else {
                return Map.of(k1, Map.of(l1, v1), k2, Map.of(l2, v2), k3, Map.of(l3, v3));
            }
        }
        // a fourth key would require many permutations, so just give up and make a basic HashMap
        HashMap<K, Map<L, V>> map = new HashMap<>(list.size());
        map.computeIfAbsent(k1, PatchedTypeBuilder::newMap).put(l1, v1);
        map.computeIfAbsent(k2, PatchedTypeBuilder::newMap).put(l2, v2);
        map.computeIfAbsent(k3, PatchedTypeBuilder::newMap).put(l3, v3);
        while (iterator.hasNext()) {
            V v = iterator.next();
            map.computeIfAbsent(outerMapper.apply(v), PatchedTypeBuilder::newMap).put(innerMapper.apply(v), v);
        }
        return map;
    }

    private static <K, V> Map<K, V> newMap(Object ignored) {
        return new HashMap<>();
    }
}
