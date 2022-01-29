package org.qbicc.plugin.patcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

final class ClassPatchInfo {

    static final ClassPatchInfo EMPTY = new ClassPatchInfo(0);

    private boolean committed;
    private Map<String, FieldPatchInfo> annotatedFields;
    private Map<String, FieldPatchInfo> replacedFields;
    private List<FieldPatchInfo> injectedFields;
    private Map<String, FieldDeleteInfo> deletedFields;
    private Map<String, RuntimeInitializerPatchInfo> runtimeInitFields;
    private Map<MethodDescriptor, ConstructorPatchInfo> annotatedConstructors;
    private Map<MethodDescriptor, ConstructorPatchInfo> replacedConstructors;
    private List<ConstructorPatchInfo> injectedConstructors;
    private Map<MethodDescriptor, ConstructorDeleteInfo> deletedConstructors;
    private Map<String, Map<MethodDescriptor, MethodPatchInfo>> annotatedMethods;
    private Map<String, Map<MethodDescriptor, MethodPatchInfo>> replacedMethods;
    private List<MethodPatchInfo> injectedMethods;
    private Map<String, Map<MethodDescriptor, MethodDeleteInfo>> deletedMethods;
    private InitializerPatchInfo replacedInitializer;
    private boolean deletedInitializer;

    ClassPatchInfo() {
        annotatedFields = Map.of();
        replacedFields = Map.of();
        injectedFields = List.of();
        deletedFields = Map.of();
        runtimeInitFields = Map.of();
        annotatedConstructors = Map.of();
        replacedConstructors = Map.of();
        injectedConstructors = List.of();
        deletedConstructors = Map.of();
        annotatedMethods = Map.of();
        replacedMethods = Map.of();
        injectedMethods = List.of();
        deletedMethods = Map.of();
    }

    ClassPatchInfo(int ignored) {
        this();
        committed = true;
    }

    ClassPatchInfo(final String internalName) {
        this();
    }

    void commit() {
        assert Thread.holdsLock(this);
        committed = true;
    }

    // remove

    FieldDeleteInfo getDeletedFieldInfo(final String name, final TypeDescriptor descriptor) {
        assert Thread.holdsLock(this);
        FieldDeleteInfo info = deletedFields.get(name);
        return info == null ? null : info.getDescriptor().equals(descriptor) ? info : null;
    }

    ConstructorDeleteInfo getDeletedConstructorInfo(final MethodDescriptor descriptor) {
        assert Thread.holdsLock(this);
        return deletedConstructors.get(descriptor);
    }

    MethodDeleteInfo getDeletedMethodInfo(final String name, final MethodDescriptor descriptor) {
        assert Thread.holdsLock(this);
        return deletedMethods.getOrDefault(name, Map.of()).get(descriptor);
    }

    boolean isDeletedInitializer() {
        assert Thread.holdsLock(this);
        return deletedInitializer;
    }

    // replace

    FieldPatchInfo getReplacementFieldInfo(final String fieldName, TypeDescriptor descriptor) {
        FieldPatchInfo fieldPatchInfo = replacedFields.get(fieldName);
        return fieldPatchInfo != null && fieldPatchInfo.getDescriptor().equals(descriptor) ? fieldPatchInfo : null;
    }

    ConstructorPatchInfo getReplacementConstructorInfo(final MethodDescriptor descriptor) {
        assert Thread.holdsLock(this);
        return replacedConstructors.get(descriptor);
    }

    MethodPatchInfo getReplacementMethodInfo(final String name, final MethodDescriptor descriptor) {
        assert Thread.holdsLock(this);
        return replacedMethods.getOrDefault(name, Map.of()).get(descriptor);
    }

    InitializerPatchInfo getReplacementInitializerInfo() {
        assert Thread.holdsLock(this);
        return replacedInitializer;
    }

    // add

    List<FieldPatchInfo> getInjectedFields() {
        assert Thread.holdsLock(this);
        return injectedFields;
    }

    List<ConstructorPatchInfo> getInjectedConstructors() {
        assert Thread.holdsLock(this);
        return injectedConstructors;
    }

    List<MethodPatchInfo> getInjectedMethods() {
        assert Thread.holdsLock(this);
        return injectedMethods;
    }

    // modify
    RuntimeInitializerPatchInfo getRuntimeInitFieldInfo(final String fieldName, TypeDescriptor descriptor) {
        RuntimeInitializerPatchInfo rtInitPatchInfo = runtimeInitFields.get(fieldName);
        return rtInitPatchInfo != null && rtInitPatchInfo.getDescriptor().equals(descriptor) ? rtInitPatchInfo : null;
    }

    FieldPatchInfo getAnnotatedFieldInfo(final String fieldName, TypeDescriptor descriptor) {
        FieldPatchInfo fieldPatchInfo = annotatedFields.get(fieldName);
        return fieldPatchInfo != null && fieldPatchInfo.getDescriptor().equals(descriptor) ? fieldPatchInfo : null;
    }

    ConstructorPatchInfo getAnnotatedConstructorInfo(final MethodDescriptor descriptor) {
        assert Thread.holdsLock(this);
        return annotatedConstructors.get(descriptor);
    }

    MethodPatchInfo getAnnotatedMethodInfo(final String name, final MethodDescriptor descriptor) {
        assert Thread.holdsLock(this);
        return annotatedMethods.getOrDefault(name, Map.of()).get(descriptor);
    }

    // Registration methods

    void addField(final FieldPatchInfo fieldPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        injectedFields = listWith(injectedFields, fieldPatchInfo);
    }

    void deleteField(final String name, final TypeDescriptor descriptor, String internalName, Annotation annotation) {
        assert Thread.holdsLock(this);
        checkCommitted();
        deletedFields = mapWith(deletedFields, name, new FieldDeleteInfo(internalName, descriptor, name, annotation));
    }

    void replaceField(final FieldPatchInfo fieldPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        final String name = fieldPatchInfo.getName();
        replacedFields = mapWith(replacedFields, name, fieldPatchInfo);
    }

    void annotateField(final FieldPatchInfo fieldPatchInfo) {
        assert  Thread.holdsLock(this);
        checkCommitted();
        final String name = fieldPatchInfo.getName();
        annotatedFields = mapWith(annotatedFields, name, fieldPatchInfo);
    }

    void runtimeInitField(final RuntimeInitializerPatchInfo runtimeInitPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        final String name = runtimeInitPatchInfo.getName();
        runtimeInitFields = mapWith(runtimeInitFields, name, runtimeInitPatchInfo);
    }

    void addConstructor(final ConstructorPatchInfo constructorPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        injectedConstructors = listWith(injectedConstructors, constructorPatchInfo);
    }

    void deleteConstructor(final MethodDescriptor descriptor, String internalName, Annotation annotation) {
        assert Thread.holdsLock(this);
        checkCommitted();
        deletedConstructors = mapWith(deletedConstructors, descriptor, new ConstructorDeleteInfo(internalName, descriptor, annotation));
    }

    void replaceConstructor(final ConstructorPatchInfo constructorPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        final MethodDescriptor descriptor = constructorPatchInfo.getDescriptor();
        replacedConstructors = mapWith(replacedConstructors, descriptor, constructorPatchInfo);
    }

    void annotateConstructor(final ConstructorPatchInfo constructorPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        final MethodDescriptor descriptor = constructorPatchInfo.getDescriptor();
        annotatedConstructors = mapWith(annotatedConstructors, descriptor, constructorPatchInfo);
    }

    void addMethod(final MethodPatchInfo methodPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        injectedMethods = listWith(injectedMethods, methodPatchInfo);
    }

    void deleteMethod(final String name, final MethodDescriptor descriptor, String internalName, Annotation annotation) {
        assert Thread.holdsLock(this);
        checkCommitted();
        deletedMethods = mapWith(deletedMethods, name, mapWith(deletedMethods.getOrDefault(name, Map.of()), descriptor, new MethodDeleteInfo(internalName, name, descriptor, annotation)));
    }

    void replaceMethod(final MethodPatchInfo methodPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        final String name = methodPatchInfo.getName();
        final MethodDescriptor descriptor = methodPatchInfo.getDescriptor();
        replacedMethods = mapWith(replacedMethods, name, mapWith(replacedMethods.getOrDefault(name, Map.of()), descriptor, methodPatchInfo));
    }

    void annotateMethod(final MethodPatchInfo methodPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        final String name = methodPatchInfo.getName();
        final MethodDescriptor descriptor = methodPatchInfo.getDescriptor();
        annotatedMethods = mapWith(annotatedMethods, name, mapWith(annotatedMethods.getOrDefault(name, Map.of()), descriptor, methodPatchInfo));
    }

    void deleteInitializer() {
        assert Thread.holdsLock(this);
        deletedInitializer = true;
    }

    void replaceInitializer(final InitializerPatchInfo initializerPatchInfo) {
        assert Thread.holdsLock(this);
        checkCommitted();
        replacedInitializer = initializerPatchInfo;
    }

    private void checkCommitted() {
        if (committed) {
            throw new IllegalStateException("Class already loaded");
        }
    }

    private <K, V> Map<K, V> mapWith(Map<K, V> orig, K key, V val) {
        int size = orig.size();
        if (orig instanceof HashMap) {
            // already exploded
            orig.put(key, val);
            return orig;
        } else if (size == 0 || size == 1 && orig.containsKey(key)) {
            return Map.of(key, val);
        } else {
            // explode it
            Map<K, V> map = new HashMap<>(orig);
            map.put(key, val);
            return map;
        }
    }

    private <E> Set<E> setWith(Set<E> orig, E elem) {
        int size = orig.size();
        if (orig instanceof HashSet) {
            // already exploded
            orig.add(elem);
            return orig;
        } else if (size == 0 || size == 1 && orig.contains(elem)) {
            return Set.of(elem);
        } else {
            // explode it
            Set<E> set = new HashSet<>(orig);
            set.add(elem);
            return set;
        }
    }

    private <E> List<E> listWith(List<E> orig, E elem) {
        int size = orig.size();
        if (orig instanceof ArrayList) {
            // already exploded
            orig.add(elem);
            return orig;
        } else if (size == 0) {
            return List.of(elem);
        } else if (size == 1) {
            return List.of(orig.get(0), elem);
        } else if (size == 2) {
            return List.of(orig.get(0), orig.get(1), elem);
        } else {
            // explode it
            List<E> list = new ArrayList<>(orig);
            list.add(elem);
            return list;
        }
    }
}
