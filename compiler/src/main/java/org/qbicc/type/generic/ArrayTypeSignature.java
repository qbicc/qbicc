package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.Set;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public final class ArrayTypeSignature extends ReferenceTypeSignature {
    private final TypeSignature elementTypeSignature;

    ArrayTypeSignature(final TypeSignature elementTypeSignature, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(elementTypeSignature.hashCode() * 19 + ArrayTypeSignature.class.hashCode(), annotations);
        this.elementTypeSignature = elementTypeSignature;
    }

    ArrayTypeSignature(final TypeSignature elementTypeSignature) {
        this(elementTypeSignature, Maps.immutable.empty());
    }

    public TypeSignature getElementTypeSignature() {
        return elementTypeSignature;
    }

    public boolean equals(final ReferenceTypeSignature other) {
        return other instanceof ArrayTypeSignature && equals((ArrayTypeSignature) other);
    }

    public boolean equals(final ArrayTypeSignature other) {
        return super.equals(other) && elementTypeSignature.equals(other.elementTypeSignature);
    }

    public StringBuilder toString(final StringBuilder target) {
        return elementTypeSignature.toString(target.append('['));
    }

    @Override
    public ArrayTypeSignature withAnnotation(Annotation annotation) {
        return (ArrayTypeSignature) super.withAnnotation(annotation);
    }

    @Override
    public ArrayTypeSignature withAnnotations(Set<Annotation> set) {
        return (ArrayTypeSignature) super.withAnnotations(set);
    }

    @Override
    public ArrayTypeSignature withOnlyAnnotations(Set<Annotation> set) {
        return (ArrayTypeSignature) super.withOnlyAnnotations(set);
    }

    @Override
    public ArrayTypeSignature withoutAnnotation(Annotation annotation) {
        return (ArrayTypeSignature) super.withoutAnnotation(annotation);
    }

    @Override
    public ArrayTypeSignature withoutAnnotation(ClassTypeDescriptor descriptor) {
        return (ArrayTypeSignature) super.withoutAnnotation(descriptor);
    }

    public ArrayTypeSignature withElementType(TypeSignature signature) {
        return new ArrayTypeSignature(elementTypeSignature, getAnnotationsPrivate());
    }

    @Override
    ArrayTypeSignature replacingAnnotationMap(ImmutableMap<ClassTypeDescriptor, Annotation> newMap) {
        return new ArrayTypeSignature(elementTypeSignature, newMap);
    }

    public ArrayTypeDescriptor asDescriptor(final ClassContext classContext) {
        return (ArrayTypeDescriptor) super.asDescriptor(classContext);
    }

    ArrayTypeDescriptor makeDescriptor(final ClassContext classContext) {
        return ArrayTypeDescriptor.of(classContext, elementTypeSignature.asDescriptor(classContext));
    }

    public static ArrayTypeSignature parse(ClassContext classContext, ByteBuffer buf) {
        expect(buf, '[');
        return Cache.get(classContext).getArrayTypeSignature(TypeSignature.parse(classContext, buf));
    }

    public static ArrayTypeSignature of(ClassContext classContext, TypeSignature elementTypeSignature) {
        return Cache.get(classContext).getArrayTypeSignature(elementTypeSignature);
    }
}
