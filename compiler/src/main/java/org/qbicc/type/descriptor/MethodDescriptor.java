package org.qbicc.type.descriptor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.qbicc.context.ClassContext;

/**
 *
 */
public final class MethodDescriptor extends Descriptor {
    private final List<TypeDescriptor> parameterTypes;
    private final TypeDescriptor returnType;

    MethodDescriptor(final List<TypeDescriptor> parameterTypes, final TypeDescriptor returnType) {
        super(Objects.hash(MethodDescriptor.class, parameterTypes, returnType));
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public List<TypeDescriptor> getParameterTypes() {
        return parameterTypes;
    }

    public TypeDescriptor getReturnType() {
        return returnType;
    }

    public boolean equals(final Descriptor other) {
        return other instanceof MethodDescriptor && equals((MethodDescriptor) other);
    }

    public boolean equals(final MethodDescriptor other) {
        return super.equals(other) && returnType.equals(other.returnType) && parameterTypes.equals(other.parameterTypes);
    }

    public StringBuilder toString(final StringBuilder target) {
        target.append('(');
        for (TypeDescriptor parameterType : parameterTypes) {
            parameterType.toString(target);
        }
        return returnType.toString(target.append(')'));
    }

    public static MethodDescriptor parse(ClassContext classContext, ByteBuffer buf) {
        if (next(buf) != '(') {
            throw parseError();
        }
        List<TypeDescriptor> paramTypes;
        if (peek(buf) != ')') {
            TypeDescriptor a = TypeDescriptor.parse(classContext, buf);
            if (peek(buf) != ')') {
                TypeDescriptor b = TypeDescriptor.parse(classContext, buf);
                if (peek(buf) != ')') {
                    TypeDescriptor c = TypeDescriptor.parse(classContext, buf);
                    if (peek(buf) != ')') {
                        TypeDescriptor d = TypeDescriptor.parse(classContext, buf);
                        if (peek(buf) != ')') {
                            TypeDescriptor e = TypeDescriptor.parse(classContext, buf);
                            if (peek(buf) != ')') {
                                TypeDescriptor f = TypeDescriptor.parse(classContext, buf);
                                if (peek(buf) != ')') {
                                    paramTypes = new ArrayList<>();
                                    Collections.addAll(paramTypes, a, b, c, d, e, f);
                                    paramTypes.add(TypeDescriptor.parse(classContext, buf));
                                    while (peek(buf) != ')') {
                                        paramTypes.add(TypeDescriptor.parse(classContext, buf));
                                    }
                                    paramTypes = List.copyOf(paramTypes);
                                } else {
                                    paramTypes = List.of(a, b, c, d, e, f);
                                }
                            } else {
                                paramTypes = List.of(a, b, c, d, e);
                            }
                        } else {
                            paramTypes = List.of(a, b, c, d);
                        }
                    } else {
                        paramTypes = List.of(a, b, c);
                    }
                } else {
                    paramTypes = List.of(a, b);
                }
            } else {
                paramTypes = List.of(a);
            }
        } else {
            paramTypes = List.of();
        }
        expect(buf, ')');
        return Cache.get(classContext).getMethodDescriptor(paramTypes, TypeDescriptor.parse(classContext, buf));
    }

    public static MethodDescriptor synthesize(ClassContext classContext, TypeDescriptor returnType, List<TypeDescriptor> paramTypes) {
        return org.qbicc.type.descriptor.Cache.get(classContext).getMethodDescriptor(paramTypes, returnType);
    }

    public static final MethodDescriptor VOID_METHOD_DESCRIPTOR = new MethodDescriptor(List.of(), BaseTypeDescriptor.V);
}
