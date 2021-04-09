package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public final class MethodSignature extends ParameterizedSignature {
    private final List<TypeSignature> parameterTypes;
    private final TypeSignature returnTypeSignature;
    private final List<ThrowsSignature> throwsSignatures;

    MethodSignature(final List<TypeParameter> typeParameters, final List<TypeSignature> parameterTypes, final TypeSignature returnTypeSignature, final List<ThrowsSignature> throwsSignatures) {
        super(Objects.hash(MethodSignature.class, parameterTypes, returnTypeSignature, throwsSignatures), typeParameters);
        this.parameterTypes = parameterTypes;
        this.returnTypeSignature = returnTypeSignature;
        this.throwsSignatures = throwsSignatures;
    }

    public List<TypeSignature> getParameterTypes() {
        return parameterTypes;
    }

    public TypeSignature getReturnTypeSignature() {
        return returnTypeSignature;
    }

    public List<ThrowsSignature> getThrowsSignatures() {
        return throwsSignatures;
    }

    public boolean equals(final ParameterizedSignature other) {
        return other instanceof MethodSignature && equals((MethodSignature) other);
    }

    public boolean equals(final MethodSignature other) {
        return super.equals(other)
            && parameterTypes.equals(other.parameterTypes)
            && returnTypeSignature.equals(other.returnTypeSignature)
            && throwsSignatures.equals(other.throwsSignatures);
    }

    public static MethodSignature parse(ClassContext classContext, ByteBuffer buf) {
        return (MethodSignature) ParameterizedSignature.parse(classContext, buf);
    }

    static MethodSignature parse(ClassContext classContext, ByteBuffer buf, List<TypeParameter> typeParameters) {
        expect(buf, '(');
        int i;
        List<TypeSignature> parameterTypes;
        if (peek(buf) != ')') {
            TypeSignature a = TypeSignature.parse(classContext, buf);
            if (peek(buf) != ')') {
                TypeSignature b = TypeSignature.parse(classContext, buf);
                if (peek(buf) != ')') {
                    TypeSignature c = TypeSignature.parse(classContext, buf);
                    if (peek(buf) != ')') {
                        TypeSignature d = TypeSignature.parse(classContext, buf);
                        if (peek(buf) != ')') {
                            TypeSignature e = TypeSignature.parse(classContext, buf);
                            if (peek(buf) != ')') {
                                TypeSignature f = TypeSignature.parse(classContext, buf);
                                if (peek(buf) != ')') {
                                    // many
                                    parameterTypes = new ArrayList<>();
                                    Collections.addAll(parameterTypes, a, b, c, d, e, f);
                                    do {
                                        parameterTypes.add(TypeSignature.parse(classContext, buf));
                                        i = peek(buf);
                                    } while (i != ')');
                                    parameterTypes = List.copyOf(parameterTypes);
                                } else {
                                    parameterTypes = List.of(a, b, c, d, e, f);
                                }
                            } else {
                                parameterTypes = List.of(a, b, c, d, e);
                            }
                        } else {
                            parameterTypes = List.of(a, b, c, d);
                        }
                    } else {
                        parameterTypes = List.of(a, b, c);
                    }
                } else {
                    parameterTypes = List.of(a, b);
                }
            } else {
                parameterTypes = List.of(a);
            }
        } else {
            parameterTypes = List.of();
        }
        expect(buf, ')'); //consume
        TypeSignature returnTypeSignature = TypeSignature.parse(classContext, buf);
        List<ThrowsSignature> throwsSignatures;
        if (buf.hasRemaining()) {
            ThrowsSignature a = ThrowsSignature.parse(classContext, buf);
            if (buf.hasRemaining()) {
                ThrowsSignature b = ThrowsSignature.parse(classContext, buf);
                if (buf.hasRemaining()) {
                    ThrowsSignature c = ThrowsSignature.parse(classContext, buf);
                    if (buf.hasRemaining()) {
                        // many
                        throwsSignatures = new ArrayList<>();
                        Collections.addAll(throwsSignatures, a, b, c);
                        do {
                            throwsSignatures.add(ThrowsSignature.parse(classContext, buf));
                        } while (buf.hasRemaining());
                        throwsSignatures = List.copyOf(throwsSignatures);
                    } else {
                        throwsSignatures = List.of(a, b, c);
                    }
                } else {
                    throwsSignatures = List.of(a, b);
                }
            } else {
                throwsSignatures = List.of(a);
            }
        } else {
            throwsSignatures = List.of();
        }
        return Cache.get(classContext).getMethodSignature(typeParameters, parameterTypes, returnTypeSignature, throwsSignatures);
    }

    public static MethodSignature synthesize(ClassContext classContext, MethodDescriptor methodDescriptor) {
        TypeSignature returnSig = TypeSignature.synthesize(classContext, methodDescriptor.getReturnType());
        List<TypeDescriptor> parameterTypes = methodDescriptor.getParameterTypes();
        int size = parameterTypes.size();
        TypeSignature[] argSigs = new TypeSignature[size];
        for (int i = 0; i < size; i ++) {
            argSigs[i] = TypeSignature.synthesize(classContext, parameterTypes.get(i));
        }
        return Cache.get(classContext).getMethodSignature(List.of(), Arrays.asList(argSigs), returnSig, List.of());
    }

    public StringBuilder toString(final StringBuilder target) {
        super.toString(target);
        target.append('(');
        for (TypeSignature p : parameterTypes) {
            p.toString(target);
        }
        target.append(')');
        returnTypeSignature.toString(target);
        for (ThrowsSignature t : throwsSignatures) {
            target.append('^');
            t.toString(target);
        }
        return target;
    }

    public static final MethodSignature VOID_METHOD_SIGNATURE = new MethodSignature(
        List.of(), List.of(), BaseTypeSignature.V, List.of()
    );
}
