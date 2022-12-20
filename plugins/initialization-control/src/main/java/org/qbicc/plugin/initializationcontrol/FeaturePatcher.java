package org.qbicc.plugin.initializationcontrol;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FeaturePatcher {

    private static final AttachmentKey<FeaturePatcher> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Set<String> runtimeInitializedClasses = ConcurrentHashMap.newKeySet();
    private final Map<String, ClassInfo> reflectiveClasses = new ConcurrentHashMap<>();
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

    public void addReflectiveClass(String internalName, boolean fields, boolean methods, boolean constructors) {
        ClassInfo prior = reflectiveClasses.putIfAbsent(internalName, new ClassInfo(fields, methods, constructors));
        if (prior != null) {
            prior.fields |= fields;
            prior.methods |= methods;
            prior.constructors |= constructors;
        }
    }

    public void addReflectiveConstructor(String className, String[] parameterTypes) {
        reflectiveConstructors.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(encodeArguments(parameterTypes));
    }

    public boolean hasReflectiveConstructors(String className) {
        ClassInfo ci = reflectiveClasses.get(className);
        return (ci != null && ci.constructors) || reflectiveConstructors.containsKey(className);
    }

    public boolean isReflectiveConstructor(String className, MethodDescriptor descriptor) {
        ClassInfo ci = reflectiveClasses.get(className);
        if (ci != null && ci.constructors) {
            return true;
        }
        Set<String> constructors = reflectiveConstructors.get(className);
        if (constructors != null) {
            for (String candidate: constructors) {
                if (matchesArguments(candidate, descriptor.getParameterTypes())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addReflectiveField(String className, String fieldName) {
        reflectiveFields.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(fieldName);
    }

    public boolean hasReflectiveFields(String className) {
        ClassInfo ci = reflectiveClasses.get(className);
        return (ci != null && ci.fields) || reflectiveFields.containsKey(className);
    }

    public boolean isReflectiveField(String className, String fieldName) {
        ClassInfo ci = reflectiveClasses.get(className);
        if (ci != null && ci.fields) {
            return true;
        }
        Set<String> fields = reflectiveFields.get(className);
        return fields != null && fields.contains(fieldName);
    }

    public void addReflectiveMethod(String className, String methodName, String[] parameterTypes) {
        reflectiveMethods.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(methodName+":"+encodeArguments(parameterTypes));
    }

    public boolean hasReflectiveMethods(String className) {
        ClassInfo ci = reflectiveClasses.get(className);
        return (ci != null && ci.methods) || reflectiveMethods.containsKey(className);
    }

    public boolean isReflectiveMethod(String className, String methodName, MethodDescriptor descriptor) {
        ClassInfo ci = reflectiveClasses.get(className);
        if (ci != null && ci.methods) {
            return true;
        }
        Set<String> encodedMethods = reflectiveMethods.get(className);
        if (encodedMethods != null) {
            for (String candidate : encodedMethods) {
                String[] split = candidate.split(":");
                if (methodName.equals(split[0]) && matchesArguments(candidate, descriptor.getParameterTypes())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String encodeArguments(String[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        if (args.length == 1 && args.equals("*")) {
            return "*"; // wildcard -- matches all args
        }
        String ans = toDescriptorString(args[0]);
        for (int i = 1; i<args.length; i++) {
            ans = ans + "," + toDescriptorString(args[i]);
        }
        return ans;
    }

    private String toDescriptorString(String t) {
        return switch (t) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "short" -> "S";
            case "char" -> "C";
            case "int" -> "I";
            case "float" -> "F";
            case "long" -> "J";
            case "double" -> "D";
            // TODO: friendlier processing of array types -- for now we just force the user to provide a descriptor if they want an array
            default -> t.startsWith("[") ? t : "J"+t.replace('.','/')+";";
        };
    }

    private boolean matchesArguments(String encodedArgs, List<TypeDescriptor> paramTypes) {
        String[] args = encodedArgs.split(",");
        if (args.length != paramTypes.size()) {
            return false;
        }
        if (args.length == 0 || (args.length == 1 && args[0].equals("*"))) {
            return true;
        }
        for (int i=0; i<args.length; i++) {
            if (!args[i].equals(paramTypes.get(i).toString())) {
                return false;
            }
        }
        return true;
    }


    private static class ClassInfo {
        boolean fields;
        boolean methods;
        boolean constructors;

        ClassInfo(boolean f, boolean m, boolean c) {
            fields = f;
            methods = m;
            constructors = c;
        }
    }
}
