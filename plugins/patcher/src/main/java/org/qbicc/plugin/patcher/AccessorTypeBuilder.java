package org.qbicc.plugin.patcher;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.plugin.core.ConditionEvaluation;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.ClassAnnotationValue;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

/**
 * The type builder for registering accessor fields.
 */
public final class AccessorTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final DefinedTypeDefinition.Builder delegate;
    private final ClassContext classContext;

    public AccessorTypeBuilder(ClassContext classContext, DefinedTypeDefinition.Builder delegate) {
        this.classContext = classContext;
        this.delegate = delegate;
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void addField(FieldResolver resolver, int index, String name, TypeDescriptor descriptor) {
        getDelegate().addField(new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                CompilationContext ctxt = classContext.getCompilationContext();
                ConditionEvaluation ce = ConditionEvaluation.get(ctxt);
                FieldElement fieldElement = resolver.resolveField(index, enclosing, builder);
                if (fieldElement.isStatic()) {
                    VmObject accessor = null;
                    for (Annotation annotation : fieldElement.getInvisibleAnnotations()) {
                        ClassTypeDescriptor annDesc = annotation.getDescriptor();
                        if (annDesc.packageAndClassNameEquals(Patcher.PATCHER_PKG, "AccessWith$List") && annotation.getValue("value") instanceof ArrayAnnotationValue array) {
                            int cnt = array.getElementCount();
                            for (int i = 0; i < cnt; i ++) {
                                if (array.getValue(i) instanceof Annotation nested && nested.getDescriptor().packageAndClassNameEquals(Patcher.PATCHER_PKG, "AccessWith")) {
                                    if (ce.evaluateConditions(classContext, fieldElement, nested)) {
                                        if (accessor != null) {
                                            ctxt.error(fieldElement, "Only one accessor may be active on a field at a time");
                                        } else {
                                            accessor = createAccessor(fieldElement, nested);
                                        }
                                    }
                                }
                            }
                        } else if (annDesc.packageAndClassNameEquals(Patcher.PATCHER_PKG, "AccessWith")) {
                            if (accessor != null) {
                                ctxt.error(fieldElement, "Only one accessor may be active on a field at a time");
                            } else {
                                accessor = createAccessor(fieldElement, annotation);
                            }
                        }
                    }
                    if (accessor != null) {
                        Patcher.get(ctxt).registerAccessor(fieldElement, accessor);
                    }
                }
                return fieldElement;
            }

            private VmObject createAccessor(final FieldElement fieldElement, final Annotation annotation) {
                ClassAnnotationValue value = (ClassAnnotationValue) annotation.getValue("value");
                TypeDescriptor desc = value.getDescriptor();
                ValueType valueType = classContext.resolveTypeFromDescriptor(desc, TypeParameterContext.EMPTY, TypeSignature.synthesize(classContext, desc));
                if (valueType instanceof ClassObjectType cot) {
                    VmThread vmThread = Vm.requireCurrentThread();
                    LoadedTypeDefinition definition = cot.getDefinition().load();
                    ObjectType accessorInterface = classContext.getCompilationContext().getBootstrapClassContext().findDefinedType("org/qbicc/runtime/patcher/Accessor").load().getType();
                    if (! definition.getType().isSubtypeOf(accessorInterface)) {
                        classContext.getCompilationContext().error(fieldElement, "Accessor class must extend %s", accessorInterface);
                        return null;
                    }
                    int idx = definition.findConstructorIndex(MethodDescriptor.VOID_METHOD_DESCRIPTOR);
                    if (idx == -1) {
                        classContext.getCompilationContext().error(fieldElement, "Accessor class must declare a no-argument constructor");
                        return null;
                    }
                    ConstructorElement constructor = definition.getConstructor(idx);
                    return vmThread.getVM().newInstance(definition.getVmClass(), constructor, List.of());
                } else {
                    classContext.getCompilationContext().error(fieldElement, "Accessor must be a class");
                    return null;
                }
            }
        }, index, name, descriptor);
    }
}
