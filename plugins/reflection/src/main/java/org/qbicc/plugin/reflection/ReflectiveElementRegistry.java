package org.qbicc.plugin.reflection;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmThread;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The central registry to track program elements that
 * may be accessed reflectively at runtime.
 */
public class ReflectiveElementRegistry {
    private static final AttachmentKey<ReflectiveElementRegistry> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;

    // These are the "early" mappings which can be added to in the earliest preHooks to ADD
    // All entries should be made before qbicc begins loading classes.
    private final Map<String, ClassInfo> reflectiveClasses = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reflectiveConstructors = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reflectiveFields = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reflectiveMethods = new ConcurrentHashMap<>();

    // These are the "late" mappings which are populated during the ADD phase
    // as classes are loaded and reachable methods are compiled and analyzed
    private final Set<MethodElement> reflectiveMethodElements = ConcurrentHashMap.newKeySet();
    private final Set<ConstructorElement> reflectiveConstructorElements = ConcurrentHashMap.newKeySet();
    private final Set<FieldElement> reflectiveFieldElements = ConcurrentHashMap.newKeySet();
    private final Set<LoadedTypeDefinition> reflectiveLoadedTypes = ConcurrentHashMap.newKeySet();

    private ReflectiveElementRegistry(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static ReflectiveElementRegistry get(CompilationContext ctxt) {
        ReflectiveElementRegistry patcher = ctxt.getAttachment(KEY);
        if (patcher == null) {
            patcher = new ReflectiveElementRegistry(ctxt);
            ReflectiveElementRegistry appearing = ctxt.putAttachmentIfAbsent(KEY, patcher);
            if (appearing != null) {
                patcher = appearing;
            }
        }
        return patcher;
    }

    /*
     * Early phase mapping methods
     */

    public void addReflectiveClass(String internalName, boolean fields, boolean methods, boolean constructors) {
        ClassInfo prior = reflectiveClasses.putIfAbsent(internalName, new ClassInfo(fields, methods, constructors));
        if (prior != null) {
            prior.fields |= fields;
            prior.methods |= methods;
            prior.constructors |= constructors;
        }
    }

    boolean isReflectiveClass(String internalName) {
        return reflectiveClasses.containsKey(internalName);
    }

    public void addReflectiveConstructor(String className, String[] parameterTypes) {
        reflectiveConstructors.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(encodeArguments(parameterTypes));
    }

    boolean hasReflectiveConstructors(String className) {
        ClassInfo ci = reflectiveClasses.get(className);
        return (ci != null && ci.constructors) || reflectiveConstructors.containsKey(className);
    }

    boolean isReflectiveConstructor(String className, MethodDescriptor descriptor) {
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

    boolean hasReflectiveFields(String className) {
        ClassInfo ci = reflectiveClasses.get(className);
        return (ci != null && ci.fields) || reflectiveFields.containsKey(className);
    }

    boolean isReflectiveField(String className, String fieldName) {
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

    boolean hasReflectiveMethods(String className) {
        ClassInfo ci = reflectiveClasses.get(className);
        return (ci != null && ci.methods) || reflectiveMethods.containsKey(className);
    }

    boolean isReflectiveMethod(String className, String methodName, MethodDescriptor descriptor) {
        ClassInfo ci = reflectiveClasses.get(className);
        if (ci != null && ci.methods) {
            return true;
        }
        Set<String> encodedMethods = reflectiveMethods.get(className);
        if (encodedMethods != null) {
            for (String candidate : encodedMethods) {
                String[] split = candidate.split(":");
                if (methodName.equals(split[0]) && matchesArguments(split.length == 1 ? "" : split[1], descriptor.getParameterTypes())) {
                    return true;
                }
            }
        }
        return false;
    }


    /*
     * Late phase mapping methods
     */
    public void registerReflectiveType(LoadedTypeDefinition ltd) {
        reflectiveLoadedTypes.add(ltd);
    }

    public boolean isReflectiveType(LoadedTypeDefinition ltd) {
        return reflectiveLoadedTypes.contains(ltd);
    }

    public boolean registerReflectiveMethod(MethodElement e) {
        boolean added = reflectiveMethodElements.add(e);
        if (added) {
            ReachabilityRoots.get(ctxt).registerReflectiveEntrypoint(e);
            ctxt.submitTask(e, methodElement -> {
                Vm vm = ctxt.getVm();
                VmThread thr = vm.newThread("genMethodAccessor", vm.getMainThreadGroup(), false,  Thread.currentThread().getPriority());
                ctxt.getVm().doAttached(thr, () -> {
                    Reflection.get(ctxt).makeAvailableForRuntimeReflection(methodElement);
                });
            });
        }
        return added;
    }

    public boolean registerReflectiveConstructor(ConstructorElement e) {
        boolean added = reflectiveConstructorElements.add(e);
        if (added) {
            ReachabilityRoots.get(ctxt).registerReflectiveEntrypoint(e);
            ctxt.submitTask(e, constructorElement -> {
                Vm vm = ctxt.getVm();
                VmThread thr = vm.newThread("genConstructorAccessor", vm.getMainThreadGroup(), false,  Thread.currentThread().getPriority());
                ctxt.getVm().doAttached(thr, () -> {
                    Reflection.get(ctxt).makeAvailableForRuntimeReflection(constructorElement);
                });
            });
        }
        return added;
    }

    public boolean registerReflectiveField(FieldElement f) {
        boolean added = reflectiveFieldElements.add(f);
        if (added) {
            if (f.isStatic()) {
                ReachabilityRoots.get(ctxt).registerHeapRoot((StaticFieldElement) f);
            }
            ctxt.submitTask(f, fieldElement -> {
                Vm vm = ctxt.getVm();
                VmThread thr = vm.newThread("genFieldAccessor", vm.getMainThreadGroup(), false,  Thread.currentThread().getPriority());
                ctxt.getVm().doAttached(thr, () -> {
                    Reflection.get(ctxt).makeAvailableForRuntimeReflection(fieldElement);
                });
            });
        }
        return added;
    }

    private String encodeArguments(String[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        if (args.length == 1 && args[0].equals("*")) {
            return "*"; // wildcard -- matches all args
        }
        StringBuilder ans = new StringBuilder(toDescriptorString(args[0]));
        for (int i = 1; i<args.length; i++) {
            ans.append(",").append(toDescriptorString(args[i]));
        }
        return ans.toString();
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
            default -> t.startsWith("[") ? t : "L"+t.replace('.','/')+";";
        };
    }

    private boolean matchesArguments(String encodedArgs, List<TypeDescriptor> paramTypes) {
        if (paramTypes.isEmpty() && encodedArgs.equals("")) {
            return true;
        }
        String[] args = encodedArgs.split(",");
        if (args.length == 1 && args[0].equals("*")) {
            return true;
        }
        if (args.length != paramTypes.size()) {
            return false;
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
