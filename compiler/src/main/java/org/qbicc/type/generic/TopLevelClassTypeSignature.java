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
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public final class TopLevelClassTypeSignature extends ClassTypeSignature {
    private final String packageName;

    TopLevelClassTypeSignature(final String packageName, final String identifier, final List<TypeArgument> typeArguments) {
        this(packageName, identifier, typeArguments, Maps.immutable.empty());
    }

    TopLevelClassTypeSignature(final String packageName, final String identifier, final List<TypeArgument> typeArguments, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        super(Objects.hash(TopLevelClassTypeSignature.class, packageName), identifier, typeArguments, annotations);
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean equals(final ClassTypeSignature other) {
        return other instanceof TopLevelClassTypeSignature && equals((TopLevelClassTypeSignature) other);
    }

    public boolean equals(final TopLevelClassTypeSignature other) {
        return super.equals(other) && packageName.equals(other.packageName);
    }

    StringBuilder prefixString(final StringBuilder target) {
        target.append('L');
        if (! packageName.isEmpty()) {
            target.append(packageName).append('/');
        }
        return simpleString(target);
    }

    @Override
    public TopLevelClassTypeSignature withAnnotation(Annotation annotation) {
        return (TopLevelClassTypeSignature) super.withAnnotation(annotation);
    }

    @Override
    public TopLevelClassTypeSignature withAnnotations(Set<Annotation> set) {
        return (TopLevelClassTypeSignature) super.withAnnotations(set);
    }

    @Override
    public TopLevelClassTypeSignature withOnlyAnnotations(Set<Annotation> set) {
        return (TopLevelClassTypeSignature) super.withOnlyAnnotations(set);
    }

    @Override
    public TopLevelClassTypeSignature withNoAnnotations() {
        return (TopLevelClassTypeSignature) super.withNoAnnotations();
    }

    @Override
    public TopLevelClassTypeSignature withoutAnnotation(Annotation annotation) {
        return (TopLevelClassTypeSignature) super.withoutAnnotation(annotation);
    }

    @Override
    public TopLevelClassTypeSignature withoutAnnotation(ClassTypeDescriptor descriptor) {
        return (TopLevelClassTypeSignature) super.withoutAnnotation(descriptor);
    }

    @Override
    TopLevelClassTypeSignature replacingAnnotationMap(ImmutableMap<ClassTypeDescriptor, Annotation> newMap) {
        return new TopLevelClassTypeSignature(packageName, getIdentifier(), getTypeArguments(), newMap);
    }

    TypeDescriptor makeDescriptor(final ClassContext classContext) {
        return ClassTypeDescriptor.synthesize(classContext, packageName + '/' + getIdentifier());
    }

    public static TopLevelClassTypeSignature parse(ClassContext classContext, ByteBuffer buf) {
        int lastIdx = -1;
        StringBuilder b = new StringBuilder();
        int i;
        for (;;) {
            i = peek(buf);
            if (i == '/') {
                buf.get();
                lastIdx = b.length();
                b.appendCodePoint(i);
            } else if (i == '.' || i == ';' || i == '<') {
                String packageName;
                String identifier;
                if (lastIdx == -1) {
                    packageName = "";
                    identifier = classContext.deduplicate(b.toString());
                } else {
                    packageName = classContext.deduplicate(b.substring(0, lastIdx));
                    identifier = classContext.deduplicate(b.substring(lastIdx + 1));
                }
                List<TypeArgument> typeArgs;
                if (i == '<') {
                    typeArgs = TypeArgument.parseList(classContext, buf);
                } else {
                    typeArgs = List.of();
                }
                return Cache.get(classContext).getTopLevelTypeSignature(packageName, identifier, typeArgs);
            } else {
                // utf-8
                b.appendCodePoint(codePoint(buf));
            }
        }

    }
}
