package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.List;
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
public final class NestedClassTypeSignature extends ClassTypeSignature {
    private final ClassTypeSignature enclosing;

    NestedClassTypeSignature(final ClassTypeSignature enclosing, final String identifier, final List<TypeArgument> typeArguments, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(Objects.hash(NestedClassTypeSignature.class, enclosing), identifier, typeArguments, annotations);
        this.enclosing = enclosing;
    }

    NestedClassTypeSignature(final ClassTypeSignature enclosing, final String identifier, final List<TypeArgument> typeArguments) {
        this(enclosing, identifier, typeArguments, Maps.immutable.empty());
    }

    public ClassTypeSignature getEnclosing() {
        return enclosing;
    }

    public boolean equals(final ClassTypeSignature other) {
        return other instanceof NestedClassTypeSignature && equals((NestedClassTypeSignature) other);
    }

    public boolean equals(final NestedClassTypeSignature other) {
        return super.equals(other) && enclosing.equals(other.enclosing);
    }

    StringBuilder prefixString(final StringBuilder target) {
        return simpleString(enclosing.prefixString(target).append('.'));
    }

    public static NestedClassTypeSignature parse(ClassTypeSignature outer, ClassContext classContext, ByteBuffer buf) {
        expect(buf, '.');
        StringBuilder b = new StringBuilder();
        int i;
        for (;;) {
            i = peek(buf);
            if (i == '/') {
                throw parseError();
            } else if (i == '.' || i == ';' || i == '<') {
                String identifier = classContext.deduplicate(b.toString());
                List<TypeArgument> typeArgs;
                if (i == '<') {
                    typeArgs = TypeArgument.parseList(classContext, buf);
                    i = peek(buf);
                    if (i != '.' && i != ';') {
                        throw parseError();
                    }
                } else {
                    typeArgs = List.of();
                }
                return Cache.get(classContext).getNestedTypeSignature(outer, identifier, typeArgs);
            } else {
                b.appendCodePoint(codePoint(buf));
            }
        }
    }

    @Override
    public NestedClassTypeSignature withAnnotation(Annotation annotation) {
        return (NestedClassTypeSignature) super.withAnnotation(annotation);
    }

    @Override
    public NestedClassTypeSignature withAnnotations(Set<Annotation> set) {
        return (NestedClassTypeSignature) super.withAnnotations(set);
    }

    @Override
    public NestedClassTypeSignature withOnlyAnnotations(Set<Annotation> set) {
        return (NestedClassTypeSignature) super.withOnlyAnnotations(set);
    }

    @Override
    public NestedClassTypeSignature withNoAnnotations() {
        return (NestedClassTypeSignature) super.withNoAnnotations();
    }

    @Override
    public NestedClassTypeSignature withoutAnnotation(Annotation annotation) {
        return (NestedClassTypeSignature) super.withoutAnnotation(annotation);
    }

    @Override
    public NestedClassTypeSignature withoutAnnotation(ClassTypeDescriptor descriptor) {
        return (NestedClassTypeSignature) super.withoutAnnotation(descriptor);
    }

    public NestedClassTypeSignature withEnclosing(ClassTypeSignature enclosing) {
        return new NestedClassTypeSignature(enclosing, getIdentifier(), getTypeArguments(), getAnnotationsPrivate());
    }

    @Override
    NestedClassTypeSignature replacingAnnotationMap(ImmutableMap<ClassTypeDescriptor, Annotation> newMap) {
        return new NestedClassTypeSignature(enclosing, getIdentifier(), getTypeArguments(), newMap);
    }

    ClassTypeDescriptor makeDescriptor(final ClassContext classContext) {
        ClassTypeDescriptor encDesc = enclosing.asDescriptor(classContext);
        final String internalName;
        if (encDesc.getPackageName().isEmpty()) {
            internalName = encDesc.getClassName() + '$' + getIdentifier();
        } else {
            internalName = encDesc.getPackageName() + '/' + encDesc.getClassName() + '$' + getIdentifier();
        }
        return ClassTypeDescriptor.synthesize(classContext, internalName);
    }
}
