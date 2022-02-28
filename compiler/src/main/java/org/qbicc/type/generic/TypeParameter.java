package org.qbicc.type.generic;

import static org.qbicc.type.generic.Signature.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.block.factory.Functions;
import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public final class TypeParameter {
    private final String identifier;
    private final ReferenceTypeSignature classBound;
    private final List<ReferenceTypeSignature> interfaceBounds;
    private final ImmutableMap<ClassTypeDescriptor, Annotation> annotations;
    private final int hashCode;

    private TypeParameter(final String identifier, final ReferenceTypeSignature classBound, final List<ReferenceTypeSignature> interfaceBounds, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        this.identifier = identifier;
        this.classBound = classBound;
        this.interfaceBounds = interfaceBounds;
        this.annotations = annotations;
        hashCode = Objects.hash(TypeParameter.class, identifier, classBound, interfaceBounds);
    }

    private TypeParameter(final TypeParameter orig, ImmutableMap<ClassTypeDescriptor, Annotation> annotations) {
        this(orig.identifier, orig.classBound, orig.interfaceBounds, annotations);
    }

    TypeParameter(final String identifier, final ReferenceTypeSignature classBound, final List<ReferenceTypeSignature> interfaceBounds) {
        this(identifier, classBound, interfaceBounds, Maps.immutable.empty());
    }

    public String getIdentifier() {
        return identifier;
    }

    public ReferenceTypeSignature getClassBound() {
        return classBound;
    }

    public List<ReferenceTypeSignature> getInterfaceBounds() {
        return interfaceBounds;
    }

    public Collection<Annotation> getAnnotations() {
        return annotations.castToMap().values();
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

    public TypeParameter withAnnotation(Annotation annotation) {
        if (annotation.equals(annotations.get(annotation.getDescriptor()))) {
            // we have that one
            return this;
        } else {
            // add or replace it
            return new TypeParameter(this, annotations.newWithKeyValue(annotation.getDescriptor(), annotation));
        }
    }

    public TypeParameter withAnnotations(Set<Annotation> set) {
        if (annotations.valuesView().containsAll(set)) {
            return this;
        } else {
            return new TypeParameter(this, annotations.newWithMap(Sets.immutable.ofAll(set).toImmutableMap(Annotation::getDescriptor, Functions.identity()).castToMap()));
        }
    }

    public TypeParameter withOnlyAnnotations(Set<Annotation> set) {
        return new TypeParameter(this, Sets.immutable.ofAll(set).toImmutableMap(Annotation::getDescriptor, Functions.identity()));
    }

    public TypeParameter withNoAnnotations() {
        return annotations.isEmpty() ? this : new TypeParameter(identifier, classBound, interfaceBounds, Maps.immutable.empty());
    }

    public TypeParameter withoutAnnotation(Annotation annotation) {
        ClassTypeDescriptor descriptor = annotation.getDescriptor();
        if (annotation.equals(annotations.get(annotation.getDescriptor()))) {
            return new TypeParameter(this, annotations.newWithoutKey(descriptor));
        } else {
            return this;
        }
    }

    public TypeParameter withoutAnnotation(ClassTypeDescriptor descriptor) {
        if (annotations.containsKey(descriptor)) {
            return new TypeParameter(this, annotations.newWithoutKey(descriptor));
        } else {
            return this;
        }
    }

    public boolean equals(final Object obj) {
        return obj instanceof TypeParameter && equals((TypeParameter) obj);
    }

    public boolean equals(final TypeParameter other) {
        return this == other || other != null && hashCode == other.hashCode && identifier.equals(other.identifier)
            && Objects.equals(classBound, other.classBound) && interfaceBounds.equals(other.interfaceBounds)
            && annotations.equals(other.annotations);
    }

    public int hashCode() {
        return hashCode;
    }

    public StringBuilder toString(StringBuilder target) {
        target.append(identifier).append(':');
        if (classBound != null) {
            classBound.toString(target);
        }
        for (ReferenceTypeSignature interfaceBound : interfaceBounds) {
            interfaceBound.toString(target.append(':'));
        }
        return target;
    }

    private static ReferenceTypeSignature parseBound(ClassContext classContext, ByteBuffer buf) {
        // the following symbols are forbidden in type parameter identifier parts:
        //   . ; [ / < > :
        // The possible productions at this point are:
        //   [                  // ArrayTypeSignature is next
        //   L Identifier /     // ClassTypeSignature
        //   L Identifier ;     // ClassTypeSignature
        //   T Identifier ;     // TypeVariableSignature
        //   Identifier :       // another TypeParameter
        // We don't know which is which until we hit any of [ . ; :
        // ...then we'll have to rewind to decide what to do next.
        int save = buf.position();
        int i = peek(buf);
        if (i == '[') {
            // easy: it's an array bound
            return ArrayTypeSignature.parse(classContext, buf);
        } else if (i == 'L' || i == 'T') {
            // might be a bound or it might be an identifier
            do {
                i = next(buf);
            } while (i != '/' && i != ';' && i != ':');
            // rewind
            buf.position(save);
            if (i == ';' || i == '/') {
                // it's a bound after all
                return ReferenceTypeSignature.parse(classContext, buf);
            } else {
                // it's an Identifier
                return null;
            }
        } else {
            // it's an Identifier
            return null;
        }
    }

    static TypeParameter parse(ClassContext classContext, ByteBuffer buf) {
        StringBuilder sb = new StringBuilder();
        int i;
        for (;;) {
            i = peek(buf);
            if (i == ':') {
                buf.get(); // consume ':'
                String identifier = classContext.deduplicate(sb.toString());
                sb.setLength(0);
                ReferenceTypeSignature classBound = parseBound(classContext, buf);
                // interface bounds
                // peek
                List<ReferenceTypeSignature> interfaceBounds;
                i = peek(buf);
                if (i == ':') {
                    // at least one
                    expect(buf, ':');
                    ReferenceTypeSignature a = ReferenceTypeSignature.parse(classContext, buf);
                    i = peek(buf);
                    if (i == ':') {
                        expect(buf, ':');
                        ReferenceTypeSignature b = ReferenceTypeSignature.parse(classContext, buf);
                        i = peek(buf);
                        if (i == ':') {
                            expect(buf, ':');
                            ReferenceTypeSignature c = ReferenceTypeSignature.parse(classContext, buf);
                            i = peek(buf);
                            if (i == ':') {
                                expect(buf, ':');
                                ReferenceTypeSignature d = ReferenceTypeSignature.parse(classContext, buf);
                                i = peek(buf);
                                if (i == ':') {
                                    expect(buf, ':');
                                    // many
                                    interfaceBounds = new ArrayList<>();
                                    interfaceBounds.add(a);
                                    interfaceBounds.add(b);
                                    interfaceBounds.add(c);
                                    interfaceBounds.add(d);
                                    for (;;) {
                                        interfaceBounds.add(ReferenceTypeSignature.parse(classContext, buf));
                                        i = peek(buf);
                                        if (i != ':') {
                                            break;
                                        }
                                        expect(buf, ':');
                                    }
                                    interfaceBounds = List.copyOf(interfaceBounds);
                                } else {
                                    interfaceBounds = List.of(a, b, c, d);
                                }
                            } else {
                                interfaceBounds = List.of(a, b, c);
                            }
                        } else {
                            interfaceBounds = List.of(a, b);
                        }
                    } else {
                        interfaceBounds = List.of(a);
                    }
                } else {
                    interfaceBounds = List.of();
                }
                return Cache.get(classContext).createTypeParameter(identifier, classBound, interfaceBounds);
            } else {
                sb.appendCodePoint(codePoint(buf));
            }
        }
    }

    static List<TypeParameter> parseList(ClassContext classContext, ByteBuffer buf) {
        expect(buf, '<');
        int i = peek(buf);
        if (i == '>') {
            buf.get(); // consume '>'
            return List.of();
        }
        TypeParameter a = parse(classContext, buf);
        i = peek(buf);
        if (i == '>') {
            buf.get(); // consume '>'
            return List.of(a);
        }
        TypeParameter b = parse(classContext, buf);
        i = peek(buf);
        if (i == '>') {
            buf.get(); // consume '>'
            return List.of(a, b);
        }
        TypeParameter c = parse(classContext, buf);
        i = peek(buf);
        if (i == '>') {
            buf.get(); // consume '>'
            return List.of(a, b, c);
        }
        TypeParameter d = parse(classContext, buf);
        i = peek(buf);
        if (i == '>') {
            buf.get(); // consume '>'
            return List.of(a, b, c, d);
        }
        // many
        List<TypeParameter> list = new ArrayList<>(16);
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(d);
        for (;;) {
            i = peek(buf);
            if (i == '>') {
                buf.get(); // consume '>'
                return List.copyOf(list);
            }
            list.add(parse(classContext, buf));
        }
    }
}
