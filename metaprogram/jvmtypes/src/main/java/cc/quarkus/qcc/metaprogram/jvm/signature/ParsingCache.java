package cc.quarkus.qcc.metaprogram.jvm.signature;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.metaprogram.jvm.PackageName;

/**
 * The cache for things that get parsed.  TODO: replace lambdas with putIfAbsent
 */
final class ParsingCache {
    static final AttachmentKey<ParsingCache> key = new AttachmentKey<>();

    private final ConcurrentMap<String, String> stringCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TypeVariableSignature> typeVarCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<TypeSignature, ArrayTypeSignature> arrayTypeCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PackageName> topLevelPackageCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<PackageName, ConcurrentMap<String, PackageName>> nestedPackageCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<PackageName, ConcurrentMap<String, ClassTypeSignature>> rawClassInPackageCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassTypeSignature, ConcurrentMap<String, ClassTypeSignature>> rawClassInEnclosingCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClassTypeSignature> topLevelRawClassCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassTypeSignature, ConcurrentMap<TypeArgument, ClassTypeSignature>> typesWithArgumentsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ReferenceTypeSignature, BoundTypeArgument> contravariantArgumentsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ReferenceTypeSignature, BoundTypeArgument> invariantArgumentsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ReferenceTypeSignature, BoundTypeArgument> covariantArgumentsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TypeParameter> baseTypeParameterCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<TypeParameter, ConcurrentMap<ReferenceTypeSignature, TypeParameter>> typeParamWithClassBoundCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<TypeParameter, ConcurrentMap<ReferenceTypeSignature, TypeParameter>> typeParamWithInterfaceBoundCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassTypeSignature, ClassDeclarationSignature> classDeclSigWithSuperclass = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassDeclarationSignature, ConcurrentMap<ClassTypeSignature, ClassDeclarationSignature>> classDeclSigWithInterface = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassDeclarationSignature, ConcurrentMap<TypeParameter, ClassDeclarationSignature>> classDeclSigWithParam = new ConcurrentHashMap<>();

    static ParsingCache get() {
        return Context.requireCurrent().computeAttachmentIfAbsent(key, ParsingCache::new);
    }

    String getCachedName(final String str, final int start, final int end) {
        // todo optimize to avoid string and lambda construction
        return stringCache.computeIfAbsent(str.substring(start, end), Function.identity());
    }

    String getCachedName(final String str) {
        return stringCache.computeIfAbsent(str, Function.identity());
    }

    TypeVariableSignature getTypeVariableNamed(final String simpleName) {
        return typeVarCache.computeIfAbsent(simpleName, TypeVariableSignatureImpl::new);
    }

    ArrayTypeSignature getArrayOf(final TypeSignature nestedType) {
        return arrayTypeCache.computeIfAbsent(nestedType, ArrayTypeSignatureImpl::new);
    }

    PackageName getPackageNamed(final PackageName enclosing, final String simpleName) {
        if (enclosing == null) {
            return topLevelPackageCache.computeIfAbsent(simpleName, PackageNameImpl::new);
        } else {
            return nestedPackageCache.computeIfAbsent(enclosing, ParsingCache::newMap).computeIfAbsent(simpleName, n -> new PackageNameImpl(enclosing, n));
        }
    }

    ClassTypeSignature getTypeSignature(final PackageName packageName, final ClassTypeSignature enclosing, final String simpleName) {
        if (packageName != null) {
            assert enclosing == null;
            return rawClassInPackageCache.computeIfAbsent(packageName, ParsingCache::newMap).computeIfAbsent(simpleName, n -> new ClassTypeSignatureNoArgs(packageName, null, n));
        } else if (enclosing != null) {
            assert packageName == null;
            return rawClassInEnclosingCache.computeIfAbsent(enclosing, ParsingCache::newMap).computeIfAbsent(simpleName, n -> new ClassTypeSignatureNoArgs(null, enclosing, n));
        } else {
            return topLevelRawClassCache.computeIfAbsent(simpleName, n -> new ClassTypeSignatureNoArgs(null, null, n));
        }
    }

    ClassTypeSignature getTypeSignature(final ClassTypeSignature delegate, final TypeArgument arg) {
        return typesWithArgumentsCache.computeIfAbsent(delegate, ParsingCache::newMap).computeIfAbsent(arg, a -> new ClassTypeSignatureWithArgs(delegate, a));
    }

    BoundTypeArgument getBoundTypeArgument(final Variance variance, final ReferenceTypeSignature nested) {
        if (variance == Variance.CONTRAVARIANT) {
            return contravariantArgumentsCache.computeIfAbsent(nested, s -> new BoundTypeArgumentImpl(Variance.CONTRAVARIANT, s));
        } else if (variance == Variance.INVARIANT) {
            return invariantArgumentsCache.computeIfAbsent(nested, s -> new BoundTypeArgumentImpl(Variance.INVARIANT, s));
        } else {
            assert variance == Variance.COVARIANT;
            return covariantArgumentsCache.computeIfAbsent(nested, s -> new BoundTypeArgumentImpl(Variance.COVARIANT, s));
        }
    }

    TypeParameter getCachedTypeParameter(final String identifier) {
        return baseTypeParameterCache.computeIfAbsent(identifier, BaseTypeParameter::new);
    }

    TypeParameter getCachedTypeParameterWithClassBound(final TypeParameter base, final ReferenceTypeSignature classBound) {
        return typeParamWithClassBoundCache.computeIfAbsent(base, ParsingCache::newMap).computeIfAbsent(classBound, b -> new TypeParameterWithClassBound(base, b));
    }

    TypeParameter getCachedTypeParameterWithInterfaceBound(final TypeParameter base, final ReferenceTypeSignature interfaceBound) {
        return typeParamWithInterfaceBoundCache.computeIfAbsent(base, ParsingCache::newMap).computeIfAbsent(interfaceBound, b -> new TypeParameterWithInterfaceBound(base, b));
    }

    ClassDeclarationSignature getCachedClassDeclarationSignature(final ClassTypeSignature superclassSig) {
        return classDeclSigWithSuperclass.computeIfAbsent(superclassSig, sc -> new ClassDeclarationSignatureWithSuperclass(RootClassDeclarationSignature.INSTANCE, superclassSig));
    }

    ClassDeclarationSignature getCachedClassDeclarationSignatureWithInterface(final ClassDeclarationSignature base, final ClassTypeSignature interfaceSig) {
        return classDeclSigWithInterface.computeIfAbsent(base, ParsingCache::newMap).computeIfAbsent(interfaceSig, sc -> new ClassDeclarationSignatureWithInterface(base, sc));
    }

    ClassDeclarationSignature getCachedClassDeclarationSignatureWithParameter(final ClassDeclarationSignature base, final TypeParameter param) {
        return classDeclSigWithParam.computeIfAbsent(base, ParsingCache::newMap).computeIfAbsent(param, p -> new ClassDeclarationSignatureWithParam(base, param));
    }

    static <I, K, V> ConcurrentHashMap<K, V> newMap(I ignored) {
        return new ConcurrentHashMap<>();
    }
}
