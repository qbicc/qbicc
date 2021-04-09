package org.qbicc.type.generic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.ClassContext;

/**
 *
 */
final class Cache {
    private static final AttachmentKey<Cache> KEY = new AttachmentKey<>();

    private final Map<String, TypeVariableSignature> typeVars = new ConcurrentHashMap<>();
    private final Map<ReferenceTypeSignature, BoundTypeArgument> covariantBounds = new ConcurrentHashMap<>();
    private final Map<ReferenceTypeSignature, BoundTypeArgument> invariantBounds = new ConcurrentHashMap<>();
    private final Map<ReferenceTypeSignature, BoundTypeArgument> contravariantBounds = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<List<TypeArgument>, TopLevelClassTypeSignature>>> topLevelSigs = new ConcurrentHashMap<>();
    private final Map<TypeSignature, ArrayTypeSignature> arraySigs = new ConcurrentHashMap<>();

    private Cache() {}

    static Cache get(ClassContext classContext) {
        return get(classContext.getCompilationContext());
    }

    static Cache get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Cache::new);
    }

    BoundTypeArgument getBoundTypeArgument(final Variance variance, final ReferenceTypeSignature referenceTypeSignature) {
        if (variance == Variance.COVARIANT) {
            return covariantBounds.computeIfAbsent(referenceTypeSignature, Cache::newCovariantBound);
        } else if (variance == Variance.INVARIANT) {
            return invariantBounds.computeIfAbsent(referenceTypeSignature, Cache::newInvariantBound);
        } else {
            assert variance == Variance.CONTRAVARIANT;
            return contravariantBounds.computeIfAbsent(referenceTypeSignature, Cache::newContravariantBound);
        }
    }

    private static BoundTypeArgument newCovariantBound(final ReferenceTypeSignature referenceTypeSignature) {
        return new BoundTypeArgument(Variance.COVARIANT, referenceTypeSignature);
    }

    private static BoundTypeArgument newInvariantBound(final ReferenceTypeSignature referenceTypeSignature) {
        return new BoundTypeArgument(Variance.INVARIANT, referenceTypeSignature);
    }

    private static BoundTypeArgument newContravariantBound(final ReferenceTypeSignature referenceTypeSignature) {
        return new BoundTypeArgument(Variance.CONTRAVARIANT, referenceTypeSignature);
    }

    TopLevelClassTypeSignature getTopLevelTypeSignature(final String packageName, final String identifier, final List<TypeArgument> typeArgs) {
        return topLevelSigs
            .computeIfAbsent(packageName, Cache::newMap)
            .computeIfAbsent(identifier, Cache::newMap)
            .computeIfAbsent(typeArgs, a -> new TopLevelClassTypeSignature(packageName, identifier, a));
    }

    NestedClassTypeSignature getNestedTypeSignature(final ClassTypeSignature outer, final String identifier, final List<TypeArgument> typeArgs) {
        return new NestedClassTypeSignature(outer, identifier, typeArgs);
    }

    ClassSignature getClassSignature(final List<TypeParameter> typeParameters, final ClassTypeSignature superClassSignature, final List<ClassTypeSignature> interfaceSignatures) {
        return new ClassSignature(typeParameters, superClassSignature, interfaceSignatures);
    }

    MethodSignature getMethodSignature(final List<TypeParameter> typeParameters, final List<TypeSignature> parameterTypes, final TypeSignature returnTypeSignature, final List<ThrowsSignature> throwsSignatures) {
        return new MethodSignature(typeParameters, parameterTypes, returnTypeSignature, throwsSignatures);
    }

    ArrayTypeSignature getArrayTypeSignature(final TypeSignature elementTypeSignature) {
        return arraySigs.computeIfAbsent(elementTypeSignature, ArrayTypeSignature::new);
    }

    TypeParameter createTypeParameter(final String identifier, final ReferenceTypeSignature classBound, final List<ReferenceTypeSignature> interfaceBounds) {
        return new TypeParameter(identifier, classBound, interfaceBounds);
    }

    TypeVariableSignature getTypeVariableSignature(final String identifier) {
        return typeVars.computeIfAbsent(identifier, TypeVariableSignature::new);
    }

    private static <K, V> Map<K, V> newMap(final Object key) {
        return new ConcurrentHashMap<>();
    }
}
