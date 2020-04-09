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

    TypeVariableSignature getTypeVariableNamed(final String name) {
        // todo impl class
        return typeVarCache.computeIfAbsent(name, n -> new TypeVariableSignature() {
            public String getSimpleName() {
                return name;
            }
        });
    }

    ArrayTypeSignature getArrayOf(final TypeSignature nestedType) {
        // todo impl class
        return arrayTypeCache.computeIfAbsent(nestedType, nt -> new ArrayTypeSignature() {
            public TypeSignature getMemberSignature() {
                return nt;
            }
        });
    }

    PackageName getPackageNamed(final PackageName enclosing, final String simpleName) {
        // todo impl class
        if (enclosing == null) {
            return topLevelPackageCache.computeIfAbsent(simpleName, n -> new PackageName() {
                public String getSimpleName() {
                    return n;
                }

                public PackageName getEnclosing() {
                    return null;
                }

                public boolean hasEnclosing() {
                    return false;
                }
            });
        } else {
            return nestedPackageCache.computeIfAbsent(enclosing, e -> new ConcurrentHashMap<>()).computeIfAbsent(simpleName, n -> new PackageName() {
                public String getSimpleName() {
                    return n;
                }

                public PackageName getEnclosing() {
                    return enclosing;
                }

                public boolean hasEnclosing() {
                    return true;
                }
            });
        }
    }

    ClassTypeSignature getTypeSignature(final PackageName packageName, final ClassTypeSignature enclosing, final String simpleName) {
        if (packageName != null) {
            assert enclosing == null;
            return rawClassInPackageCache.computeIfAbsent(packageName, pn -> new ConcurrentHashMap<>()).computeIfAbsent(simpleName, n -> new ClassTypeSignatureNoArgs(packageName, null, n));
        } else if (enclosing != null) {
            assert packageName == null;
            return rawClassInEnclosingCache.computeIfAbsent(enclosing, e -> new ConcurrentHashMap<>()).computeIfAbsent(simpleName, n -> new ClassTypeSignatureNoArgs(null, enclosing, n));
        } else {
            return topLevelRawClassCache.computeIfAbsent(simpleName, n -> new ClassTypeSignatureNoArgs(null, null, n));
        }
    }

    ClassTypeSignature getTypeSignature(final ClassTypeSignature delegate, final TypeArgument arg) {
        return typesWithArgumentsCache.computeIfAbsent(delegate, s -> new ConcurrentHashMap<>()).computeIfAbsent(arg, a -> new ClassTypeSignatureWithArgs(delegate, a));
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
}
