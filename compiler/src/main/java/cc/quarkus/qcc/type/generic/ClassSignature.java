package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.definition.ClassContext;

/**
 *
 */
public final class ClassSignature extends ParameterizedSignature {
    private final ClassTypeSignature superClassSignature;
    private final List<ClassTypeSignature> interfaceSignatures;

    ClassSignature(final List<TypeParameter> typeParameters, final ClassTypeSignature superClassSignature, final List<ClassTypeSignature> interfaceSignatures) {
        super(Objects.hash(superClassSignature, interfaceSignatures), typeParameters);
        this.superClassSignature = superClassSignature;
        this.interfaceSignatures = interfaceSignatures;
    }

    public ClassTypeSignature getSuperClassSignature() {
        return superClassSignature;
    }

    public List<ClassTypeSignature> getInterfaceSignatures() {
        return interfaceSignatures;
    }

    public boolean equals(final ParameterizedSignature other) {
        return other instanceof ClassSignature && equals((ClassSignature) other);
    }

    public boolean equals(final ClassSignature other) {
        return super.equals(other) && superClassSignature.equals(other.superClassSignature) && interfaceSignatures.equals(other.interfaceSignatures);
    }

    public StringBuilder toString(final StringBuilder target) {
        super.toString(target);
        ClassTypeSignature scs = this.superClassSignature;
        if (scs != null) {
            scs.toString(target);
        }
        for (ClassTypeSignature interfaceSignature : interfaceSignatures) {
            interfaceSignature.toString(target);
        }
        return target;
    }

    public static ClassSignature synthesize(ClassContext classContext, ClassTypeSignature superClassSig, List<ClassTypeSignature> interfaceSigs) {
        return Cache.get(classContext).getClassSignature(List.of(), superClassSig, interfaceSigs);
    }

    public static ClassSignature parse(ClassContext classContext, ByteBuffer buf) {
        return (ClassSignature) ParameterizedSignature.parse(classContext, buf);
    }

    static ClassSignature parse(ClassContext classContext, ByteBuffer buf, List<TypeParameter> typeParameters) {
        ClassTypeSignature superClassSignature = ClassTypeSignature.parse(classContext, buf);
        List<ClassTypeSignature> interfaceSignatures;
        if (buf.hasRemaining()) {
            ClassTypeSignature a = ClassTypeSignature.parse(classContext, buf);
            if (buf.hasRemaining()) {
                ClassTypeSignature b = ClassTypeSignature.parse(classContext, buf);
                if (buf.hasRemaining()) {
                    ClassTypeSignature c = ClassTypeSignature.parse(classContext, buf);
                    if (buf.hasRemaining()) {
                        ClassTypeSignature d = ClassTypeSignature.parse(classContext, buf);
                        if (buf.hasRemaining()) {
                            ClassTypeSignature e = ClassTypeSignature.parse(classContext, buf);
                            if (buf.hasRemaining()) {
                                ClassTypeSignature f = ClassTypeSignature.parse(classContext, buf);
                                if (buf.hasRemaining()) {
                                    // many
                                    interfaceSignatures = new ArrayList<>(16);
                                    Collections.addAll(interfaceSignatures, a, b, c, d, e, f);
                                    do {
                                        interfaceSignatures.add(ClassTypeSignature.parse(classContext, buf));
                                    } while (buf.hasRemaining());
                                    interfaceSignatures = List.copyOf(interfaceSignatures);
                                } else {
                                    interfaceSignatures = List.of(a, b, c, d, e, f);
                                }
                            } else {
                                interfaceSignatures = List.of(a, b, c, d, e);
                            }
                        } else {
                            interfaceSignatures = List.of(a, b, c, d);
                        }
                    } else {
                        interfaceSignatures = List.of(a, b, c);
                    }
                } else {
                    interfaceSignatures = List.of(a, b);
                }
            } else {
                interfaceSignatures = List.of(a);
            }
        } else {
            interfaceSignatures = List.of();
        }
        return Cache.get(classContext).getClassSignature(typeParameters, superClassSignature, interfaceSignatures);
    }
}
