package org.qbicc.plugin.initializationcontrol;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.descriptor.MethodDescriptor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FeaturePatcher {

    private static final AttachmentKey<FeaturePatcher> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Set<String> runtimeInitializedClasses = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> reflectiveConstructors = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reflectiveFields = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reflectiveMethods = new ConcurrentHashMap<>();

    private FeaturePatcher(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static FeaturePatcher get(CompilationContext ctxt) {
        FeaturePatcher patcher = ctxt.getAttachment(KEY);
        if (patcher == null) {
            patcher = new FeaturePatcher(ctxt);
            FeaturePatcher appearing = ctxt.putAttachmentIfAbsent(KEY, patcher);
            if (appearing != null) {
                patcher = appearing;
            }
        }
        return patcher;
    }

    public void addRuntimeInitializedClass(String internalName) {
        runtimeInitializedClasses.add(internalName);
    }

    public boolean isRuntimeInitializedClass(String internalName) {
        return runtimeInitializedClasses.contains(internalName);
    }


    public void addReflectiveConstructor(String className, String descriptor) {
        reflectiveConstructors.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(descriptor);
    }

    public boolean hasReflectiveConstructors(String className) {
        return reflectiveConstructors.containsKey(className);
    }

    public boolean isReflectiveConstructor(String className, MethodDescriptor descriptor) {
        Set<String> constructors = reflectiveConstructors.get(className);
        return constructors != null && constructors.contains(descriptor.toString());
    }

    public void addReflectiveField(String className, String fieldName) {
        reflectiveFields.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(fieldName);
    }

    public boolean hasReflectiveFields(String className) {
        return reflectiveFields.containsKey(className);
    }

    public boolean isReflectiveField(String className, String fieldName) {
        Set<String> fields = reflectiveFields.get(className);
        return fields != null && fields.contains(fieldName);
    }

    public void addReflectiveMethod(String className, String methodName, String descriptor) {
        String encodedMethod = methodName+":"+descriptor;
        reflectiveMethods.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(encodedMethod);
    }

    public boolean hasReflectiveMethods(String className) {
        return reflectiveMethods.containsKey(className);
    }

    public boolean isReflectiveMethod(String className, String methodName, MethodDescriptor descriptor) {
        Set<String> encodedMethods = reflectiveMethods.get(className);
        return encodedMethods != null && encodedMethods.contains(methodName+":"+descriptor);
    }
}
