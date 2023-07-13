package org.qbicc.plugin.patcher;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.ClassContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ClassAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

final class ClassContextPatchInfo {

    private final Map<String, ClassPatchInfo> classPatchInfoMap;
    private final Map<String, String> patchClassMapping;
    private final ClassContext classContext;

    ClassContextPatchInfo(ClassContext classContext) {
        this.classContext = classContext;
        classPatchInfoMap = new ConcurrentHashMap<>();
        patchClassMapping = new ConcurrentHashMap<>();
    }

    boolean isPatchClass(final String internalName) {
        // used to hide patch classes from class loading
        return patchClassMapping.containsKey(internalName);
    }

    String getTargetForPatchClass(final String patchClassName) {
        // used to map patch class descriptors to their original types
        return patchClassMapping.get(patchClassName);
    }

    TypeDescriptor transform(TypeDescriptor desc) {
        if (desc instanceof ClassTypeDescriptor ctd) {
            return transform(ctd);
        } else if (desc instanceof ArrayTypeDescriptor atd) {
            TypeDescriptor etd = atd.getElementTypeDescriptor();
            TypeDescriptor transformed = transform(etd);
            return transformed.equals(etd) ? desc : ArrayTypeDescriptor.of(classContext, transformed);
        } else {
            return desc;
        }
    }

    ArrayTypeDescriptor transform(ArrayTypeDescriptor desc) {
        return ArrayTypeDescriptor.of(classContext, transform(desc.getElementTypeDescriptor()));
    }

    ClassTypeDescriptor transform(ClassTypeDescriptor ctd) {
        String target = getTargetForPatchClass(ctd.getPackageName() + "/" + ctd.getClassName());
        return target == null ? ctd : ClassTypeDescriptor.synthesize(classContext, target);
    }

    MethodDescriptor transform(MethodDescriptor desc) {
        TypeDescriptor returnType = desc.getReturnType();
        TypeDescriptor newReturnType = transform(returnType);
        boolean changed = ! returnType.equals(newReturnType);
        List<TypeDescriptor> parameterTypes = desc.getParameterTypes();
        int cnt = parameterTypes.size();
        TypeDescriptor[] newParamTypes = new TypeDescriptor[cnt];
        for (int i = 0; i < cnt; i ++) {
            TypeDescriptor origDesc = parameterTypes.get(i);
            newParamTypes[i] = transform(origDesc);
            if (! (changed || origDesc.equals(newParamTypes[i]))) {
                changed = true;
            }
        }
        return changed ? MethodDescriptor.synthesize(classContext, newReturnType, List.of(newParamTypes)) : desc;
    }

    /**
     * Get the patch information for the given class internal name.
     *
     * @param internalName the internal name (must not be {@code null})
     * @return the class patch info (not {@code null})
     */
    ClassPatchInfo get(final String internalName) {
        return classPatchInfoMap.getOrDefault(internalName, ClassPatchInfo.EMPTY);
    }

    ClassPatchInfo getOrAdd(final String internalName) {
        return classPatchInfoMap.computeIfAbsent(internalName, ClassPatchInfo::new);
    }

    void processClasses(ClassContext classContext, Iterator<String> iterator) {
        while (iterator.hasNext()) {
            processClass(classContext, iterator.next());
        }
    }

    // kinds of member patch operations
    private static final int K_ALIAS = 0;
    private static final int K_ADD = 1;
    private static final int K_REMOVE = 2;
    private static final int K_REPLACE = 3;
    private static final int K_ANNOTATE = 4;

    void processClass(final ClassContext classContext, final String className) {
        String internalName = className.replace('.', '/');
        byte[] classBytes = classContext.getResource(internalName + ".class");
        if (classBytes == null) {
            classContext.getCompilationContext().error("Patch class %s was not found", className);
            return;
        }
        ClassFile classFile = ClassFile.of(classContext, ByteBuffer.wrap(classBytes));
        // determine whether the class is a run time aspect
        Annotation replaceInit = null;
        InitializerResolver initResolver = null;
        int initIndex = 0;
        int cnt = classFile.getAttributeCount();
        boolean runTimeAspect = false;
        String patchedClassPackage = null;
        String patchedClassName = null;
        List<Annotation> transferredAnnotations = null;
        boolean shouldTransferAnnotations = false;
        for (int i = 0; i < cnt; i ++) {
            if (classFile.attributeNameEquals(i, "RuntimeInvisibleAnnotations")) {
                // found annotations
                ByteBuffer buf = classFile.getRawAttributeContent(i);
                int ac = buf.getShort() & 0xffff;
                for (int j = 0; j < ac; j ++) {
                    Annotation annotation = Annotation.parse(classFile, classContext, buf);
                    ClassTypeDescriptor descriptor = annotation.getDescriptor();
                    if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "RunTimeAspect")) {
                        if (replaceInit != null) {
                            classContext.getCompilationContext().error("Patch class \"%s\" cannot both replace the initializer and provide a run time aspect", className);
                            return;
                        }
                        // this annotation is not conditional
                        runTimeAspect = true;
                    } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Patch") && annotation.getValue("value") instanceof StringAnnotationValue sav) {
                        if (patchedClassName != null) {
                            classContext.getCompilationContext().warning("Patch class \"%s\" has more than one annotation designating the class to patch", className);
                            continue;
                        }

                        String string = sav.getString().replace('.', '/');
                        int idx = string.lastIndexOf('/');
                        if (idx == -1) {
                            patchedClassPackage = "";
                            patchedClassName = string;
                        } else {
                            patchedClassPackage = string.substring(0, idx);
                            patchedClassName = string.substring(idx + 1);
                        }
                    } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "PatchClass") && annotation.getValue("value") instanceof ClassAnnotationValue cav) {
                        if (patchedClassName != null) {
                            classContext.getCompilationContext().warning("Patch class \"%s\" has more than one annotation designating the class to patch", className);
                            continue;
                        }
                        if (cav.getDescriptor() instanceof ClassTypeDescriptor ctd) {
                            patchedClassName = ctd.getClassName();
                            patchedClassPackage = ctd.getPackageName();
                        } else {
                            classContext.getCompilationContext().error("Patch class \"%s\" designates a non-class to patch", className);
                            return;
                        }
                    } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "ReplaceInit")) {
                        if (runTimeAspect) {
                            classContext.getCompilationContext().error("Patch class \"%s\" cannot both replace the initializer and provide a run time aspect", className);
                            return;
                        }
                        replaceInit = annotation;
                    } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Annotate")) {
                        shouldTransferAnnotations = true;
                    } else {
                        if (transferredAnnotations == null) {
                            transferredAnnotations = new ArrayList<>();
                        }
                        transferredAnnotations.add(annotation);
                    }
                }
            }
        }
        if (patchedClassName == null) {
            classContext.getCompilationContext().error("Patch class \"%s\" does not designate a class to patch", className);
            return;
        }
        String patchedClassInternalName = patchedClassPackage.isEmpty() ? patchedClassName : patchedClassPackage + '/' + patchedClassName;
        patchClassMapping.put(internalName, patchedClassInternalName);
        ClassPatchInfo classPatchInfo = getOrAdd(patchedClassInternalName);
        boolean foundInit = false;
        synchronized (classPatchInfo) {
            if (shouldTransferAnnotations) {
                for (Annotation a : transferredAnnotations) {
                    classPatchInfo.addClassAnnotation(a);
                }
            }
            // do methods before fields because included may be the field initializer
            cnt = classFile.getMethodCount();
            // use these modifiers when adding a new method
            int addModifiers = runTimeAspect ? ClassFile.I_ACC_RUN_TIME : 0;
            outer: for (int i = 0; i < cnt; i ++) {
                String patchMethodName = classFile.getMethodName(i);
                String methodName = patchMethodName;
                if (patchMethodName.equals("<clinit>")) {
                    // initializer
                    initResolver = classFile;
                    foundInit = true;
                    if (runTimeAspect) {
                        // wrap the resolver so we only resolve one time when multiple fields point to it
                        initResolver = new OnceRunTimeInitializerResolver(initResolver);
                        // but pass the same index for all because we do not know which field will be reached first
                        initIndex = i;
                        continue;
                    } else if (replaceInit != null) {
                        classPatchInfo.replaceInitializer(new InitializerPatchInfo(i, initResolver, internalName, replaceInit));
                    } else {
                        classContext.getCompilationContext().warning(getMethodLocation(internalName, patchMethodName), "Patch class initializer will be ignored");
                    }
                }
                boolean ctor = patchMethodName.equals("<init>");
                int attrCnt = classFile.getMethodAttributeCount(i);
                int kind = K_ALIAS;
                Annotation controllingAnnotation = null;
                List<Annotation> additionalAnnotations = null;
                for (int j = 0; j < attrCnt; j ++) {
                    if (classFile.methodAttributeNameEquals(i, j, "RuntimeInvisibleAnnotations")) {
                        // found annotations
                        ByteBuffer buf = classFile.getMethodRawAttributeContent(i, j);
                        int ac = buf.getShort() & 0xffff;
                        for (int k = 0; k < ac; k ++) {
                            Annotation annotation = Annotation.parse(classFile, classContext, buf);
                            ClassTypeDescriptor descriptor = annotation.getDescriptor();
                            if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Add")) {
                                if (kind == K_ALIAS) {
                                    kind = K_ADD;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getMethodLocation(internalName, patchMethodName));
                                    continue outer;
                                }
                            } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Remove")) {
                                if (kind == K_ALIAS) {
                                    kind = K_REMOVE;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getMethodLocation(internalName, patchMethodName));
                                    continue outer;
                                }
                            } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Replace")) {
                                if (kind == K_ALIAS) {
                                    kind = K_REPLACE;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getMethodLocation(internalName, patchMethodName));
                                    continue outer;
                                }
                            } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Patch") && annotation.getValue("value") instanceof StringAnnotationValue sav) {
                                if (ctor) {
                                    classContext.getCompilationContext().warning(getMethodLocation(internalName, patchMethodName), "Constructors cannot have specified names");
                                } else {
                                    methodName = sav.getString();
                                }
                            } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Annotate")) {
                                if (kind == K_ALIAS) {
                                    kind = K_ANNOTATE;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getMethodLocation(internalName, patchMethodName));
                                    continue outer;
                                }
                            } else {
                                if (additionalAnnotations == null) {
                                    additionalAnnotations = new ArrayList<>();
                                }
                                additionalAnnotations.add(annotation);
                            }
                        }
                        break;
                    }
                }
                MethodDescriptor methodDesc = transform(classFile.getMethodDescriptor(i));
                if (kind == K_ADD) {
                    if (ctor) {
                        classPatchInfo.addConstructor(new ConstructorPatchInfo(i, addModifiers, classFile, methodDesc, internalName, controllingAnnotation, null));
                    } else {
                        classPatchInfo.addMethod(new MethodPatchInfo(i, addModifiers, classFile, methodDesc, methodName, internalName, controllingAnnotation, null));
                    }
                } else if (kind == K_REMOVE) {
                    if (ctor) {
                        classPatchInfo.deleteConstructor(methodDesc, internalName, controllingAnnotation);
                    } else {
                        classPatchInfo.deleteMethod(methodName, methodDesc, internalName, controllingAnnotation);
                    }
                } else if (kind == K_REPLACE) {
                    if (ctor) {
                        classPatchInfo.replaceConstructor(new ConstructorPatchInfo(i, 0, classFile, methodDesc, internalName, controllingAnnotation, null));
                    } else {
                        classPatchInfo.replaceMethod(new MethodPatchInfo(i, 0, classFile, methodDesc, methodName, internalName, controllingAnnotation, null));
                    }
                } else if (kind == K_ANNOTATE) {
                    if (ctor) {
                        classPatchInfo.annotateConstructor(new ConstructorPatchInfo(i, 0, classFile, methodDesc, internalName, controllingAnnotation, additionalAnnotations));
                    } else {
                        classPatchInfo.annotateMethod(new MethodPatchInfo(i, 0, classFile, methodDesc, methodName, internalName, controllingAnnotation, additionalAnnotations));
                    }
                } else {
                    assert kind == K_ALIAS;
                }
            }
            if (! foundInit && replaceInit != null) {
                // delete the initializer altogether
                classPatchInfo.deleteInitializer();
            }

            // now examine the fields and produce patch info for each
            cnt = classFile.getFieldCount();
            outer: for (int i = 0; i < cnt; i ++) {
                String patchFieldName = classFile.getFieldName(i);
                String fieldName = patchFieldName;
                int fieldMods = classFile.getFieldModifiers(i);
                int attrCnt = classFile.getFieldAttributeCount(i);
                int kind = K_ALIAS;
                Annotation controllingAnnotation = null;
                List<Annotation> addedAnnotations = null;
                for (int j = 0; j < attrCnt; j ++) {
                    if (classFile.fieldAttributeNameEquals(i, j, "RuntimeInvisibleAnnotations")) {
                        // found annotations
                        ByteBuffer buf = classFile.getFieldRawAttributeContent(i, j);
                        int ac = buf.getShort() & 0xffff;
                        for (int k = 0; k < ac; k ++) {
                            Annotation annotation = Annotation.parse(classFile, classContext, buf);
                            ClassTypeDescriptor descriptor = annotation.getDescriptor();
                            if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Add")) {
                                if (kind == K_ALIAS) {
                                    kind = K_ADD;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getFieldLocation(internalName, patchFieldName));
                                    continue outer;
                                }
                            } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Remove")) {
                                if (kind == K_ALIAS) {
                                    kind = K_REMOVE;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getFieldLocation(internalName, patchFieldName));
                                    continue outer;
                                }
                            } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Replace")) {
                                if (kind == K_ALIAS) {
                                    kind = K_REPLACE;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getFieldLocation(internalName, patchFieldName));
                                    continue outer;
                                }
                            } else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Patch") && annotation.getValue("value") instanceof StringAnnotationValue sav) {
                                patchFieldName = sav.getString();
                            }  else if (descriptor.packageAndClassNameEquals(Patcher.PATCHER_PKG, "Annotate")) {
                                if (kind == K_ALIAS) {
                                    kind = K_ANNOTATE;
                                    controllingAnnotation = annotation;
                                } else {
                                    wrongAnnotationWarning(classContext, getFieldLocation(internalName, patchFieldName));
                                    continue outer;
                                }
                            } else {
                                if (addedAnnotations == null) {
                                    addedAnnotations = new ArrayList<>();
                                }
                                addedAnnotations.add(annotation);
                            }
                        }
                        break;
                    }
                }
                TypeDescriptor fieldDesc = transform(classFile.getFieldDescriptor(i));
                boolean isStatic = (fieldMods & ClassFile.ACC_STATIC) != 0;
                if (kind == K_ADD) {
                    if (isStatic && runTimeAspect) {
                        classPatchInfo.runtimeInitField(new RuntimeInitializerPatchInfo(internalName, i, initResolver, initIndex, fieldDesc, fieldName, controllingAnnotation));
                    }
                    classPatchInfo.addField(new FieldPatchInfo(internalName, i, 0, classFile, fieldDesc, fieldName, controllingAnnotation, null));
                } else if (kind == K_REMOVE) {
                    classPatchInfo.deleteField(fieldName, fieldDesc, internalName, controllingAnnotation);
                } else if (kind == K_REPLACE) {
                    if (isStatic && runTimeAspect) {
                        classPatchInfo.runtimeInitField(new RuntimeInitializerPatchInfo(internalName, i, initResolver, initIndex, fieldDesc, fieldName, controllingAnnotation));
                    }
                    classPatchInfo.replaceField(new FieldPatchInfo(internalName, i, 0, classFile, fieldDesc, fieldName, controllingAnnotation, null));
                } else if (kind == K_ANNOTATE) {
                    classPatchInfo.annotateField(new FieldPatchInfo(internalName, i, 0, classFile, fieldDesc, fieldName, controllingAnnotation, addedAnnotations));
                } else {
                    assert kind == K_ALIAS;
                    if (isStatic && runTimeAspect) {
                        classPatchInfo.runtimeInitField(new RuntimeInitializerPatchInfo(internalName, i, initResolver, initIndex, fieldDesc, fieldName, controllingAnnotation));
                    }
                }
            }
        }
    }

    private static Diagnostic wrongAnnotationWarning(final ClassContext classContext, final Location loc) {
        return classContext.getCompilationContext().warning(loc, "Patch field must be annotated with no more than one of `@Add`, `@Remove`, or `@Replace`");
    }

    static Location getFieldLocation(final String internalName, final String fieldName) {
        return Location.builder()
            .setMemberKind(Location.MemberKind.FIELD)
            .setMemberName(fieldName)
            .setClassInternalName(internalName)
            .build();
    }

    static Location getMethodLocation(final String internalName, final String methodName) {
        return Location.builder()
            .setMemberKind(methodName.equals("<init>") ? Location.MemberKind.CONSTRUCTOR : Location.MemberKind.METHOD)
            .setMemberName(methodName)
            .setClassInternalName(internalName)
            .build();
    }

}
