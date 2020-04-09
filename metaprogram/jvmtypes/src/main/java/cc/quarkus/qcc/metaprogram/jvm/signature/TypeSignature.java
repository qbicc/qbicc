package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.metaprogram.jvm.PackageName;

/**
 * The base type for all JVM-defined generic signature types.
 */
public interface TypeSignature {
    default boolean isArray() {
        return false;
    }

    default ArrayTypeSignature asArray() {
        throw new ClassCastException();
    }

    default boolean isReference() {
        return false;
    }

    default ReferenceTypeSignature asReference() {
        throw new ClassCastException();
    }

    default boolean isTypeVariable() {
        return false;
    }

    default TypeVariableSignature asTypeVariable() {
        throw new ClassCastException();
    }

    default boolean isClass() {
        return false;
    }

    default ClassTypeSignature asClass() {
        throw new ClassCastException();
    }

    default boolean isBase() {
        return false;
    }

    default BaseTypeSignature asBase() {
        throw new ClassCastException();
    }

    default boolean isThrowable() {
        return false;
    }

    default ThrowableTypeSignature asThrowable() {
        throw new ClassCastException();
    }

    /**
     * Parse a type signature.  Requires an active {@link Context}.
     *
     * @param signature the signature string
     * @return the signature object
     * @throws IllegalArgumentException if the string is not valid
     */
    static TypeSignature parseTypeSignature(String signature) {
        return Parsing.parseTypeSignature(signature);
    }

    static TypeSignature forClass(Class<?> clazz) {
        final ParsingCache pc = ParsingCache.get();
        if (clazz.isPrimitive()) {
            final BaseTypeSignature res = BaseTypeSignature.forClass(clazz);
            if (res == null) {
                throw new IllegalArgumentException("No type signature for " + clazz);
            }
            return res;
        } else if (clazz.isArray()) {
            return pc.getArrayOf(forClass(clazz.getComponentType()));
        } else {
            final Class<?> enclosingClass = clazz.getEnclosingClass();
            ClassTypeSignature enclosing;
            PackageName packageName = null;
            if (enclosingClass != null) {
                enclosing = (ClassTypeSignature) forClass(enclosingClass);
                packageName = null;
            } else {
                enclosing = null;
                final String clazzName = clazz.getName();
                int end = clazzName.indexOf('.');
                if (end != -1) {
                    int start = 0;
                    String seg;
                    do {
                        seg = pc.getCachedName(clazzName, start, end);
                        packageName = pc.getPackageNamed(packageName, seg);
                        start = end + 1;
                        end = clazzName.indexOf('.', start);
                    } while (end != -1);
                }
            }
            final String simpleName = pc.getCachedName(clazz.getSimpleName());
            return pc.getTypeSignature(packageName, enclosing, simpleName);
        }
    }
}
