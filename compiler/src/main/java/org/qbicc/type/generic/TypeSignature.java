package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.block.factory.Functions;
import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public abstract class TypeSignature extends Signature {
    private final ImmutableMap<ClassTypeDescriptor, Annotation> annotations;
    private TypeDescriptor descriptor;

    TypeSignature(final int hashCode, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(annotations.hashCode() * 19 + hashCode);
        this.annotations = annotations;
    }

    public Collection<Annotation> getAnnotations() {
        return annotations.castToMap().values();
    }

    ImmutableMap<ClassTypeDescriptor, Annotation> getAnnotationsPrivate() {
        return annotations;
    }

    public boolean hasAnnotation(ClassTypeDescriptor desc) {
        return annotations.containsKey(desc);
    }

    public boolean hasAnnotation(Annotation annotation) {
        return annotation.equals(annotations.get(annotation.getDescriptor()));
    }

    public Annotation getAnnotation(ClassTypeDescriptor desc) {
        return annotations.get(desc);
    }

    public TypeSignature withAnnotation(Annotation annotation) {
        if (annotation.equals(annotations.get(annotation.getDescriptor()))) {
            // we have that one
            return this;
        } else {
            // add or replace it
            return replacingAnnotationMap(annotations.newWithKeyValue(annotation.getDescriptor(), annotation));
        }
    }

    abstract TypeSignature replacingAnnotationMap(ImmutableMap<ClassTypeDescriptor, Annotation> newMap);

    public TypeSignature withAnnotations(Set<Annotation> set) {
        if (annotations.valuesView().containsAll(set)) {
            return this;
        } else {
            return replacingAnnotationMap(annotations.newWithMap(Sets.immutable.ofAll(set).toImmutableMap(Annotation::getDescriptor, Functions.identity()).castToMap()));
        }
    }

    public TypeSignature withOnlyAnnotations(Set<Annotation> set) {
        return replacingAnnotationMap(Sets.immutable.ofAll(set).toImmutableMap(Annotation::getDescriptor, Functions.identity()));
    }

    public TypeSignature withNoAnnotations() {
        return annotations.isEmpty() ? this : replacingAnnotationMap(Maps.immutable.empty());
    }

    public TypeSignature withoutAnnotation(Annotation annotation) {
        ClassTypeDescriptor descriptor = annotation.getDescriptor();
        if (annotation.equals(annotations.get(annotation.getDescriptor()))) {
            return replacingAnnotationMap(annotations.newWithoutKey(descriptor));
        } else {
            return this;
        }
    }

    public TypeSignature withoutAnnotation(ClassTypeDescriptor descriptor) {
        if (annotations.containsKey(descriptor)) {
            return replacingAnnotationMap(annotations.newWithoutKey(descriptor));
        } else {
            return this;
        }
    }

    public final boolean equals(final Signature other) {
        return other instanceof TypeSignature && equals((TypeSignature) other);
    }

    public boolean equals(final TypeSignature other) {
        return super.equals(other) && annotations.equals(other.annotations);
    }

    public TypeDescriptor asDescriptor(ClassContext classContext) {
        TypeDescriptor descriptor = this.descriptor;
        if (descriptor == null) {
            descriptor = this.descriptor = makeDescriptor(classContext);
        }
        return descriptor;
    }

    abstract TypeDescriptor makeDescriptor(ClassContext classContext);

    public static TypeSignature parse(ClassContext classContext, ByteBuffer buf) {
        int i = peek(buf);
        if (i == 'L') {
            return ClassTypeSignature.parse(classContext, buf);
        } else if (i == 'T') {
            return TypeVariableSignature.parse(classContext, buf);
        } else if (i == '[') {
            return ArrayTypeSignature.parse(classContext, buf);
        } else {
            return BaseTypeSignature.parse(buf);
        }
    }

    public static TypeSignature synthesize(ClassContext classContext, TypeDescriptor descriptor) {
        if (descriptor instanceof BaseTypeDescriptor) {
            return BaseTypeSignature.forChar(((BaseTypeDescriptor) descriptor).getShortName());
        } else if (descriptor instanceof ArrayTypeDescriptor) {
            return Cache.get(classContext).getArrayTypeSignature(synthesize(classContext, ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor()));
        } else {
            assert descriptor instanceof ClassTypeDescriptor;
            ClassTypeDescriptor classDesc = (ClassTypeDescriptor) descriptor;
            return Cache.get(classContext).getTopLevelTypeSignature(classDesc.getPackageName(), classDesc.getClassName(), List.of());
        }
    }
}
