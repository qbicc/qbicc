package org.qbicc.type.descriptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.ClassContext;

/**
 *
 */
final class Cache {
    private static final AttachmentKey<Cache> KEY = new AttachmentKey<>();

    private final Map<String, Map<String, ClassTypeDescriptor>> classTypes = new ConcurrentHashMap<>();
    private final Map<TypeDescriptor, ArrayTypeDescriptor> arrayTypes = new ConcurrentHashMap<>();
    private final Map<TypeDescriptor, Map<List<TypeDescriptor>, MethodDescriptor>> methods = new ConcurrentHashMap<>();

    private Cache() {}

    static Cache get(ClassContext classContext) {
        return get(classContext.getCompilationContext());
    }

    static Cache get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Cache::new);
    }

    ClassTypeDescriptor getClassTypeDescriptor(final String packageName, final String internalName) {
        return classTypes
            .computeIfAbsent(packageName, Cache::newMap)
            .computeIfAbsent(internalName, i -> new ClassTypeDescriptor(packageName, i));
    }

    ArrayTypeDescriptor getArrayTypeDescriptor(final TypeDescriptor elementType) {
        return arrayTypes.computeIfAbsent(elementType, ArrayTypeDescriptor::new);
    }

    MethodDescriptor getMethodDescriptor(final List<TypeDescriptor> parameterTypes, final TypeDescriptor returnType) {
        return methods
            .computeIfAbsent(returnType, Cache::newMap)
            .computeIfAbsent(parameterTypes, p -> new MethodDescriptor(p, returnType));
    }

    private static <K, V> Map<K, V> newMap(final Object key) {
        return new ConcurrentHashMap<>();
    }
}
