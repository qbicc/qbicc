package org.qbicc.plugin.patcher;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

public class Patcher {
    static final String PATCHER_PKG = "org/qbicc/runtime/patcher";

    private static final AttachmentKey<Patcher> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Map<ClassContext, ClassContextPatchInfo> patchInfoMap = new ConcurrentHashMap<>();
    private final Map<FieldElement, VmObject> accessors = new ConcurrentHashMap<>();

    private Patcher(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static Patcher get(CompilationContext ctxt) {
        Patcher patcher = ctxt.getAttachment(KEY);
        if (patcher == null) {
            patcher = new Patcher(ctxt);
            Patcher appearing = ctxt.putAttachmentIfAbsent(KEY, patcher);
            if (appearing != null) {
                patcher = appearing;
            }
        }
        return patcher;
    }

    public static void initialize(ClassContext classContext) {
        Patcher patcher = get(classContext.getCompilationContext());
        ClassContextPatchInfo contextInfo = patcher.getOrAdd(classContext);
        byte[] patchInfo = classContext.getResource("META-INF/qbicc/qbicc-patch-info");
        if (patchInfo != null) {
            try (ByteArrayInputStream is = new ByteArrayInputStream(patchInfo)) {
                try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    try (BufferedReader reader = new BufferedReader(isr)) {
                        Iterator<String> iterator = new Iterator<>() {
                            String next;

                            public boolean hasNext() {
                                while (next == null) {
                                    String line;
                                    try {
                                        line = reader.readLine();
                                    } catch (IOException e) {
                                        // should be impossible
                                        throw new IOError(e);
                                    }
                                    if (line == null) {
                                        return false;
                                    }
                                    int commentIdx = line.indexOf('#');
                                    if (commentIdx != -1) {
                                        line = line.substring(0, commentIdx);
                                    }
                                    line = line.trim();
                                    if (!line.isEmpty()) {
                                        next = line;
                                        break;
                                    }
                                }
                                return true;
                            }

                            public String next() {
                                if (!hasNext()) {
                                    throw new NoSuchElementException();
                                }
                                try {
                                    return next;
                                } finally {
                                    next = null;
                                }
                            }
                        };
                        contextInfo.processClasses(classContext, iterator);
                    }
                }
            } catch (IOException e) {
                classContext.getCompilationContext().error(e, "Unexpected exception while trying to process patch info resource");
                return;
            }
        }
    }

    public static DefinedTypeDefinition.Builder getTypeBuilder(ClassContext classContext, DefinedTypeDefinition.Builder delegate) {
        Patcher patcher = get(classContext.getCompilationContext());
        ClassContextPatchInfo contextInfo = patcher.get(classContext);
        return contextInfo != null ? new PatchedTypeBuilder(classContext, contextInfo, delegate) : delegate;
    }

    ClassContextPatchInfo get(ClassContext classContext) {
        return patchInfoMap.get(classContext);
    }

    ClassContextPatchInfo getOrAdd(ClassContext classContext) {
        ClassContextPatchInfo info = get(classContext);
        if (info == null) {
            info = new ClassContextPatchInfo(classContext);
        }
        ClassContextPatchInfo appearing = patchInfoMap.putIfAbsent(classContext, info);
        if (appearing != null) {
            info = appearing;
        }
        return info;
    }

    /**
     * Add a field to the given class.  The field is not defined until the class is first loaded.  The field may be a
     * build time field or a run time field ({@link org.qbicc.type.definition.classfile.ClassFile#I_ACC_RUN_TIME I_ACC_RUN_TIME}).
     * An initializer may be given for {@code static} fields, but it must be a run time initializer, even if the field
     * is a build time field. Initializers are ignored for instance fields.
     *
     * @param classContext the class context of the class (must not be {@code null})
     * @param internalName the name of the class (must not be {@code null})
     * @param fieldName the name of the field (must not be {@code null})
     * @param descriptor the field descriptor (must not be {@code null})
     * @param resolver the resolver for the added field (must not be {@code null})
     * @param index the index of the added field to pass to the resolver
     * @param addModifiers modifiers to add, if any
     * @param initResolver the initializer resolver, or {@code null} if there is no run time initializer for this field
     * @param initIndex the initializer index
     */
    public void addField(final ClassContext classContext, final String internalName, final String fieldName, final TypeDescriptor descriptor, final FieldResolver resolver, final int index, int addModifiers, final InitializerResolver initResolver, final int initIndex) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.addField(new FieldPatchInfo(internalName, index, addModifiers, initResolver, initIndex, resolver, descriptor, fieldName, null));
        }
    }

    /**
     * Delete a field.
     *
     * @param classContext the class context of the class (must not be {@code null})
     * @param internalName the name of the class (must not be {@code null})
     * @param fieldName the name of the field (must not be {@code null})
     * @param descriptor the field descriptor (must not be {@code null})
     */
    public void deleteField(final ClassContext classContext, final String internalName, final String fieldName, final TypeDescriptor descriptor) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.deleteField(fieldName, descriptor, internalName, null);
        }
    }

    /**
     * Add a field to the given class.  The field is not defined until the class is first loaded.  The field may be a
     * build time field or a run time field ({@link org.qbicc.type.definition.classfile.ClassFile#I_ACC_RUN_TIME I_ACC_RUN_TIME}).
     * An initializer may be given for {@code static} fields, but it must be a run time initializer, even if the field
     * is a build time field. Initializers are ignored for instance fields.
     *
     * @param classContext the class context of the class (must not be {@code null})
     * @param internalName the name of the class (must not be {@code null})
     * @param fieldName the name of the field (must not be {@code null})
     * @param descriptor the field descriptor (must not be {@code null})
     * @param resolver the resolver for the added field (must not be {@code null})
     * @param index the index of the added field to pass to the resolver
     * @param addModifiers modifiers to add, if any
     * @param initResolver the initializer resolver, or {@code null} if there is no run time initializer for this field
     * @param initIndex the initializer index
     */
    public void replaceField(final ClassContext classContext, final String internalName, final String fieldName, final TypeDescriptor descriptor, final FieldResolver resolver, final int index, int addModifiers, final InitializerResolver initResolver, final int initIndex) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.replaceField(new FieldPatchInfo(internalName, index, addModifiers, initResolver, initIndex, resolver, descriptor, fieldName, null));
        }
    }

    public void addConstructor(final ClassContext classContext, final String internalName, final MethodDescriptor descriptor, final ConstructorResolver resolver, final int index, int addModifiers) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.addConstructor(new ConstructorPatchInfo(index, addModifiers, resolver, descriptor, internalName, null));
        }
    }

    public void deleteConstructor(final ClassContext classContext, final String internalName, final MethodDescriptor descriptor) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.deleteConstructor(descriptor, internalName, null);
        }
    }

    public void replaceConstructor(final ClassContext classContext, final String internalName, final MethodDescriptor descriptor, final ConstructorResolver resolver, final int index, int addModifiers) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.replaceConstructor(new ConstructorPatchInfo(index, addModifiers, resolver, descriptor, internalName, null));
        }
    }

    public void addMethod(final ClassContext classContext, final String internalName, final String methodName, final MethodDescriptor descriptor, final MethodResolver resolver, final int index, int addModifiers) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.addMethod(new MethodPatchInfo(index, addModifiers, resolver, descriptor, methodName, internalName, null));
        }
    }

    public void deleteMethod(final ClassContext classContext, final String internalName, final String methodName, final MethodDescriptor descriptor) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.deleteMethod(methodName, descriptor, internalName, null);
        }
    }

    public void replaceMethod(final ClassContext classContext, final String internalName, final String methodName, final MethodDescriptor descriptor, final MethodResolver resolver, final int index, int addModifiers) {
        ClassPatchInfo classInfo = getOrAdd(classContext).getOrAdd(internalName);
        synchronized (classInfo) {
            classInfo.replaceMethod(new MethodPatchInfo(index, addModifiers, resolver, descriptor, methodName, internalName, null));
        }
    }

    void registerAccessor(FieldElement element, VmObject accessor) {
        accessors.putIfAbsent(element, accessor);
    }

    VmObject lookUpAccessor(FieldElement element) {
        return accessors.get(element);
    }
}
