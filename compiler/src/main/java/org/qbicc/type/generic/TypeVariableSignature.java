package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public final class TypeVariableSignature extends ThrowsSignature {
    private final String identifier;

    TypeVariableSignature(final String identifier) {
        this(identifier, Maps.immutable.empty());
    }

    TypeVariableSignature(final String identifier, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(Objects.hash(TypeVariableSignature.class, identifier), annotations);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean equals(final ThrowsSignature other) {
        return other instanceof TypeVariableSignature && equals((TypeVariableSignature) other);
    }

    public boolean equals(final TypeVariableSignature other) {
        return super.equals(other) && identifier.equals(other.identifier);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append('T').append(identifier).append(';');
    }

    @Override
    public TypeVariableSignature withAnnotation(Annotation annotation) {
        return (TypeVariableSignature) super.withAnnotation(annotation);
    }

    @Override
    public TypeVariableSignature withAnnotations(Set<Annotation> set) {
        return (TypeVariableSignature) super.withAnnotations(set);
    }

    @Override
    public TypeVariableSignature withOnlyAnnotations(Set<Annotation> set) {
        return (TypeVariableSignature) super.withOnlyAnnotations(set);
    }

    @Override
    public TypeVariableSignature withNoAnnotations() {
        return (TypeVariableSignature) super.withNoAnnotations();
    }

    @Override
    public TypeVariableSignature withoutAnnotation(Annotation annotation) {
        return (TypeVariableSignature) super.withoutAnnotation(annotation);
    }

    @Override
    public TypeVariableSignature withoutAnnotation(ClassTypeDescriptor descriptor) {
        return (TypeVariableSignature) super.withoutAnnotation(descriptor);
    }

    @Override
    TypeVariableSignature replacingAnnotationMap(ImmutableMap<ClassTypeDescriptor, Annotation> newMap) {
        return new TypeVariableSignature(identifier, newMap);
    }

    public ClassTypeDescriptor asDescriptor(final ClassContext classContext) {
        return (ClassTypeDescriptor) super.asDescriptor(classContext);
    }

    ClassTypeDescriptor makeDescriptor(final ClassContext classContext) {
        return ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
    }

    public static TypeVariableSignature parse(ClassContext classContext, ByteBuffer buf) {
        expect(buf, 'T');
        StringBuilder sb = new StringBuilder();
        int i = peek(buf);
        while (i != ';') {
            sb.appendCodePoint(codePoint(buf));
            i = peek(buf);
        }
        buf.get(); // consume ';'
        return Cache.get(classContext).getTypeVariableSignature(classContext.deduplicate(sb.toString()));
    }
}
