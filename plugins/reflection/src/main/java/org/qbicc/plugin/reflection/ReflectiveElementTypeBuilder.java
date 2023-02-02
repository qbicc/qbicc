package org.qbicc.plugin.reflection;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.core.ConditionEvaluation;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.ClassAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

import java.util.List;
import java.util.function.Predicate;

public class ReflectiveElementTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final DefinedTypeDefinition.Builder delegate;
    private final ClassContext classContext;
    private boolean reflectiveClass;
    private boolean reflectiveFields;
    private boolean reflectiveConstructors;
    private boolean reflectiveMethods;

    private final ClassTypeDescriptor reflectivelyAccessed;
    private final ClassTypeDescriptor reflectivelyAccesses;

    public ReflectiveElementTypeBuilder(final ClassContext classCtxt, DefinedTypeDefinition.Builder delegate) {
        this.delegate = delegate;
        this.classContext = classCtxt;
        reflectivelyAccessed = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/ReflectivelyAccessed");
        reflectivelyAccesses = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/ReflectivelyAccesses");
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void setName(String internalName) {
        ReflectiveElementRegistry registry = ReflectiveElementRegistry.get(classContext.getCompilationContext());
        if (registry.isReflectiveClass(internalName)) {
            reflectiveClass = true;
        }
        if (registry.hasReflectiveConstructors(internalName)) {
            reflectiveConstructors = true;
        }
        if (registry.hasReflectiveFields(internalName)) {
            reflectiveFields = true;
        }
        if (registry.hasReflectiveMethods(internalName)) {
            reflectiveMethods = true;
        }
        delegate.setName(internalName);
    }

    @Override
    public void setInvisibleAnnotations(final List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.getDescriptor().equals(reflectivelyAccesses)) {
                CompilationContext ctxt = classContext.getCompilationContext();
                ConditionEvaluation conditionEvaluation = ConditionEvaluation.get(ctxt);
                if (conditionEvaluation.evaluateConditions(classContext, this, annotation)) {
                    ReflectiveElementRegistry registry = ReflectiveElementRegistry.get(ctxt);
                    ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                    int cnt = array.getElementCount();
                    for (int j = 0; j < cnt; j ++) {
                        Annotation element = (Annotation) array.getValue(j);
                        TypeDescriptor clazz = ((ClassAnnotationValue) element.getValue("clazz")).getDescriptor();
                        String method = ((StringAnnotationValue) element.getValue("method")).getString();
                        try {
                            ValueType vt = classContext.resolveTypeFromDescriptor(clazz, TypeParameterContext.EMPTY, TypeSignature.synthesize(classContext, clazz));
                            if (vt instanceof ClassObjectType ct) {
                                LoadedTypeDefinition ltd = ct.getDefinition().load();
                                ArrayAnnotationValue ap = ((ArrayAnnotationValue) element.getValue("params"));
                                final Predicate<List<TypeDescriptor>> checkParams;
                                if (ap == null) {
                                    checkParams = args -> true;
                                } else {
                                    TypeDescriptor[] params = new TypeDescriptor[ap.getElementCount()];
                                    for (int i = 0; i < params.length; i++) {
                                        params[i] = ((ClassAnnotationValue) ap.getValue(i)).getDescriptor();
                                    }
                                    checkParams = args -> {
                                        if (args.size() != params.length) {
                                            return false;
                                        }
                                        for (int i = 0; i < params.length; i++) {
                                            if (!args.get(i).equals(params[i])) {
                                                return false;
                                            }
                                        }
                                        return true;
                                    };
                                }
                                if (method.equals("<init>")) {
                                    int idx = ltd.findSingleConstructorIndex(ce -> checkParams.test(ce.getDescriptor().getParameterTypes()));
                                    if (idx != -1) {
                                        registry.registerReflectiveConstructor(ltd.getConstructor(idx));
                                    } else {
                                        ctxt.warning("@RA Annotation not processed on %s. No match for %s.%s", this.getLocation(), clazz, method);
                                    }
                                } else {
                                    int idx = ltd.findSingleMethodIndex(me -> me.nameEquals(method) && checkParams.test(me.getDescriptor().getParameterTypes()));
                                    if (idx != -1) {
                                        registry.registerReflectiveMethod(ltd.getMethod(idx));
                                    } else {
                                        ctxt.warning("RA Annotation not processed on %s. No match for %s.%s", this.getLocation(), clazz, method);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            ctxt.warning(e,"RA Annotation not processed in %s. No unique match for  %s.%s", this.getLocation(), clazz,  method);
                        }
                    }
                }
            }
        }
        getDelegate().setInvisibleAnnotations(annotations);
    }

    @Override
    public void addConstructor(ConstructorResolver resolver, int index, MethodDescriptor descriptor) {
        if (reflectiveConstructors) {
            Delegating.super.addConstructor(new ConstructorResolver() {
                @Override
                public ConstructorElement resolveConstructor(int index, DefinedTypeDefinition enclosing, ConstructorElement.Builder builder) {
                    ConstructorElement constructorElement = resolver.resolveConstructor(index, enclosing, builder);
                    ReflectiveElementRegistry registry = ReflectiveElementRegistry.get(classContext.getCompilationContext());
                    String className = constructorElement.getEnclosingType().getInternalName();
                    boolean isReflective = registry.isReflectiveConstructor(className, constructorElement.getDescriptor());
                    for (Annotation annotation : constructorElement.getInvisibleAnnotations()) {
                        if (annotation.getDescriptor().equals(reflectivelyAccessed)) {
                            isReflective = true;
                        }
                    }
                    if (isReflective) {
                        registry.registerReflectiveConstructor(constructorElement);
                    }
                    return constructorElement;
                }
            }, index, descriptor);
        } else {
            delegate.addConstructor(resolver, index, descriptor);
        }
    }

    @Override
    public void addMethod(MethodResolver resolver, int index, String name, MethodDescriptor descriptor) {
        if (reflectiveMethods) {
            Delegating.super.addMethod(new MethodResolver() {
                @Override
                public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
                    MethodElement methodElement = resolver.resolveMethod(index, enclosing, builder);
                    ReflectiveElementRegistry registry = ReflectiveElementRegistry.get(classContext.getCompilationContext());
                    String className = methodElement.getEnclosingType().getInternalName();
                    boolean isReflective = registry.isReflectiveMethod(className, methodElement.getName(), methodElement.getDescriptor());
                    for (Annotation annotation : methodElement.getInvisibleAnnotations()) {
                         if (annotation.getDescriptor().equals(reflectivelyAccessed)) {
                             isReflective = true;
                        }
                    }
                    if (isReflective) {
                        registry.registerReflectiveMethod(methodElement);
                    }
                    return methodElement;
                }
            }, index, name, descriptor);
        } else {
            delegate.addMethod(resolver, index, name, descriptor);
        }
    }

    @Override
    public void addField(FieldResolver resolver, int index, String name, TypeDescriptor descriptor) {
        if (reflectiveFields) {
            final FieldResolver fr = resolver;
            Delegating.super.addField(new FieldResolver() {
                @Override
                public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                    FieldElement fieldElement = fr.resolveField(index, enclosing, builder);
                    ReflectiveElementRegistry registry = ReflectiveElementRegistry.get(classContext.getCompilationContext());
                    String className = fieldElement.getEnclosingType().getInternalName();
                    boolean isReflective = registry.isReflectiveField(className, fieldElement.getName());
                    for (Annotation annotation : fieldElement.getInvisibleAnnotations()) {
                        if (annotation.getDescriptor().equals(reflectivelyAccessed)) {
                           isReflective = true;
                        }
                    }
                    if (isReflective) {
                       registry.registerReflectiveField(fieldElement);
                    }
                    return fieldElement;
                }
            }, index, name, descriptor);
        } else {
            delegate.addField(resolver, index, name, descriptor);
        }
    }

    @Override
    public DefinedTypeDefinition build() {
        DefinedTypeDefinition result = delegate.build();
        if (reflectiveClass) {
            ReflectiveElementRegistry registry = ReflectiveElementRegistry.get(classContext.getCompilationContext());
            classContext.getCompilationContext().submitTask(result, dtd -> {
                registry.registerReflectiveType(dtd.load());
            });
        }
        return result;
    }
}
